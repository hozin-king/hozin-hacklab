package com.hozinking.hacklab

/**
 * Mesin lab Fase 3 — logika murni, tanpa dependensi Android, supaya bisa
 * diuji unit. Semua target di sini PALSU: buku tamu palsu, profil palsu,
 * tabel users palsu, jaringan WiFi palsu, server palsu, botnet palsu,
 * bursa dan kontrak kripto palsu. Tidak ada satu pun yang menyentuh
 * jaringan atau sistem nyata.
 */

// ---------------------------------------------------------------- XSS ---

enum class XssKind {
    NONE,
    SCRIPT_TAG,
    EVENT_HANDLER,
    SVG_ONLOAD,
    JS_URI
}

data class XssResult(
    val kind: XssKind,
    val triggered: Boolean,
    val cookieStolen: Boolean,
    val explanation: String
)

object XssEngine {

    const val FAKE_COOKIE = "PHPSESSID=9f2ac41b7e5d"

    fun classify(payload: String): XssResult {
        val lower = payload.toLowerCase()
        if (lower.contains("<script")) {
            return XssResult(
                XssKind.SCRIPT_TAG, true, true,
                "Tag <script> lolos tanpa encoding — browser menjalankan " +
                    "isinya sebagai kode. Inilah XSS tersimpan: payloadmu " +
                    "disimpan lalu dijalankan di browser setiap pengunjung."
            )
        }
        if (lower.contains("<svg") && lower.contains("onload")) {
            return XssResult(
                XssKind.SVG_ONLOAD, true, true,
                "Tag <svg> dengan onload sama berbahayanya dengan " +
                    "<script>: browser menjalankan JavaScript saat elemen " +
                    "dimuat — tanpa perlu tag script sama sekali."
            )
        }
        if (Regex("on\\w+\\s*=").containsMatchIn(lower)) {
            return XssResult(
                XssKind.EVENT_HANDLER, true, true,
                "Event handler (onerror, onload, onclick, ...) adalah " +
                    "jalur XSS klasik saat tag <script> diblokir: kode " +
                    "menempel di atribut elemen dan jalan saat event-nya " +
                    "terjadi."
            )
        }
        if (lower.contains("javascript:")) {
            return XssResult(
                XssKind.JS_URI, true, true,
                "Skema javascript: di atribut href menjalankan kode saat " +
                    "tautan diklik. Di lab ini kliknya disimulasikan — " +
                    "korban yang mengklik tautanmu akan mengeksekusinya."
            )
        }
        return XssResult(
            XssKind.NONE, false, false,
            "Tidak ada kode yang dieksekusi — payload ini dirender " +
                "sebagai teks biasa. Tag HTML yang tidak berbahaya " +
                "tetap tampil sebagai teks, bukan kode."
        )
    }
}

// ---------------------------------------------------------------- CSS ---

data class CssRule(val selector: String, val props: Map<String, String>)

object CssEngine {

    val KNOWN_SELECTORS = setOf("body", ".header", ".profil", ".tombol", ".bio")

    /** Mengurai blok "selector { prop: value; ... }" menjadi aturan. */
    fun parse(input: String): List<CssRule> {
        val rules = ArrayList<CssRule>()
        val block = Regex("([^{}]+)\\{([^}]*)\\}")
        for (m in block.findAll(input)) {
            val selector = m.groupValues[1].trim().toLowerCase()
            if (selector.isEmpty()) continue
            val props = LinkedHashMap<String, String>()
            for (decl in m.groupValues[2].split(";")) {
                val parts = decl.split(":")
                if (parts.size < 2) continue
                val name = parts[0].trim().toLowerCase()
                val value = parts.subList(1, parts.size).joinToString(":").trim()
                if (name.isNotEmpty() && value.isNotEmpty()) props[name] = value
            }
            rules.add(CssRule(selector, props))
        }
        return rules
    }

    /** Aturan yang memunculkan overlay login palsu. */
    fun isOverlayRule(rule: CssRule): Boolean {
        if (rule.selector != ".login-palsu") return false
        val pos = rule.props["position"] ?: return false
        return pos == "fixed" || pos == "absolute"
    }

    /** Aturan yang benar-benar mengubah tampilan halaman palsu. */
    fun isDefaceRule(rule: CssRule): Boolean {
        if (!KNOWN_SELECTORS.contains(rule.selector)) return false
        return rule.props.containsKey("background") ||
            rule.props.containsKey("color") ||
            rule.props.containsKey("display") ||
            rule.props.containsKey("font-size")
    }

    /** Sekadar teori exfiltrasi lewat attribute selector. */
    fun mentionsExfiltration(input: String): Boolean {
        val lower = input.toLowerCase()
        return lower.contains("input[") && lower.contains("url(")
    }
}

// --------------------------------------------------------------- SQLi ---

data class FakeUser(val id: Int, val username: String, val passwordHash: String)

sealed class SqliOutcome {
    class Fail(val note: String) : SqliOutcome()
    class Bypass(val user: FakeUser, val query: String, val note: String) : SqliOutcome()
    class Dump(val rows: List<FakeUser>, val query: String, val note: String) : SqliOutcome()
    class Normal(val user: FakeUser, val query: String, val note: String) : SqliOutcome()
}

object SqliEngine {

    // Tabel PALSU. Hash di sini hanya teks contoh, bukan hash asli.
    val USERS = listOf(
        FakeUser(1, "admin", "hash-palsu-admin-9f2a"),
        FakeUser(2, "alice", "hash-palsu-alice-41bc"),
        FakeUser(3, "bob", "hash-palsu-bob-77e0")
    )

    fun buildQuery(username: String, password: String): String {
        return "SELECT * FROM users WHERE username = '$username' " +
            "AND password = '$password'"
    }

    fun attempt(username: String, password: String): SqliOutcome {
        val query = buildQuery(username, password)
        val lower = username.toLowerCase()
        if (lower.contains("union") && lower.contains("select")) {
            return SqliOutcome.Dump(
                USERS, query,
                "UNION menempelkan SELECT kedua dan hasilnya digabung " +
                    "dengan query login — seluruh isi tabel users bocor " +
                    "lewat halaman yang sama."
            )
        }
        val tautology = lower.contains("'1'='1'") ||
            lower.contains("1=1") ||
            lower.contains("'a'='a'")
        if (tautology) {
            val commented = lower.contains("--") ||
                lower.contains("#") ||
                lower.contains("/*")
            if (commented) {
                return SqliOutcome.Bypass(
                    USERS[0], query,
                    "OR membuat kondisi selalu benar, dan -- mematikan " +
                        "sisa query termasuk pengecekan password. Login " +
                        "lolos sebagai baris pertama tabel: admin."
                )
            }
            return SqliOutcome.Fail(
                "Tautologi ada, tapi password tetap diperiksa: AND " +
                    "mengikat lebih kuat daripada OR. Tanpa komentar -- " +
                    "untuk mematikan sisa query, login tetap gagal."
            )
        }
        for (user in USERS) {
            val realPassword = when (user.username) {
                "admin" -> "admin123"
                "alice" -> "alicepass"
                "bob" -> "bobpass"
                else -> ""
            }
            if (username == user.username && password == realPassword) {
                return SqliOutcome.Normal(
                    user, query,
                    "Login normal sebagai ${user.username} — tanpa " +
                        "injeksi, kredensial benar tetap bisa masuk."
                )
            }
        }
        return SqliOutcome.Fail(
            "Kombinasi username/password salah — dan tidak ada injeksi " +
                "yang mengubah query."
        )
    }
}

// --------------------------------------------------------------- WiFi ---

data class FakeAp(
    val bssid: String,
    val essid: String,
    val channel: Int,
    val enc: String,
    val pwr: Int
)

data class FakeClient(
    val station: String,
    val bssid: String,
    val pwr: Int,
    val frames: Int
)

/** Simulator ala aircrack-ng. Semua jaringan dan klien di sini palsu. */
class WifiEngine {

    var monitorMode = false
        private set
    var scanned = false
        private set
    var deauthFrames = 0
        private set
    val disconnected = LinkedHashSet<String>()

    val aps = listOf(
        FakeAp("AA:BB:CC:DD:EE:01", "RumahKita", 6, "WPA2", -42),
        FakeAp("AA:BB:CC:DD:EE:02", "KostPutri_5G", 36, "WPA2", -55),
        FakeAp("AA:BB:CC:DD:EE:03", "WarungKopi", 11, "WPA2", -63),
        FakeAp("AA:BB:CC:DD:EE:04", "WifiGratis", 1, "OPN", -71)
    )

    val clients = listOf(
        FakeClient("11:22:33:44:55:66", "AA:BB:CC:DD:EE:01", -58, 184),
        FakeClient("22:33:44:55:66:77", "AA:BB:CC:DD:EE:01", -61, 96),
        FakeClient("33:44:55:66:77:88", "AA:BB:CC:DD:EE:02", -66, 210),
        FakeClient("44:55:66:77:88:99", "AA:BB:CC:DD:EE:03", -70, 41)
    )

    fun prompt(): String = "root@kali:~# "

    fun execute(rawInput: String): EngineResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return EngineResult.Text("")
        val parts = input.split(Regex("\\s+"))
        val cmd = parts[0]
        return when (cmd) {
            "help" -> EngineResult.Text(HELP_TEXT)
            "clear" -> EngineResult.Clear
            "airmon-ng" -> runAirmon(parts)
            "airodump-ng" -> runAirodump(parts)
            "aireplay-ng" -> runAireplay(parts)
            else -> EngineResult.Text(
                "Perintah '$cmd' belum dikenal di lab ini. Ketik help."
            )
        }
    }

    private fun runAirmon(parts: List<String>): EngineResult {
        if (parts.size >= 3 && parts[1] == "start" && parts[2] == "wlan0") {
            if (monitorMode) {
                return EngineResult.Text("wlan0mon sudah dalam mode monitor.")
            }
            monitorMode = true
            return EngineResult.Text(
                "Found 1 processes that could cause trouble.\n" +
                    "  PID  Name\n" +
                    "  512  NetworkManager\n" +
                    "PHY     Interface  Driver      Chipset\n" +
                    "phy0    wlan0      rtl88xx     Realtek 802.11ac\n" +
                    "        (monitor mode enabled on mon0)\n" +
                    "        (mode monitor aktif di wlan0mon — SIMULASI)"
            )
        }
        return EngineResult.Text(
            "Pakai: airmon-ng start wlan0\n" +
                "Contoh: airmon-ng start wlan0"
        )
    }

    private fun runAirodump(parts: List<String>): EngineResult {
        if (!monitorMode) {
            return EngineResult.Text(
                "Kartu belum mode monitor. Jalankan dulu: " +
                    "airmon-ng start wlan0"
            )
        }
        if (parts.size < 2 || parts[1] != "wlan0mon") {
            return EngineResult.Text(
                "Pakai: airodump-ng wlan0mon\n" +
                    "Contoh: airodump-ng wlan0mon"
            )
        }
        scanned = true
        val out = StringBuilder()
        out.append("CH  BSSID              PWR  Beacons  #Data  ENC   ESSID\n")
        for (ap in aps) {
            out.append(
                "%-3d %-18s %-4d %-8d %-6d %-5s %s\n".format(
                    ap.channel, ap.bssid, ap.pwr, 120, 45, ap.enc, ap.essid
                )
            )
        }
        out.append("\nBSSID              STATION            PWR   Frames\n")
        for (c in clients) {
            val status = if (disconnected.contains(c.station)) "TERPUTUS" else ""
            out.append(
                "%-18s %-18s %-5d %-6d %s".format(
                    c.bssid, c.station, c.pwr, c.frames, status
                )
            )
            out.append("\n")
        }
        out.append("\n(Semua jaringan dan klien di atas PALSU — hasil simulasi.)")
        return EngineResult.Text(out.toString().trimEnd())
    }

    private fun runAireplay(parts: List<String>): EngineResult {
        if (!monitorMode) {
            return EngineResult.Text(
                "Kartu belum mode monitor. Jalankan dulu: " +
                    "airmon-ng start wlan0"
            )
        }
        if (!scanned) {
            return EngineResult.Text(
                "Belum ada hasil scan. Jalankan dulu: " +
                    "airodump-ng wlan0mon"
            )
        }
        var count = 10
        var bssid: String? = null
        var station: String? = null
        var i = 1
        while (i < parts.size) {
            when (parts[i]) {
                "--deauth" -> {
                    if (i + 1 < parts.size) count = parts[i + 1].toIntOrNull() ?: 10
                    i += 2
                }
                "-a" -> {
                    if (i + 1 < parts.size) bssid = parts[i + 1]
                    i += 2
                }
                "-c" -> {
                    if (i + 1 < parts.size) station = parts[i + 1]
                    i += 2
                }
                else -> i += 1
            }
        }
        if (bssid == null || station == null) {
            return EngineResult.Text(
                "Pakai: aireplay-ng --deauth <jumlah> -a <BSSID> " +
                    "-c <STATION> wlan0mon"
            )
        }
        val ap = aps.find { it.bssid == bssid }
            ?: return EngineResult.Text("BSSID $bssid tidak ada di hasil scan.")
        val client = clients.find { it.station == station && it.bssid == bssid }
            ?: return EngineResult.Text(
                "STATION $station bukan klien dari $bssid."
            )
        deauthFrames += count
        disconnected.add(station)
        return EngineResult.Text(
            "Waiting for beacon frame (BSSID: $bssid) on channel ${ap.channel}\n" +
                "Sending $count directed DeAuth (code 7). STMAC: [$station] ...\n" +
                "  $count ACKs received — klien $station TERPUTUS dari ${ap.essid}.\n" +
                "Total frame deauth terkirim: $deauthFrames " +
                "(SIMULASI — tidak ada frame yang benar-benar dikirim.)"
        )
    }

    companion object {
        private const val HELP_TEXT =
            "Perintah lab WiFi (simulasi aircrack-ng):\n" +
                "  airmon-ng start wlan0\n" +
                "      masuk mode monitor (interface jadi wlan0mon)\n" +
                "  airodump-ng wlan0mon\n" +
                "      pindai jaringan dan klien di sekitar\n" +
                "  aireplay-ng --deauth <jumlah> -a <BSSID> -c <STATION> wlan0mon\n" +
                "      kirim frame deauth ke satu klien\n" +
                "  clear               membersihkan layar\n" +
                "  help                menampilkan daftar ini"
    }
}

// --------------------------------------------------------------- DDoS ---

enum class DdosAttack(val label: String, val load: Int, val cpuTarget: Int) {
    NONE("Tidak ada", 0, 18),
    SYN("SYN Flood", 8500, 96),
    UDP("UDP Flood", 13000, 100),
    HTTP("HTTP Flood", 5200, 84)
}

/**
 * Server palsu di bawah flood simulasi. Satu tick = satu detik simulasi.
 * Tidak ada paket yang dikirim ke mana pun: angka-angkanya hanya model.
 */
class DdosEngine {

    var attack: DdosAttack = DdosAttack.NONE
        private set
    var rateLimit = false
        private set
    var waf = false
        private set
    var anycast = false
        private set

    var reqPerSec = 120
        private set
    var latencyMs = 24
        private set
    var cpu = 18
        private set
    var downed = false
        private set
    var downedEver = false
        private set
    var recovered = false
        private set
    var seconds = 0
        private set

    val bots = 12400
    val cpuHistory = ArrayList<Int>()

    val status: String
        get() = if (downed) "DOWN" else "ONLINE"

    val complete: Boolean
        get() = downedEver && recovered

    fun startAttack(next: DdosAttack): String {
        if (next == DdosAttack.NONE) return "Pilih jenis flood: SYN, UDP, atau HTTP."
        attack = next
        return "[botnet] $bots bot diperintah: ${next.label} " +
            "ke 203.0.113.10:80 — trafik simulasi mulai membanjir."
    }

    fun stopAttack(): String {
        if (attack == DdosAttack.NONE) return "Tidak ada serangan yang berjalan."
        attack = DdosAttack.NONE
        return "Serangan dihentikan — botnet diam. " +
            "Trafik kembali ke pengunjung normal."
    }

    fun toggleMitigation(name: String): String {
        when (name) {
            "rate" -> {
                rateLimit = !rateLimit
                return if (rateLimit) {
                    "[mitigasi] Rate limit AKTIF: maks 100 req/detik per IP — " +
                        "bot yang membabi buta langsung kena potong."
                } else {
                    "[mitigasi] Rate limit dimatikan."
                }
            }
            "waf" -> {
                waf = !waf
                return if (waf) {
                    "[mitigasi] WAF/CDN AKTIF: trafik disaring di tepi " +
                        "jaringan, pola serangan diblokir sebelum sampai " +
                        "ke server asal."
                } else {
                    "[mitigasi] WAF/CDN dimatikan."
                }
            }
            "anycast" -> {
                anycast = !anycast
                return if (anycast) {
                    "[mitigasi] Anycast AKTIF: server disebar ke 8 lokasi — " +
                        "banjir terpecah, tidak ada satu titik yang " +
                        "menanggung semuanya."
                } else {
                    "[mitigasi] Anycast dimatikan."
                }
            }
            else -> return "Mitigasi tidak dikenal: $name."
        }
    }

    /** Maju satu detik simulasi; kembalikan satu baris log. */
    fun tick(): String {
        seconds += 1
        var factor = 1.0
        if (rateLimit) factor *= 0.4
        if (waf) factor *= 0.5
        if (anycast) factor *= 0.35
        val targetReq = ((120 + attack.load) * factor).toInt()
        val diff = targetReq - reqPerSec
        reqPerSec += when {
            diff == 0 -> 0
            diff > 0 -> maxOf(1, diff / 2)
            else -> minOf(-1, diff / 2)
        }
        var targetCpu = attack.cpuTarget
        if (rateLimit) targetCpu -= 30
        if (waf) targetCpu -= 25
        if (anycast) targetCpu -= 20
        if (targetCpu < 15) targetCpu = 15
        val cpuDiff = targetCpu - cpu
        cpu += when {
            cpuDiff == 0 -> 0
            cpuDiff > 0 -> minOf(8, cpuDiff)
            else -> maxOf(-8, cpuDiff)
        }
        latencyMs = 24 + reqPerSec / 60
        if (cpuHistory.size >= 24) cpuHistory.removeAt(0)
        cpuHistory.add(cpu)
        if (cpu >= 95 && !downed) {
            downed = true
            downedEver = true
            return "[t+${seconds}s] req/s=$reqPerSec cpu=$cpu% " +
                "lat=${latencyMs}ms status=DOWN — server tidak merespons!"
        }
        if (downed && cpu < 40) {
            downed = false
            recovered = true
            return "[t+${seconds}s] req/s=$reqPerSec cpu=$cpu% " +
                "lat=${latencyMs}ms status=ONLINE — server pulih berkat mitigasi."
        }
        return "[t+${seconds}s] req/s=$reqPerSec cpu=$cpu% " +
            "lat=${latencyMs}ms status=$status"
    }
}

// --------------------------------------------------------- Hack Server ---

/** Host PALSU 203.0.113.10 (alamat dokumentasi TEST-NET). */
class ServerHackEngine {

    var scanned = false
        private set
    var shell = false
        private set
    var shellUser = ""
        private set
    var flagGot = false
        private set

    fun prompt(): String {
        return if (shell) {
            if (shellUser == "www-data") "www-data@victim:/var/www$ " else "admin@victim:~$ "
        } else {
            "pemula@hacklab:~$ "
        }
    }

    fun execute(rawInput: String): EngineResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return EngineResult.Text("")
        val parts = input.split(Regex("\\s+"))
        val cmd = parts[0]
        return when (cmd) {
            "help" -> EngineResult.Text(HELP_TEXT)
            "clear" -> EngineResult.Clear
            "nmap" -> runNmap(parts)
            "exploit" -> runExploit(parts)
            "whoami" -> runShellOnly { shellUser }
            "id" -> runShellOnly { "uid=1000($shellUser) gid=1000($shellUser)" }
            "ls" -> runShellOnly {
                if (shellUser == "admin") "flag.txt  dokumen  backup.sql" else "index.php  wp-config.php"
            }
            "cat" -> runCat(parts)
            "sudo" -> runSudo(parts)
            "exit" -> runExit()
            "ssh" -> EngineResult.Text(
                "Lab ini tidak membuka koneksi SSH nyata. " +
                    "Pakai: exploit ssh (kredensial lemah admin:admin)."
            )
            else -> EngineResult.Text(
                "Perintah '$cmd' belum dikenal di lab ini. Ketik help."
            )
        }
    }

    private fun runShellOnly(block: () -> String): EngineResult {
        if (!shell) {
            return EngineResult.Text(
                "Kamu belum punya shell di server target. " +
                    "Mulai dari nmap, lalu exploit."
            )
        }
        return EngineResult.Text(block())
    }

    private fun runNmap(parts: List<String>): EngineResult {
        if (!parts.contains("203.0.113.10")) {
            return EngineResult.Text(
                "Pakai: nmap -sV 203.0.113.10\n" +
                    "Contoh: nmap -sV 203.0.113.10"
            )
        }
        scanned = true
        return EngineResult.Text(
            "Starting Nmap 7.94 ( https://nmap.org )\n" +
                "Nmap scan report for 203.0.113.10\n" +
                "Host is up (0.042s latency).\n" +
                "PORT     STATE SERVICE  VERSION\n" +
                "22/tcp   open  ssh      OpenSSH 7.2p2 (protokol 2.0)\n" +
                "80/tcp   open  http     Apache httpd 2.4.7 (WordPress 4.7.0)\n" +
                "3306/tcp open  mysql    MySQL 5.5.62 (terekspos ke internet!)\n" +
                "Service detection performed.\n" +
                "Catatan: WordPress 4.7.0 dan OpenSSH 7.2 sudah tua — " +
                "cek celah yang dikenal."
        )
    }

    private fun runExploit(parts: List<String>): EngineResult {
        if (!scanned) {
            return EngineResult.Text(
                "Jangan asal tembak. Pindai dulu: nmap -sV 203.0.113.10"
            )
        }
        if (parts.size < 2) {
            return EngineResult.Text(
                "Pakai: exploit wordpress  ATAU  exploit ssh"
            )
        }
        return when (parts[1]) {
            "wordpress" -> {
                shell = true
                shellUser = "www-data"
                EngineResult.Text(
                    "[+] Mengirim payload ke plugin WordPress 4.7.0...\n" +
                        "[+] Reverse shell masuk! Kamu sekarang www-data@victim.\n" +
                        "Perhatian: www-data haknya kecil. Cari jalan naik " +
                        "ke admin — sudo-nya mungkin salah konfigurasi."
                )
            }
            "ssh" -> {
                shell = true
                shellUser = "admin"
                EngineResult.Text(
                    "[+] Mencoba kredensial default ke SSH...\n" +
                        "[+] admin:admin DITERIMA. Shell masuk sebagai admin.\n" +
                        "Kredensial lemah adalah celah paling memalukan — " +
                        "dan paling sering terjadi."
                )
            }
            else -> EngineResult.Text(
                "Target exploit tidak dikenal: ${parts[1]}. " +
                    "Yang ada: wordpress, ssh."
            )
        }
    }

    private fun runCat(parts: List<String>): EngineResult {
        if (!shell) {
            return EngineResult.Text(
                "Kamu belum punya shell. Mulai dari nmap, lalu exploit."
            )
        }
        if (parts.size < 2 || parts[1] != "/root/flag.txt") {
            return EngineResult.Text("Di lab ini, yang dicari: /root/flag.txt")
        }
        return if (shellUser == "admin") {
            flagGot = true
            EngineResult.Text(
                "FLAG{recon_ke_root}\n" +
                    "Flag direbut! Rantai lengkap: recon -> exploit -> flag."
            )
        } else {
            EngineResult.Text(
                "cat: /root/flag.txt: Permission denied — file ini " +
                    "milik root.\n" +
                    "Petunjuk: admin punya sudo tanpa password. " +
                    "Coba: sudo cat /root/flag.txt"
            )
        }
    }

    private fun runSudo(parts: List<String>): EngineResult {
        if (!shell) {
            return EngineResult.Text(
                "Kamu belum punya shell. Mulai dari nmap, lalu exploit."
            )
        }
        if (shellUser != "admin") {
            return EngineResult.Text(
                "www-data is not in the sudoers file. " +
                    "This incident will be reported."
            )
        }
        if (parts.size >= 3 && parts[1] == "cat" && parts[2] == "/root/flag.txt") {
            flagGot = true
            return EngineResult.Text(
                "FLAG{recon_ke_root}\n" +
                    "Flag direbut! Rantai lengkap: recon -> exploit -> flag.\n" +
                    "(sudo tanpa password di tangan admin adalah " +
                    "miskonfigurasi — di server nyata, ini harus dimatikan.)"
            )
        }
        return EngineResult.Text(
            "Pakai: sudo cat /root/flag.txt"
        )
    }

    private fun runExit(): EngineResult {
        if (!shell) return EngineResult.Text("Tidak ada shell yang berjalan.")
        shell = false
        shellUser = ""
        return EngineResult.Text("Shell ditutup. Kamu kembali ke hacklab.")
    }

    companion object {
        private const val HELP_TEXT =
            "Perintah lab Hack Server (target PALSU 203.0.113.10):\n" +
                "  nmap -sV 203.0.113.10\n" +
                "      pindai port dan versi layanan\n" +
                "  exploit wordpress | exploit ssh\n" +
                "      eksploitasi celah yang ditemukan\n" +
                "  whoami, id, ls, cat /root/flag.txt\n" +
                "      perintah di dalam shell\n" +
                "  sudo cat /root/flag.txt\n" +
                "      jalankan sebagai admin (bila punya sudo)\n" +
                "  exit                tutup shell\n" +
                "  clear               membersihkan layar\n" +
                "  help                menampilkan daftar ini"
    }
}

// -------------------------------------------------------- Hack Crypto ---

enum class CryptoScenario {
    NONE,
    HOT_WALLET,
    REENTRANCY
}

/**
 * Dua insiden FIKTIF. Nama bursa "NusaCoin", kontrak "TokenPay", alamat
 * dan angka semuanya karangan — hanya polanya yang terinspirasi berita
 * publik tentang insiden kripto.
 */
class CryptoEngine {

    var scenario: CryptoScenario = CryptoScenario.NONE
        private set
    var aAudited = false
        private set
    var aTraced = false
        private set
    var aFrozen = false
        private set
    var bAudited = false
        private set
    var bDrained = false
        private set
    var bPatched = false
        private set

    val aDone: Boolean
        get() = aTraced && aFrozen
    val bDone: Boolean
        get() = bDrained && bPatched
    val complete: Boolean
        get() = aDone && bDone

    fun prompt(): String = "analis@nusacoin:~$ "

    fun execute(rawInput: String): EngineResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return EngineResult.Text("")
        val parts = input.split(Regex("\\s+"))
        return when (parts[0]) {
            "help" -> EngineResult.Text(HELP_TEXT)
            "clear" -> EngineResult.Clear
            "pilih", "skenario", "scenario" -> runPick(parts)
            "audit" -> runAudit()
            "trace" -> runTrace(parts)
            "freeze" -> runFreeze()
            "exploit" -> runExploit()
            "patch" -> runPatch()
            "status" -> EngineResult.Text(statusText())
            else -> EngineResult.Text(
                "Perintah '${parts[0]}' belum dikenal di lab ini. Ketik help."
            )
        }
    }

    private fun runPick(parts: List<String>): EngineResult {
        if (parts.size < 2) {
            return EngineResult.Text("Pakai: pilih a  ATAU  pilih b")
        }
        return when (parts[1]) {
            "a" -> {
                scenario = CryptoScenario.HOT_WALLET
                EngineResult.Text(
                    "Skenario A dipilih: kebocoran kunci hot wallet " +
                        "bursa fiktif NusaCoin.\n" +
                        "Langkah: audit -> trace TX-7f3a -> freeze."
                )
            }
            "b" -> {
                scenario = CryptoScenario.REENTRANCY
                EngineResult.Text(
                    "Skenario B dipilih: reentrancy pada kontrak fiktif " +
                        "TokenPay.\n" +
                        "Langkah: audit -> exploit -> patch."
                )
            }
            else -> EngineResult.Text("Skenario tidak dikenal: ${parts[1]}. Pilih a atau b.")
        }
    }

    private fun runAudit(): EngineResult {
        return when (scenario) {
            CryptoScenario.HOT_WALLET -> {
                aAudited = true
                EngineResult.Text(
                    "[audit] Bursa fiktif NusaCoin — hot wallet:\n" +
                        "  Saldo hot wallet : 1.240 BTC (dana penarikan harian)\n" +
                        "  Temuan           : kunci privat hot wallet bocor " +
                        "lewat repo publik yang salah konfigurasi.\n" +
                        "  Transaksi keluar : TX-7f3a (1.240 BTC, 02:14 WIB)\n" +
                        "Lacak dengan: trace TX-7f3a"
                )
            }
            CryptoScenario.REENTRANCY -> {
                bAudited = true
                EngineResult.Text(
                    "[audit] Kontrak fiktif TokenPay — fungsi withdraw():\n" +
                        "  function withdraw(uint amount) public {\n" +
                        "      require(balances[msg.sender] >= amount);\n" +
                        "      (bool ok, ) = msg.sender.call{value: amount}(\"\");\n" +
                        "      require(ok);\n" +
                        "      balances[msg.sender] -= amount; " +
                        "// baru dikurangi SETELAH kirim\n" +
                        "  }\n" +
                        "  Temuan: saldo dikurangi SETELAH dana dikirim — " +
                        "penyerang bisa memanggil withdraw lagi sebelum " +
                        "saldo berubah. Saldo kontrak: 860 ETH."
                )
            }
            CryptoScenario.NONE -> EngineResult.Text(
                "Pilih skenario dulu: pilih a (hot wallet) atau pilih b (reentrancy)."
            )
        }
    }

    private fun runTrace(parts: List<String>): EngineResult {
        if (scenario != CryptoScenario.HOT_WALLET) {
            return EngineResult.Text(
                "trace hanya berlaku di skenario A (hot wallet). " +
                    "Pilih skenario dulu: pilih a"
            )
        }
        if (!aAudited) {
            return EngineResult.Text("Audit dulu: audit")
        }
        if (parts.size < 2 || parts[1] != "TX-7f3a") {
            return EngineResult.Text("Pakai: trace TX-7f3a")
        }
        aTraced = true
        return EngineResult.Text(
            "[trace] Aliran dana TX-7f3a (1.240 BTC):\n" +
                "  hop 0: hot-wallet NusaCoin -> bc1qfiktif7f3a...\n" +
                "  hop 1: bc1qfiktif7f3a... -> bc1qmixer9c2... (mixer)\n" +
                "  hop 2: bc1qmixer9c2... -> bc1qpenyerang4d8... " +
                "(ALAMAT PENYERANG)\n" +
                "Alamat penyerang teridentifikasi. Bekukan sisa dana: freeze"
        )
    }

    private fun runFreeze(): EngineResult {
        if (scenario != CryptoScenario.HOT_WALLET) {
            return EngineResult.Text(
                "freeze hanya berlaku di skenario A (hot wallet)."
            )
        }
        if (!aTraced) {
            return EngineResult.Text(
                "Lacak dulu alirannya: trace TX-7f3a"
            )
        }
        aFrozen = true
        return EngineResult.Text(
            "[freeze] Sisa dana 3.860 BTC dipindah ke cold wallet " +
                "multisig (2 dari 3 penanda tangan).\n" +
                "Penarikan dari hot wallet dihentikan. " +
                "Skenario A selesai — sisa dana aman."
        )
    }

    private fun runExploit(): EngineResult {
        if (scenario != CryptoScenario.REENTRANCY) {
            return EngineResult.Text(
                "exploit hanya berlaku di skenario B (reentrancy)."
            )
        }
        if (!bAudited) {
            return EngineResult.Text("Audit kontrak dulu: audit")
        }
        bDrained = true
        return EngineResult.Text(
            "[exploit] Reentrancy berjalan — withdraw dipanggil " +
                "berulang sebelum saldo berkurang:\n" +
                "  panggilan 1: 10 ETH terkirim (saldo tercatat masih 10)\n" +
                "  panggilan 2: 10 ETH terkirim (saldo tercatat masih 10)\n" +
                "  ...\n" +
                "  Kontrak terkuras: 860 ETH -> 0 ETH.\n" +
                "Tambal kontraknya: patch"
        )
    }

    private fun runPatch(): EngineResult {
        if (scenario != CryptoScenario.REENTRANCY) {
            return EngineResult.Text(
                "patch hanya berlaku di skenario B (reentrancy)."
            )
        }
        if (!bDrained) {
            return EngineResult.Text(
                "Buktikan dulu celahnya: exploit"
            )
        }
        bPatched = true
        return EngineResult.Text(
            "[patch] Kontrak ditambal (SIMULASI):\n" +
                "  - checks-effects-interactions: saldo dikurangi " +
                "SEBELUM dana dikirim.\n" +
                "  - reentrancy guard: withdraw tidak bisa dipanggil " +
                "ulang di tengah eksekusi.\n" +
                "Skenario B selesai — kontrak aman."
        )
    }

    fun statusText(): String {
        val a = "A (hot wallet): audit=${mark(aAudited)} " +
            "trace=${mark(aTraced)} freeze=${mark(aFrozen)}"
        val b = "B (reentrancy): audit=${mark(bAudited)} " +
            "exploit=${mark(bDrained)} patch=${mark(bPatched)}"
        return "Status lab kripto (semua data FIKTIF):\n  $a\n  $b"
    }

    private fun mark(done: Boolean): String = if (done) "OK" else "--"

    companion object {
        private const val HELP_TEXT =
            "Perintah lab Hack Crypto (semua data FIKTIF):\n" +
                "  pilih a | pilih b   pilih skenario\n" +
                "  audit               audit wallet / kontrak\n" +
                "  trace TX-7f3a        lacak aliran dana (skenario A)\n" +
                "  freeze              bekukan sisa dana (skenario A)\n" +
                "  exploit             buktikan reentrancy (skenario B)\n" +
                "  patch               tambal kontrak (skenario B)\n" +
                "  status              lihat progres kedua skenario\n" +
                "  clear               membersihkan layar\n" +
                "  help                menampilkan daftar ini"
    }
}
