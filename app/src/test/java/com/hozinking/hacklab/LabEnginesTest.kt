package com.hozinking.hacklab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Fase 3 lab engines (LabEngines.kt). The engines are
 * pure Kotlin — no Android — so these run on the plain JVM.
 */
class LabEnginesTest {

    // ---------------------------------------------------------------- XSS

    @Test
    fun xss_scriptTag_triggersAndStealsCookie() {
        val r = XssEngine.classify("<script>alert(1)</script>")
        assertEquals(XssKind.SCRIPT_TAG, r.kind)
        assertTrue(r.triggered)
        assertTrue(r.cookieStolen)
    }

    @Test
    fun xss_imgOnerror_triggers() {
        val r = XssEngine.classify("<img src=x onerror=alert(1)>")
        assertEquals(XssKind.EVENT_HANDLER, r.kind)
        assertTrue(r.triggered)
    }

    @Test
    fun xss_svgOnload_triggers() {
        val r = XssEngine.classify("<svg onload=alert(1)>")
        assertEquals(XssKind.SVG_ONLOAD, r.kind)
        assertTrue(r.triggered)
    }

    @Test
    fun xss_javascriptUri_triggers() {
        val r = XssEngine.classify("<a href=\"javascript:alert(1)\">klik</a>")
        assertEquals(XssKind.JS_URI, r.kind)
        assertTrue(r.triggered)
    }

    @Test
    fun xss_plainText_doesNotTrigger() {
        val r = XssEngine.classify("Halo, situsnya bagus!")
        assertEquals(XssKind.NONE, r.kind)
        assertFalse(r.triggered)
        assertFalse(r.cookieStolen)
    }

    @Test
    fun xss_harmlessTag_doesNotTrigger() {
        val r = XssEngine.classify("<b>tebal</b> dan <i>miring</i>")
        assertEquals(XssKind.NONE, r.kind)
        assertFalse(r.triggered)
    }

    // ---------------------------------------------------------------- CSS

    @Test
    fun css_parse_twoRules() {
        val rules = CssEngine.parse(".profil { background: red; color: white; } body { display: block; }")
        assertEquals(2, rules.size)
        assertEquals(".profil", rules[0].selector)
        assertEquals("red", rules[0].props["background"])
        assertEquals("white", rules[0].props["color"])
    }

    @Test
    fun css_overlayRule_detected() {
        val rules = CssEngine.parse(".login-palsu { position: fixed; top: 0; }")
        assertTrue(CssEngine.isOverlayRule(rules[0]))
        assertFalse(CssEngine.isDefaceRule(rules[0]))
    }

    @Test
    fun css_defaceRule_detected() {
        val rules = CssEngine.parse(".profil { display: none; }")
        assertTrue(CssEngine.isDefaceRule(rules[0]))
        assertFalse(CssEngine.isOverlayRule(rules[0]))
    }

    @Test
    fun css_unknownSelector_notDeface() {
        val rules = CssEngine.parse(".tidak-ada { background: red; }")
        assertFalse(CssEngine.isDefaceRule(rules[0]))
    }

    @Test
    fun css_exfiltrationMention_detected() {
        assertTrue(CssEngine.mentionsExfiltration("input[value^=\"a\"] { background: url(https://x.example/a); }"))
        assertFalse(CssEngine.mentionsExfiltration(".profil { background: red; }"))
    }

    // --------------------------------------------------------------- SQLi

    @Test
    fun sqli_bypassWithComment_asAdmin() {
        val outcome = SqliEngine.attempt("' OR '1'='1' --", "apa-saja")
        assertTrue(outcome is SqliOutcome.Bypass)
        assertEquals("admin", (outcome as SqliOutcome.Bypass).user.username)
    }

    @Test
    fun sqli_tautologyWithoutComment_fails() {
        // AND binds tighter than OR: without the -- comment the password
        // is still checked, so the login must fail.
        val outcome = SqliEngine.attempt("' OR '1'='1", "salah")
        assertTrue(outcome is SqliOutcome.Fail)
    }

    @Test
    fun sqli_union_dumpsTable() {
        val outcome = SqliEngine.attempt("' UNION SELECT id, username, password FROM users --", "")
        assertTrue(outcome is SqliOutcome.Dump)
        assertEquals(3, (outcome as SqliOutcome.Dump).rows.size)
    }

    @Test
    fun sqli_correctCredentials_normalLogin() {
        val outcome = SqliEngine.attempt("alice", "alicepass")
        assertTrue(outcome is SqliOutcome.Normal)
        assertEquals("alice", (outcome as SqliOutcome.Normal).user.username)
    }

    @Test
    fun sqli_wrongCredentials_fail() {
        val outcome = SqliEngine.attempt("alice", "salah")
        assertTrue(outcome is SqliOutcome.Fail)
    }

    @Test
    fun sqli_queryShowsRawInput() {
        val q = SqliEngine.buildQuery("' OR 1=1 --", "x")
        assertTrue(q.contains("' OR 1=1 --"))
    }

    // --------------------------------------------------------------- WiFi

    @Test
    fun wifi_airodumpBeforeMonitor_refused() {
        val engine = WifiEngine()
        val r = engine.execute("airodump-ng wlan0mon")
        assertTrue(r is EngineResult.Text)
        assertTrue((r as EngineResult.Text).text.contains("mode monitor"))
        assertFalse(engine.scanned)
    }

    @Test
    fun wifi_airmon_thenScan_listsAps() {
        val engine = WifiEngine()
        engine.execute("airmon-ng start wlan0")
        assertTrue(engine.monitorMode)
        val r = engine.execute("airodump-ng wlan0mon")
        assertTrue(engine.scanned)
        val text = (r as EngineResult.Text).text
        assertTrue(text.contains("RumahKita"))
        assertTrue(text.contains("AA:BB:CC:DD:EE:01"))
    }

    @Test
    fun wifi_deauthUnknownBssid_refused() {
        val engine = WifiEngine()
        engine.execute("airmon-ng start wlan0")
        engine.execute("airodump-ng wlan0mon")
        val r = engine.execute("aireplay-ng --deauth 10 -a FF:FF:FF:FF:FF:FF -c 11:22:33:44:55:66 wlan0mon")
        assertTrue((r as EngineResult.Text).text.contains("tidak ada di hasil scan"))
        assertTrue(engine.disconnected.isEmpty())
    }

    @Test
    fun wifi_deauthValid_disconnectsClient() {
        val engine = WifiEngine()
        engine.execute("airmon-ng start wlan0")
        engine.execute("airodump-ng wlan0mon")
        val r = engine.execute("aireplay-ng --deauth 10 -a AA:BB:CC:DD:EE:01 -c 11:22:33:44:55:66 wlan0mon")
        assertTrue((r as EngineResult.Text).text.contains("TERPUTUS"))
        assertEquals(10, engine.deauthFrames)
        assertTrue(engine.disconnected.contains("11:22:33:44:55:66"))
    }

    // --------------------------------------------------------------- DDoS

    @Test
    fun ddos_baseline_stable() {
        val engine = DdosEngine()
        repeat(5) { engine.tick() }
        assertEquals("ONLINE", engine.status)
        assertEquals(18, engine.cpu)
        assertFalse(engine.complete)
    }

    @Test
    fun ddos_synFlood_downsServer_thenMitigationRecovers() {
        val engine = DdosEngine()
        engine.startAttack(DdosAttack.SYN)
        var ticks = 0
        while (!engine.downed && ticks < 40) {
            engine.tick()
            ticks++
        }
        assertTrue("server should go DOWN under SYN flood", engine.downed)
        assertTrue(engine.downedEver)
        engine.stopAttack()
        engine.toggleMitigation("rate")
        engine.toggleMitigation("waf")
        engine.toggleMitigation("anycast")
        ticks = 0
        while (!engine.recovered && ticks < 60) {
            engine.tick()
            ticks++
        }
        assertTrue("server should recover with mitigations", engine.recovered)
        assertEquals("ONLINE", engine.status)
        assertTrue(engine.complete)
    }

    @Test
    fun ddos_mitigationAlone_doesNotDown() {
        val engine = DdosEngine()
        engine.toggleMitigation("rate")
        engine.toggleMitigation("waf")
        engine.toggleMitigation("anycast")
        engine.startAttack(DdosAttack.SYN)
        repeat(30) { engine.tick() }
        assertFalse("full mitigation should hold the server", engine.downed)
    }

    // --------------------------------------------------------- Hack Server

    @Test
    fun server_exploitBeforeScan_refused() {
        val engine = ServerHackEngine()
        val r = engine.execute("exploit wordpress")
        assertTrue((r as EngineResult.Text).text.contains("Pindai dulu"))
        assertFalse(engine.shell)
    }

    @Test
    fun server_nmap_thenWordpressShell_thenSudoFlag() {
        val engine = ServerHackEngine()
        engine.execute("nmap -sV 203.0.113.10")
        assertTrue(engine.scanned)
        engine.execute("exploit wordpress")
        assertTrue(engine.shell)
        assertEquals("www-data", engine.shellUser)
        val denied = engine.execute("cat /root/flag.txt")
        assertTrue((denied as EngineResult.Text).text.contains("Permission denied"))
        assertFalse(engine.flagGot)
        engine.execute("exploit ssh")
        assertEquals("admin", engine.shellUser)
        engine.execute("sudo cat /root/flag.txt")
        assertTrue(engine.flagGot)
    }

    @Test
    fun server_catWithoutShell_refused() {
        val engine = ServerHackEngine()
        val r = engine.execute("cat /root/flag.txt")
        assertTrue((r as EngineResult.Text).text.contains("belum punya shell"))
    }

    // -------------------------------------------------------- Hack Crypto

    @Test
    fun crypto_auditWithoutScenario_asksToPick() {
        val engine = CryptoEngine()
        val r = engine.execute("audit")
        assertTrue((r as EngineResult.Text).text.contains("Pilih skenario dulu"))
    }

    @Test
    fun crypto_scenarioA_fullChain() {
        val engine = CryptoEngine()
        engine.execute("pilih a")
        engine.execute("audit")
        val wrong = engine.execute("trace TX-0000")
        assertTrue((wrong as EngineResult.Text).text.contains("trace TX-7f3a"))
        assertFalse(engine.aTraced)
        engine.execute("trace TX-7f3a")
        assertTrue(engine.aTraced)
        engine.execute("freeze")
        assertTrue(engine.aDone)
        assertFalse(engine.complete)
    }

    @Test
    fun crypto_scenarioB_fullChain() {
        val engine = CryptoEngine()
        engine.execute("pilih b")
        engine.execute("audit")
        engine.execute("exploit")
        assertTrue(engine.bDrained)
        engine.execute("patch")
        assertTrue(engine.bDone)
    }

    @Test
    fun crypto_bothScenarios_complete() {
        val engine = CryptoEngine()
        engine.execute("pilih a")
        engine.execute("audit")
        engine.execute("trace TX-7f3a")
        engine.execute("freeze")
        engine.execute("pilih b")
        engine.execute("audit")
        engine.execute("exploit")
        engine.execute("patch")
        assertTrue(engine.complete)
        assertTrue(engine.statusText().contains("OK"))
    }

    // ------------------------------------------------------------- Catalog

    @Test
    fun labs_catalog_hasSevenLabs() {
        assertEquals(7, Labs.COUNT)
        assertEquals(7, Labs.ALL.size)
        for (i in 0 until Labs.COUNT) {
            val lab = Labs.get(i)
            assertTrue(lab != null)
            assertEquals(i, lab!!.index)
            assertTrue(lab.flag.startsWith("FLAG{"))
        }
    }
}
