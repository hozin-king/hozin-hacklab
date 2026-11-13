package com.hozinking.hacklab

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * UI ketujuh simulator lab. Setiap builder mengisi satu container yang
 * diberikan activity, lalu mengembalikan sesi (untuk menghentikan Handler
 * yang berdetak) atau null bila tidak ada yang perlu dihentikan.
 *
 * Semua simulator menyerang target PALSU di dalam aplikasi: tidak ada
 * WebView, tidak ada internet, tidak ada paket yang dikirim.
 */

/** Sesi lab yang perlu dihentikan saat activity dihancurkan. */
interface LabSession {
    fun stop()
}

/**
 * Panel terminal kecil (dipakai lab WiFi, Hack Server, Hack Crypto):
 * satu log monospace yang bisa di-scroll, satu baris input, satu tombol
 * petunjuk. Polanya sama seperti TerminalActivity, tanpa kursor berkedip.
 */
class TerminalPane(
    private val activity: Activity,
    container: LinearLayout,
    private val promptFn: () -> String,
    private val onCommand: (String) -> EngineResult,
    hintText: String
) {
    private val logView: TextView
    private val logScroll: ScrollView
    private var logSpans = SpannableStringBuilder()
    private val input: EditText

    init {
        logView = TextView(activity).apply {
            typeface = Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setBackgroundColor(Ui.CARD)
            setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12))
        }
        logScroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(logView)
        }
        container.addView(logScroll)

        val inputRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(container, 12)
            layoutParams = params
        }
        input = EditText(activity).apply {
            hint = "Ketik perintah…"
            typeface = Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.SECONDARY)
            setBackgroundColor(Ui.CARD)
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_SEND
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
                ) {
                    sendCommand()
                    true
                } else {
                    false
                }
            }
        }
        inputRow.addView(input)
        val sendButton = Ui.primaryButton(activity, "Kirim").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = Ui.dp(container, 8)
            layoutParams = params
            setOnClickListener { sendCommand() }
        }
        inputRow.addView(sendButton)
        container.addView(inputRow)

        val hintButton = Ui.secondaryButton(activity, "Petunjuk").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(container, 8)
            layoutParams = params
            setOnClickListener {
                appendLine("Petunjuk: $hintText")
                printPrompt()
                scrollToBottom()
            }
        }
        container.addView(hintButton)
    }

    fun appendLine(text: String) {
        logSpans.append(text).append("\n")
        logView.text = logSpans
    }

    fun clear() {
        logSpans = SpannableStringBuilder()
        logView.text = logSpans
    }

    fun printPrompt() {
        val start = logSpans.length
        logSpans.append(promptFn())
        logSpans.setSpan(
            ForegroundColorSpan(Ui.ACCENT),
            start,
            logSpans.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        logView.text = logSpans
    }

    fun scrollToBottom() {
        logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun sendCommand() {
        val raw = input.text.toString()
        if (raw.trim().isEmpty()) return
        input.setText("")
        logSpans.append(raw.trim()).append("\n")
        when (val result = onCommand(raw)) {
            is EngineResult.Text -> {
                if (result.text.isNotEmpty()) {
                    logSpans.append(result.text).append("\n")
                }
            }
            is EngineResult.Clear -> {
                logSpans = SpannableStringBuilder()
            }
        }
        logView.text = logSpans
        printPrompt()
        scrollToBottom()
    }
}

/** Membungkus konten form-lab dalam ScrollView yang mengisi container. */
private fun formScroll(activity: Activity, container: LinearLayout): LinearLayout {
    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
    }
    val scroll = ScrollView(activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        addView(content)
    }
    container.addView(scroll)
    return content
}

private fun sectionPanel(activity: Activity, content: LinearLayout): LinearLayout {
    val panel = Ui.panel(activity).apply {
        setPadding(
            Ui.dp(content, 16),
            Ui.dp(content, 16),
            Ui.dp(content, 16),
            Ui.dp(content, 16)
        )
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(content, 12)
        layoutParams = params
    }
    content.addView(panel)
    return panel
}

// ---------------------------------------------------------------- XSS ---

fun buildXssLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val content = formScroll(activity, container)
    content.addView(Ui.label(activity, "BUKU TAMU PALSU").apply {
        setPadding(0, 0, 0, Ui.dp(container, 8))
    })
    content.addView(Ui.small(activity, "Komentar ditampilkan apa adanya — tanpa encoding. Semua pengunjung dan data di sini palsu.").apply {
        setPadding(0, 0, 0, Ui.dp(container, 12))
    })

    val commentsBox = sectionPanel(activity, content)
    fun addComment(name: String, text: String) {
        commentsBox.addView(Ui.body(activity, "$name: $text").apply {
            setPadding(0, 0, 0, Ui.dp(container, 8))
        })
    }
    addComment("Rani", "Situsnya bagus, terus berkarya!")
    addComment("Budi", "Tutorialnya jelas banget.")

    val nameInput = EditText(activity).apply {
        hint = "Namamu"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.CARD)
        maxLines = 1
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 12)
        layoutParams = params
    }
    content.addView(nameInput)
    val commentInput = EditText(activity).apply {
        hint = "Tulis komentarmu… (coba payload XSS)"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.CARD)
        maxLines = 3
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
    }
    content.addView(commentInput)

    val resultLog = Ui.mono(activity, "", 13f, Ui.TEXT)
    val resultPanel = sectionPanel(activity, content)
    resultPanel.addView(Ui.label(activity, "HASIL").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    resultPanel.addView(resultLog)

    val alertBox = sectionPanel(activity, content).apply {
        visibility = View.GONE
    }
    alertBox.addView(TextView(activity).apply {
        text = "ALERT (palsu)"
        textSize = 17f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Ui.ACCENT)
    })
    alertBox.addView(Ui.body(activity, "alert(1) — dialog ini PALSU, dibuat simulator untuk menunjukkan apa yang terjadi di browser korban saat payloadmu dijalankan."))

    val cookieBox = sectionPanel(activity, content).apply {
        visibility = View.GONE
    }
    cookieBox.addView(Ui.label(activity, "COOKIE YANG DICURI (palsu)").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    cookieBox.addView(Ui.mono(activity, XssEngine.FAKE_COOKIE, 13f, Ui.ACCENT))
    cookieBox.addView(Ui.small(activity, "Cookie sesi palsu ini \"dikirim\" ke server penyerang (simulasi). Di dunia nyata, penyerang lalu masuk sebagai korban tanpa password.").apply {
        setPadding(0, Ui.dp(container, 4), 0, 0)
    })

    var flagged = false
    val sendButton = Ui.primaryButton(activity, "Kirim komentar").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 12)
        layoutParams = params
        setOnClickListener {
            val payload = commentInput.text.toString()
            if (payload.isBlank()) return@setOnClickListener
            val name = nameInput.text.toString().ifBlank { "Anonim" }
            addComment(name, payload)
            val result = XssEngine.classify(payload)
            resultLog.text = "Jenis: ${result.kind}\n${result.explanation}"
            if (result.triggered) {
                alertBox.visibility = View.VISIBLE
                if (result.cookieStolen) cookieBox.visibility = View.VISIBLE
                if (!flagged) {
                    flagged = true
                    onFlag()
                }
            }
            commentInput.setText("")
        }
    }
    content.addView(sendButton)
    return null
}

// ---------------------------------------------------------------- CSS ---

private fun parseCssColor(value: String): Int? {
    return try {
        Color.parseColor(value.trim())
    } catch (e: Exception) {
        null
    }
}

fun buildCssLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val content = formScroll(activity, container)
    content.addView(Ui.label(activity, "HALAMAN PROFIL PALSU").apply {
        setPadding(0, 0, 0, Ui.dp(container, 8))
    })
    content.addView(Ui.small(activity, "Halaman ini merender CSS kiriman pengguna tanpa sanitasi. Semua elemen palsu.").apply {
        setPadding(0, 0, 0, Ui.dp(container, 12))
    })

    val pageBox = sectionPanel(activity, content)
    val headerView = Ui.mono(activity, "PROFIL — @pemula", 14f, Ui.ACCENT).apply {
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 0, 0, Ui.dp(container, 8))
    }
    pageBox.addView(headerView)
    val avatarView = View(activity).apply {
        setBackgroundColor(Color.parseColor("#2962FF"))
        layoutParams = LinearLayout.LayoutParams(Ui.dp(container, 56), Ui.dp(container, 56))
    }
    pageBox.addView(avatarView)
    val nameView = Ui.body(activity, "Pemula HackLab").apply {
        setPadding(0, Ui.dp(container, 8), 0, 0)
    }
    pageBox.addView(nameView)
    val bioView = Ui.small(activity, "Belajar terminal dan keamanan siber.").apply {
        setPadding(0, Ui.dp(container, 2), 0, 0)
    }
    pageBox.addView(bioView)
    val followButton = Ui.secondaryButton(activity, "Ikuti").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
    }
    pageBox.addView(followButton)

    val cssInput = EditText(activity).apply {
        hint = "Tulis CSS… contoh: .profil { background: red; }"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.CARD)
        maxLines = 4
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 12)
        layoutParams = params
    }
    content.addView(cssInput)

    val resultLog = Ui.mono(activity, "", 13f, Ui.TEXT)
    val resultPanel = sectionPanel(activity, content)
    resultPanel.addView(Ui.label(activity, "HASIL").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    resultPanel.addView(resultLog)

    val overlayBox = sectionPanel(activity, content).apply {
        visibility = View.GONE
    }
    overlayBox.addView(TextView(activity).apply {
        text = "FORM LOGIN PALSU (overlay)"
        textSize = 17f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Ui.ACCENT)
    })
    overlayBox.addView(Ui.body(activity, "Form ini muncul karena CSS suntikanmu menimpanya di atas halaman. Di dunia nyata, korban mengira ini login asli.").apply {
        setPadding(0, Ui.dp(container, 4), 0, 0)
    })
    val fakeUser = EditText(activity).apply {
        hint = "Username (jangan isi password asli)"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.BG)
        maxLines = 1
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
    }
    overlayBox.addView(fakeUser)
    val fakePass = EditText(activity).apply {
        hint = "Password (jangan isi password asli)"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.BG)
        maxLines = 1
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
    }
    overlayBox.addView(fakePass)
    val fakeLoginButton = Ui.primaryButton(activity, "Masuk (palsu)").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
        setOnClickListener {
            resultLog.text = "Form PALSU — jangan pernah mengetik password asli " +
                "ke form yang muncul dari suntikan seperti ini."
        }
    }
    overlayBox.addView(fakeLoginButton)

    var defaced = false
    var overlayShown = false
    var flagged = false
    val logLines = ArrayList<String>()
    fun appendLog(text: String) {
        logLines.add(text)
        resultLog.text = logLines.joinToString("\n")
    }

    fun applyRule(rule: CssRule) {
        val target: View? = when (rule.selector) {
            "body" -> pageBox
            ".header" -> headerView
            ".profil" -> nameView
            ".tombol" -> followButton
            ".bio" -> bioView
            else -> null
        }
        if (target == null) {
            appendLog("Selektor ${rule.selector} tidak dikenal di halaman ini — diabaikan.")
            return
        }
        for ((prop, value) in rule.props) {
            when (prop) {
                "background" -> {
                    val color = parseCssColor(value)
                    if (color != null) {
                        target.setBackgroundColor(color)
                        appendLog("Aturan ${rule.selector} { background: $value } diterapkan.")
                    } else {
                        appendLog("Warna tidak dikenal: $value — diabaikan.")
                    }
                }
                "color" -> {
                    val color = parseCssColor(value)
                    if (color != null && target is TextView) {
                        target.setTextColor(color)
                        appendLog("Aturan ${rule.selector} { color: $value } diterapkan.")
                    } else {
                        appendLog("Warna tidak dikenal: $value — diabaikan.")
                    }
                }
                "display" -> {
                    target.visibility = if (value == "none") View.GONE else View.VISIBLE
                    appendLog("Aturan ${rule.selector} { display: $value } diterapkan.")
                }
                "font-size" -> {
                    val size = value.removeSuffix("px").toFloatOrNull()
                    if (size != null && target is TextView) {
                        target.textSize = size
                        appendLog("Aturan ${rule.selector} { font-size: $value } diterapkan.")
                    } else {
                        appendLog("Ukuran tidak dikenal: $value — diabaikan.")
                    }
                }
                else -> appendLog("Properti $prop tidak didukung simulator ini — diabaikan.")
            }
        }
    }

    val applyButton = Ui.primaryButton(activity, "Terapkan CSS").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 12)
        layoutParams = params
        setOnClickListener {
            val input = cssInput.text.toString()
            if (input.isBlank()) return@setOnClickListener
            val rules = CssEngine.parse(input)
            if (rules.isEmpty()) {
                appendLog("Tidak ada aturan CSS yang dikenali. Contoh: .profil { background: red; }")
                return@setOnClickListener
            }
            for (rule in rules) {
                if (CssEngine.isOverlayRule(rule)) {
                    overlayBox.visibility = View.VISIBLE
                    overlayShown = true
                    appendLog("Aturan .login-palsu { position: fixed } — overlay login PALSU muncul di atas halaman!")
                } else if (CssEngine.isDefaceRule(rule)) {
                    defaced = true
                    applyRule(rule)
                } else {
                    appendLog("Aturan ${rule.selector} tidak mengubah halaman ini — diabaikan.")
                }
            }
            if (CssEngine.mentionsExfiltration(input)) {
                appendLog("Teori exfiltrasi: input[value^=\"...\"] { background: url(...) } — satu huruf demi satu huruf bisa bocor lewat URL yang dimuat.")
            }
            if (defaced && overlayShown && !flagged) {
                flagged = true
                onFlag()
            }
        }
    }
    content.addView(applyButton)
    val resetButton = Ui.secondaryButton(activity, "Reset halaman").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
        setOnClickListener {
            pageBox.setBackgroundColor(Ui.CARD)
            headerView.setBackgroundColor(Color.TRANSPARENT)
            headerView.setTextColor(Ui.ACCENT)
            nameView.setBackgroundColor(Color.TRANSPARENT)
            nameView.setTextColor(Ui.TEXT)
            nameView.visibility = View.VISIBLE
            nameView.textSize = 15f
            bioView.setBackgroundColor(Color.TRANSPARENT)
            bioView.setTextColor(Ui.SECONDARY)
            bioView.visibility = View.VISIBLE
            bioView.textSize = 13f
            followButton.setBackgroundColor(Ui.CARD)
            followButton.setTextColor(Ui.TEXT)
            followButton.visibility = View.VISIBLE
            overlayBox.visibility = View.GONE
            defaced = false
            overlayShown = false
            appendLog("Halaman direset ke tampilan awal.")
        }
    }
    content.addView(resetButton)
    return null
}

// --------------------------------------------------------------- SQLi ---

fun buildSqliLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val content = formScroll(activity, container)
    content.addView(Ui.label(activity, "LOGIN PALSU — DATABASE PALSU").apply {
        setPadding(0, 0, 0, Ui.dp(container, 8))
    })
    content.addView(Ui.small(activity, "Form ini membangun query dengan menempelkan inputmu langsung. Tabel users di bawah ini palsu.").apply {
        setPadding(0, 0, 0, Ui.dp(container, 12))
    })

    val formPanel = sectionPanel(activity, content)
    val userInput = EditText(activity).apply {
        hint = "Username"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.BG)
        maxLines = 1
    }
    formPanel.addView(userInput)
    val passInput = EditText(activity).apply {
        hint = "Password"
        textSize = 15f
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.SECONDARY)
        setBackgroundColor(Ui.BG)
        maxLines = 1
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
    }
    formPanel.addView(passInput)

    val logView = Ui.mono(activity, "", 13f, Ui.TEXT)
    val logPanel = sectionPanel(activity, content)
    logPanel.addView(Ui.label(activity, "QUERY & HASIL").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    logPanel.addView(logView)

    val logLines = ArrayList<String>()
    fun appendLog(text: String) {
        logLines.add(text)
        logView.text = logLines.joinToString("\n")
    }

    var bypassed = false
    var dumped = false
    var flagged = false
    val loginButton = Ui.primaryButton(activity, "Masuk").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 12)
        layoutParams = params
        setOnClickListener {
            val user = userInput.text.toString()
            val pass = passInput.text.toString()
            appendLog("Query yang terbentuk:")
            appendLog(SqliEngine.buildQuery(user, pass))
            when (val outcome = SqliEngine.attempt(user, pass)) {
                is SqliOutcome.Fail -> appendLog("Login gagal. ${outcome.note}")
                is SqliOutcome.Normal -> {
                    appendLog("Login berhasil sebagai ${outcome.user.username}. ${outcome.note}")
                }
                is SqliOutcome.Bypass -> {
                    appendLog("Login BERHASIL sebagai admin — tanpa password! ${outcome.note}")
                    bypassed = true
                }
                is SqliOutcome.Dump -> {
                    appendLog("Dump tabel users (${outcome.rows.size} baris):")
                    for (row in outcome.rows) {
                        appendLog("${row.id} | ${row.username} | ${row.passwordHash}")
                    }
                    appendLog(outcome.note)
                    dumped = true
                }
            }
            if (bypassed && dumped && !flagged) {
                flagged = true
                onFlag()
            }
        }
    }
    content.addView(loginButton)
    return null
}

// --------------------------------------------------------------- WiFi ---

fun buildWifiLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val engine = WifiEngine()
    var flagged = false
    val pane = TerminalPane(activity, container, { engine.prompt() }, { cmd ->
        val result = engine.execute(cmd)
        if (engine.disconnected.isNotEmpty() && !flagged) {
            flagged = true
            onFlag()
        }
        result
    }, lab.hint)
    pane.appendLine("Kali Linux (SIMULASI) — kartu wlan0, semua jaringan dan klien palsu.")
    pane.appendLine("Tidak ada frame yang benar-benar dikirim dari HP ini.")
    pane.appendLine("Ketik help untuk melihat perintah.")
    pane.printPrompt()
    pane.scrollToBottom()
    return null
}

// --------------------------------------------------------------- DDoS ---

private fun ddosHealth(engine: DdosEngine): String {
    return "TARGET PALSU: 203.0.113.10:80 (server simulasi)\n" +
        "Status   : ${engine.status}\n" +
        "Request  : ${engine.reqPerSec} req/detik\n" +
        "Latensi  : ${engine.latencyMs} ms\n" +
        "CPU      : ${engine.cpu}%\n" +
        "Botnet   : ${engine.bots} bot siap\n" +
        "Serangan : ${engine.attack.label}"
}

private fun ddosGraph(history: List<Int>): String {
    val rows = StringBuilder()
    val thresholds = listOf(88, 76, 64, 52, 40, 28, 16, 4)
    for (t in thresholds) {
        rows.append("%3d%% |".format(t))
        for (v in history) rows.append(if (v >= t) "█" else " ")
        rows.append("\n")
    }
    rows.append("      +")
    for (i in history.indices) rows.append("-")
    rows.append("\n")
    return rows.toString().trimEnd()
}

fun buildDdosLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val engine = DdosEngine()
    val content = formScroll(activity, container)
    content.addView(Ui.label(activity, "KONSOL BOTNET (SIMULASI)").apply {
        setPadding(0, 0, 0, Ui.dp(container, 8))
    })
    content.addView(Ui.small(activity, "Semua trafik simulasi — tidak ada satu paket pun yang dikirim ke host mana pun. Yang tumbang hanya server palsu ini.").apply {
        setPadding(0, 0, 0, Ui.dp(container, 12))
    })

    val healthView = Ui.mono(activity, ddosHealth(engine), 13f, Ui.TEXT)
    val healthPanel = sectionPanel(activity, content)
    healthPanel.addView(Ui.label(activity, "KESEHATAN SERVER").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    healthPanel.addView(healthView)

    val graphView = Ui.mono(activity, "Grafik CPU muncul setelah beberapa detik.", 13f, Ui.ACCENT)
    val graphPanel = sectionPanel(activity, content)
    graphPanel.addView(Ui.label(activity, "GRAFIK CPU (per detik)").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    graphPanel.addView(graphView)

    val logView = Ui.mono(activity, "", 13f, Ui.TEXT)
    val logPanel = sectionPanel(activity, content)
    logPanel.addView(Ui.label(activity, "LOG SERANGAN").apply {
        setPadding(0, 0, 0, Ui.dp(container, 4))
    })
    logPanel.addView(logView)

    val logLines = ArrayList<String>()
    fun appendLog(text: String) {
        logLines.add(text)
        if (logLines.size > 14) logLines.removeAt(0)
        logView.text = logLines.joinToString("\n")
    }

    val attacks = listOf(DdosAttack.SYN, DdosAttack.UDP, DdosAttack.HTTP)
    for (attack in attacks) {
        val button = Ui.primaryButton(activity, attack.label).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(container, 8)
            layoutParams = params
            setOnClickListener {
                appendLog(engine.startAttack(attack))
            }
        }
        content.addView(button)
    }
    val stopButton = Ui.secondaryButton(activity, "Hentikan serangan").apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = Ui.dp(container, 8)
        layoutParams = params
        setOnClickListener {
            appendLog(engine.stopAttack())
        }
    }
    content.addView(stopButton)

    val mitigationButtons = LinkedHashMap<String, Button>()
    val mitigations = listOf(
        "rate" to "Rate Limit",
        "waf" to "WAF/CDN",
        "anycast" to "Anycast"
    )
    for ((key, label) in mitigations) {
        val button = Ui.secondaryButton(activity, "$label: MATI").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(container, 8)
            layoutParams = params
            setOnClickListener {
                val line = engine.toggleMitigation(key)
                val active = when (key) {
                    "rate" -> engine.rateLimit
                    "waf" -> engine.waf
                    else -> engine.anycast
                }
                text = "$label: " + if (active) "AKTIF" else "MATI"
                appendLog(line)
            }
        }
        mitigationButtons[key] = button
        content.addView(button)
    }

    val handler = Handler(Looper.getMainLooper())
    var running = true
    val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            appendLog(engine.tick())
            healthView.text = ddosHealth(engine)
            graphView.text = ddosGraph(engine.cpuHistory)
            if (engine.complete) onFlag()
            handler.postDelayed(this, 900)
        }
    }
    handler.postDelayed(tickRunnable, 900)
    return object : LabSession {
        override fun stop() {
            running = false
            handler.removeCallbacks(tickRunnable)
        }
    }
}

// --------------------------------------------------------- Hack Server ---

fun buildServerLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val engine = ServerHackEngine()
    var flagged = false
    val pane = TerminalPane(activity, container, { engine.prompt() }, { cmd ->
        val result = engine.execute(cmd)
        if (engine.flagGot && !flagged) {
            flagged = true
            onFlag()
        }
        result
    }, lab.hint)
    pane.appendLine("HackLab — target PALSU 203.0.113.10 (alamat dokumentasi TEST-NET).")
    pane.appendLine("Ketik help untuk melihat perintah.")
    pane.printPrompt()
    pane.scrollToBottom()
    return null
}

// -------------------------------------------------------- Hack Crypto ---

fun buildCryptoLab(
    activity: Activity,
    container: LinearLayout,
    lab: LabDef,
    onFlag: () -> Unit
): LabSession? {
    val engine = CryptoEngine()
    var flagged = false
    val pane = TerminalPane(activity, container, { engine.prompt() }, { cmd ->
        val result = engine.execute(cmd)
        if (engine.complete && !flagged) {
            flagged = true
            onFlag()
        }
        result
    }, lab.hint)
    pane.appendLine("Lab insiden kripto — semua nama, alamat, dan angka FIKTIF.")
    pane.appendLine("Pola yang dipelajari terinspirasi berita publik, tanpa detail insiden nyata.")
    pane.appendLine("Pilih skenario: pilih a (hot wallet) atau pilih b (reentrancy). Ketik help untuk perintah.")
    pane.printPrompt()
    pane.scrollToBottom()
    return null
}
