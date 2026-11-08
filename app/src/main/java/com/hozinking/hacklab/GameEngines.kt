@file:OptIn(kotlin.ExperimentalStdlibApi::class)
package com.hozinking.hacklab

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

// The five offline mini-game engines. Each one builds its own UI into
// the container it is given and calls onWin exactly when its level is
// really solved. stop() cancels any pending animation (the host calls
// it when the player leaves the level or the activity dies).

interface MiniGameEngine {
    fun stop()
}

typealias Pos = Pair<Int, Int>

// A short log that keeps only the last lines, so it never grows forever.
private class MiniLog(private val view: TextView, private val maxLines: Int = 12) {
    private val lines = ArrayList<String>()
    fun add(text: String) {
        lines.add(text)
        while (lines.size > maxLines) lines.removeAt(0)
        view.text = lines.joinToString("\n")
    }

    fun clear() {
        lines.clear()
        view.text = ""
    }
}

private fun logView(activity: Activity): TextView {
    return TextView(activity).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 13f
        setTextColor(Ui.TEXT)
        setBackgroundColor(Ui.CARD)
        setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12))
    }
}

private fun gridView(activity: Activity): TextView {
    return TextView(activity).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 20f
        setTextColor(Ui.TEXT)
        setBackgroundColor(Ui.CARD)
        setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12))
    }
}

private fun spacedParams(activity: Activity, top: Int): LinearLayout.LayoutParams {
    val params = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    params.topMargin = (top * activity.resources.displayMetrics.density).toInt()
    return params
}

// ---------------------------------------------------------------------------
// 1. Code Combat: "Misi Kode". Type commands (maju / kiri / kanan / ambil),
// tap Jalankan, and the hero executes them one by one. Win: every gem
// collected with ambil, and the hero standing on the target.
// ---------------------------------------------------------------------------
class CodeCombatEngine(
    private val activity: Activity,
    private val root: LinearLayout,
    private val level: CombatLevel,
    private val onWin: () -> Unit
) : MiniGameEngine {

    private val handler = Handler(Looper.getMainLooper())
    private val dirs = listOf(Pos(1, 0), Pos(0, 1), Pos(-1, 0), Pos(0, -1))
    private val arrows = listOf('>', 'v', '<', '^')

    private lateinit var gridText: TextView
    private lateinit var log: MiniLog
    private lateinit var input: EditText
    private lateinit var runButton: Button
    private lateinit var resetButton: Button

    private var hero = Pos(0, 0)
    private var facing = 0
    private var target = Pos(0, 0)
    private var gems = HashSet<Pos>()
    private var collected = HashSet<Pos>()
    private var running = false

    init {
        build()
        reset()
    }

    private fun build() {
        root.addView(Ui.label(activity, "MISI KODE — ${level.name}"))
        root.addView(Ui.small(activity, level.hint).apply {
            layoutParams = spacedParams(activity, 4)
        })
        gridText = gridView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(gridText)
        val logText = logView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(logText)
        log = MiniLog(logText)
        input = EditText(activity).apply {
            hint = "maju\nkiri\nkanan\nambil"
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.SECONDARY)
            setBackgroundColor(Ui.CARD)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(input)
        runButton = Ui.primaryButton(activity, "Jalankan").apply {
            layoutParams = spacedParams(activity, 12)
            setOnClickListener { runProgram() }
        }
        root.addView(runButton)
        resetButton = Ui.secondaryButton(activity, "Reset").apply {
            layoutParams = spacedParams(activity, 8)
            setOnClickListener { reset() }
        }
        root.addView(resetButton)
    }

    private fun reset() {
        handler.removeCallbacksAndMessages(null)
        running = false
        hero = Pos(0, 0)
        facing = 0
        gems = HashSet()
        collected = HashSet()
        for (y in level.grid.indices) {
            for (x in level.grid[y].indices) {
                when (level.grid[y][x]) {
                    'H' -> hero = Pos(x, y)
                    'G' -> gems.add(Pos(x, y))
                    'T' -> target = Pos(x, y)
                }
            }
        }
        log.clear()
        log.add("Ketik perintahmu, lalu tap Jalankan.")
        render()
        runButton.isEnabled = true
        input.setText("")
    }

    private fun render() {
        val sb = StringBuilder()
        for (y in level.grid.indices) {
            for (x in level.grid[y].indices) {
                val p = Pos(x, y)
                sb.append(
                    when {
                        p == hero -> arrows[facing]
                        p in collected -> '.'
                        else -> level.grid[y][x]
                    }
                )
            }
            if (y < level.grid.size - 1) sb.append('\n')
        }
        gridText.text = sb.toString()
    }

    private fun runProgram() {
        if (running) return
        val raw = input.text.toString()
        val cmds = raw.lowercase().split("\\s+".toRegex())
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (cmds.isEmpty()) {
            log.add("Program kosong. Ketik perintah dulu.")
            return
        }
        for (c in cmds) {
            if (c != "maju" && c != "kiri" && c != "kanan" && c != "ambil") {
                log.add("Perintah tidak dikenal: $c")
                return
            }
        }
        running = true
        runButton.isEnabled = false
        log.add("Menjalankan ${cmds.size} perintah…")
        step(cmds, 0)
    }

    private fun step(cmds: List<String>, index: Int) {
        if (!running) return
        if (index >= cmds.size) {
            finishRun()
            return
        }
        if (!executeOne(cmds[index])) {
            running = false
            runButton.isEnabled = true
            return
        }
        render()
        handler.postDelayed({ step(cmds, index + 1) }, 450)
    }

    // Returns false when the program must stop (an error).
    private fun executeOne(cmd: String): Boolean {
        when (cmd) {
            "kiri" -> facing = (facing + 3) % 4
            "kanan" -> facing = (facing + 1) % 4
            "maju" -> {
                val nx = hero.first + dirs[facing].first
                val ny = hero.second + dirs[facing].second
                if (ny !in level.grid.indices || nx !in level.grid[ny].indices ||
                    level.grid[ny][nx] == '#'
                ) {
                    log.add("Dinding! Program berhenti.")
                    return false
                }
                hero = Pos(nx, ny)
            }
            "ambil" -> {
                if (hero in gems && hero !in collected) {
                    collected.add(hero)
                    log.add("Permata diambil (${collected.size}/${gems.size}).")
                } else {
                    log.add("Tidak ada permata di sini. Program berhenti.")
                    return false
                }
            }
        }
        return true
    }

    private fun finishRun() {
        running = false
        runButton.isEnabled = true
        if (collected.size == gems.size && hero == target) {
            log.add("Misi selesai! Semua permata diambil.")
            onWin()
        } else {
            log.add("Program selesai, tapi misi belum tuntas. Coba lagi.")
        }
    }

    override fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}

// ---------------------------------------------------------------------------
// 2. Bitburner: "Retas Server". A fake terminal: scan, connect <port>,
// then crack <4-digit PIN> with bigger/smaller hints. Win: correct PIN
// before the tries run out.
// ---------------------------------------------------------------------------
class BitburnerEngine(
    private val activity: Activity,
    private val root: LinearLayout,
    private val level: HackLevel,
    private val onWin: () -> Unit
) : MiniGameEngine {

    private lateinit var log: MiniLog
    private lateinit var input: EditText

    private var scanned = false
    private var connected: Int? = null
    private var tries = 0
    private var locked = false
    private var won = false

    init {
        build()
        reset()
    }

    private fun build() {
        root.addView(Ui.label(activity, "RETAS SERVER — ${level.name}"))
        root.addView(Ui.small(activity, level.hint).apply {
            layoutParams = spacedParams(activity, 4)
        })
        root.addView(Ui.small(activity, "Perintah: scan · connect <port> · crack <PIN> · help").apply {
            layoutParams = spacedParams(activity, 4)
        })
        val logText = logView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(logText)
        log = MiniLog(logText, 14)
        val inputRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = spacedParams(activity, 12)
        }
        input = EditText(activity).apply {
            hint = "Ketik perintah…"
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.SECONDARY)
            setBackgroundColor(Ui.CARD)
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_SEND
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
                ) {
                    sendCommand()
                    true
                } else false
            }
        }
        inputRow.addView(input)
        inputRow.addView(Ui.primaryButton(activity, "Kirim").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { sendCommand() }
        })
        root.addView(inputRow)
        root.addView(Ui.secondaryButton(activity, "Reset").apply {
            layoutParams = spacedParams(activity, 8)
            setOnClickListener { reset() }
        })
    }

    private fun reset() {
        scanned = false
        connected = null
        tries = 0
        locked = false
        won = false
        log.clear()
        log.add("Target: ${level.server}")
        log.add("Ketik scan untuk melihat port yang terbuka.")
        input.setText("")
    }

    private fun sendCommand() {
        if (won) return
        val raw = input.text.toString().trim()
        if (raw.isEmpty()) return
        input.setText("")
        log.add("> $raw")
        val parts = raw.split("\\s+".toRegex())
        when (parts[0].lowercase()) {
            "scan" -> {
                scanned = true
                val names = level.ports.joinToString(", ") { "$it (${serviceName(it)})" }
                log.add("Port terbuka: $names")
            }
            "connect" -> {
                if (!scanned) {
                    log.add("Scan dulu sebelum connect.")
                    return
                }
                val port = parts.getOrNull(1)?.toIntOrNull()
                if (port == null) {
                    log.add("Pakai: connect <port>")
                } else if (port in level.ports) {
                    connected = port
                    log.add("Terhubung ke port $port.")
                } else {
                    log.add("Port $port tertutup.")
                }
            }
            "crack" -> {
                if (connected == null) {
                    log.add("Belum terhubung. Ketik connect <port> dulu.")
                    return
                }
                if (locked) {
                    log.add("Server mengunci koneksi. Reset untuk coba lagi.")
                    return
                }
                val guess = parts.getOrNull(1) ?: ""
                if (guess.length != 4 || guess.any { !it.isDigit() }) {
                    log.add("PIN harus 4 digit angka.")
                    return
                }
                tries++
                if (guess == level.pin) {
                    won = true
                    log.add("AKSES DIBERIKAN. Server ${level.server} berhasil diretas.")
                    onWin()
                } else if (tries >= level.maxTries) {
                    locked = true
                    log.add("Terlalu banyak percobaan. Server mengunci koneksi.")
                } else if (guess < level.pin) {
                    log.add("PIN lebih besar dari $guess. (${tries}/${level.maxTries})")
                } else {
                    log.add("PIN lebih kecil dari $guess. (${tries}/${level.maxTries})")
                }
            }
            "help" -> log.add("scan · connect <port> · crack <PIN> · help")
            else -> log.add("Perintah tidak dikenal. Ketik help.")
        }
    }

    private fun serviceName(port: Int): String {
        return when (port) {
            21 -> "ftp"
            22 -> "ssh"
            80 -> "http"
            443 -> "https"
            1337 -> "game"
            3306 -> "mysql"
            8080 -> "proxy"
            else -> "layanan"
        }
    }

    override fun stop() {
        // No animation to cancel.
    }
}

// ---------------------------------------------------------------------------
// 3. CodinGame: "Debug Kode". A short program with exactly one bug.
// The player picks which line is wrong. Win: the right line.
// ---------------------------------------------------------------------------
class CodinGameEngine(
    private val activity: Activity,
    private val root: LinearLayout,
    private val level: DebugLevel,
    private val onWin: () -> Unit
) : MiniGameEngine {

    private lateinit var feedback: TextView
    private var won = false

    init {
        build()
    }

    private fun build() {
        root.addView(Ui.label(activity, "DEBUG KODE — ${level.name}"))
        root.addView(Ui.small(activity, "Satu baris di program ini salah. Baris mana?").apply {
            layoutParams = spacedParams(activity, 4)
        })
        val codeText = TextView(activity).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setBackgroundColor(Ui.CARD)
            setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12))
            layoutParams = spacedParams(activity, 12)
            text = level.code.mapIndexed { i, line -> "${i + 1}  $line" }.joinToString("\n")
        }
        root.addView(codeText)
        feedback = Ui.body(activity, "").apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(feedback)
        for (i in level.code.indices) {
            root.addView(Ui.secondaryButton(activity, "Baris ${i + 1}").apply {
                layoutParams = spacedParams(activity, 8)
                setOnClickListener { pick(i + 1) }
            })
        }
    }

    private fun pick(line: Int) {
        if (won) return
        if (line == level.bugLine) {
            won = true
            feedback.text = "Benar! ${level.explain}"
            feedback.setTextColor(Ui.ACCENT)
            onWin()
        } else {
            feedback.text = "Bukan baris itu. Coba lagi."
            feedback.setTextColor(Ui.TEXT)
        }
    }

    override fun stop() {
        // No animation to cancel.
    }
}

// ---------------------------------------------------------------------------
// 4. Screeps: "Program Bot". Build a short program from kumpul / antar /
// ulang, then the bot runs it on its own: kumpul walks (shortest path) to
// the nearest resource and picks it up, antar walks home and deposits,
// ulang restarts the program (up to 3 runs total). Win: every resource
// deposited before the step budget runs out. The bot carries at most 2.
// ---------------------------------------------------------------------------
class ScreepsEngine(
    private val activity: Activity,
    private val root: LinearLayout,
    private val level: BotLevel,
    private val onWin: () -> Unit
) : MiniGameEngine {

    private val handler = Handler(Looper.getMainLooper())
    private val dirs = listOf(Pos(1, 0), Pos(-1, 0), Pos(0, 1), Pos(0, -1))

    private lateinit var gridText: TextView
    private lateinit var statusText: TextView
    private lateinit var programText: TextView
    private lateinit var log: MiniLog
    private lateinit var runButton: Button

    private var bot = Pos(0, 0)
    private var base = Pos(0, 0)
    private var resources = HashSet<Pos>()
    private var totalResources = 0
    private var delivered = 0
    private var carry = 0
    private var steps = 0
    private val program = ArrayList<String>()
    private var running = false
    private var runIndex = 0
    private var loops = 0
    private var won = false

    init {
        build()
        reset()
    }

    private fun build() {
        root.addView(Ui.label(activity, "PROGRAM BOT — ${level.name}"))
        root.addView(Ui.small(activity, level.hint).apply {
            layoutParams = spacedParams(activity, 4)
        })
        gridText = gridView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(gridText)
        statusText = Ui.mono(activity, "", 13f, Ui.ACCENT).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(statusText)
        programText = Ui.body(activity, "").apply {
            layoutParams = spacedParams(activity, 4)
        }
        root.addView(programText)
        val logText = logView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(logText)
        log = MiniLog(logText)
        val actionRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = spacedParams(activity, 12)
        }
        for (action in listOf("kumpul", "antar", "ulang")) {
            actionRow.addView(Ui.secondaryButton(activity, action).apply {
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (action != "kumpul") params.leftMargin = Ui.dp(this, 8)
                layoutParams = params
                setOnClickListener { addAction(action) }
            })
        }
        root.addView(actionRow)
        root.addView(Ui.secondaryButton(activity, "Hapus aksi terakhir").apply {
            layoutParams = spacedParams(activity, 8)
            setOnClickListener {
                if (!running && program.isNotEmpty()) {
                    program.removeAt(program.size - 1)
                    renderProgram()
                }
            }
        })
        runButton = Ui.primaryButton(activity, "Jalankan").apply {
            layoutParams = spacedParams(activity, 12)
            setOnClickListener { runProgram() }
        }
        root.addView(runButton)
        root.addView(Ui.secondaryButton(activity, "Reset").apply {
            layoutParams = spacedParams(activity, 8)
            setOnClickListener { reset() }
        })
    }

    private fun reset() {
        handler.removeCallbacksAndMessages(null)
        running = false
        won = false
        bot = Pos(0, 0)
        base = Pos(0, 0)
        resources = HashSet()
        for (y in level.grid.indices) {
            for (x in level.grid[y].indices) {
                when (level.grid[y][x]) {
                    'B' -> bot = Pos(x, y)
                    'H' -> base = Pos(x, y)
                    'R' -> resources.add(Pos(x, y))
                }
            }
        }
        totalResources = resources.size
        delivered = 0
        carry = 0
        steps = 0
        program.clear()
        log.clear()
        log.add("Susun program dari aksi, lalu tap Jalankan.")
        render()
        renderProgram()
        runButton.isEnabled = true
    }

    private fun addAction(action: String) {
        if (running) return
        if (program.size >= 6) {
            log.add("Program maksimal 6 aksi.")
            return
        }
        program.add(action)
        renderProgram()
    }

    private fun renderProgram() {
        programText.text = if (program.isEmpty()) {
            "Program: (kosong)"
        } else {
            "Program: " + program.joinToString(" → ")
        }
    }

    private fun render() {
        val sb = StringBuilder()
        for (y in level.grid.indices) {
            for (x in level.grid[y].indices) {
                val p = Pos(x, y)
                sb.append(
                    when {
                        p == bot -> 'B'
                        p in resources -> 'R'
                        else -> level.grid[y][x]
                    }
                )
            }
            if (y < level.grid.size - 1) sb.append('\n')
        }
        gridText.text = sb.toString()
        statusText.text = "Diantar: $delivered/$totalResources · Langkah: $steps/${level.budget} · Dibawa: $carry/2"
    }

    private fun runProgram() {
        if (running || won) return
        if (program.isEmpty()) {
            log.add("Program kosong. Tambah aksi dulu.")
            return
        }
        running = true
        runIndex = 0
        loops = 0
        runButton.isEnabled = false
        log.add("Menjalankan program…")
        nextAction()
    }

    private fun nextAction() {
        if (!running) return
        if (runIndex >= program.size) {
            endRun()
            return
        }
        val action = program[runIndex]
        runIndex++
        when (action) {
            "kumpul" -> doCollect()
            "antar" -> doDeliver()
            "ulang" -> {
                if (loops < 2) {
                    loops++
                    runIndex = 0
                    log.add("Ulang dari awal (putaran ${loops + 1}/3).")
                    nextAction()
                } else {
                    endRun()
                }
            }
        }
    }

    private fun doCollect() {
        if (carry >= 2) {
            fail("Bot sudah membawa 2 resource. Antar dulu.")
            return
        }
        if (resources.isEmpty()) {
            fail("Tidak ada resource lagi di peta.")
            return
        }
        var best: List<Pos>? = null
        var bestTarget: Pos? = null
        for (r in resources) {
            val path = bfs(bot, r)
            if (path != null && (best == null || path.size < best.size)) {
                best = path
                bestTarget = r
            }
        }
        if (best == null || bestTarget == null) {
            fail("Resource tidak terjangkau.")
            return
        }
        val target = bestTarget
        animatePath(best) {
            resources.remove(target)
            carry++
            log.add("Kumpul resource. Dibawa: $carry/2.")
            render()
            nextAction()
        }
    }

    private fun doDeliver() {
        val path = bfs(bot, base)
        if (path == null) {
            fail("Base tidak terjangkau.")
            return
        }
        animatePath(path) {
            delivered += carry
            carry = 0
            log.add("Antar: total diantar $delivered/$totalResources.")
            render()
            if (delivered >= totalResources) {
                running = false
                runButton.isEnabled = true
                log.add("Semua resource diantar!")
                won = true
                onWin()
            } else {
                nextAction()
            }
        }
    }

    private fun animatePath(path: List<Pos>, onArrive: () -> Unit) {
        var i = 0
        fun step() {
            if (!running) return
            if (i >= path.size) {
                onArrive()
                return
            }
            bot = path[i]
            i++
            steps++
            if (steps > level.budget) {
                fail("Waktu habis! Langkah melewati batas ${level.budget}.")
                return
            }
            render()
            handler.postDelayed({ step() }, 280)
        }
        step()
    }

    private fun endRun() {
        running = false
        runButton.isEnabled = true
        if (delivered >= totalResources) {
            log.add("Semua resource diantar!")
            won = true
            onWin()
        } else {
            log.add("Program selesai. Resource belum semua diantar.")
        }
    }

    private fun fail(message: String) {
        running = false
        runButton.isEnabled = true
        log.add(message)
    }

    // Shortest path avoiding walls, or null when unreachable.
    private fun bfs(from: Pos, to: Pos): List<Pos>? {
        val queue = ArrayDeque<Pos>()
        val prev = HashMap<Pos, Pos>()
        queue.add(from)
        prev[from] = from
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == to) {
                val path = ArrayList<Pos>()
                var p = cur
                while (p != from) {
                    path.add(0, p)
                    p = prev[p]!!
                }
                return path
            }
            for ((dx, dy) in dirs) {
                val np = Pos(cur.first + dx, cur.second + dy)
                if (np.second in level.grid.indices &&
                    np.first in level.grid[np.second].indices &&
                    level.grid[np.second][np.first] != '#' &&
                    np !in prev
                ) {
                    prev[np] = cur
                    queue.add(np)
                }
            }
        }
        return null
    }

    override fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}

// ---------------------------------------------------------------------------
// 5. Opus Magnum: "Mesin Alkimia". Tap operations in order to transform
// the starting material into the target product. A wrong operation wastes
// one of the limited steps. Win: the target material is reached.
// ---------------------------------------------------------------------------
class OpusEngine(
    private val activity: Activity,
    private val root: LinearLayout,
    private val level: AlchemyLevel,
    private val onWin: () -> Unit
) : MiniGameEngine {

    private lateinit var currentText: TextView
    private lateinit var stepsText: TextView
    private lateinit var log: MiniLog

    private var current = level.start
    private var stepsUsed = 0
    private var won = false

    init {
        build()
        reset()
    }

    private fun build() {
        root.addView(Ui.label(activity, "MESIN ALKIMIA — ${level.name}"))
        root.addView(Ui.small(activity, "Ubah bahan awal menjadi produk target. Setiap ketukan memakai 1 langkah.").apply {
            layoutParams = spacedParams(activity, 4)
        })
        currentText = Ui.mono(activity, "", 18f, Ui.ACCENT).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(currentText)
        root.addView(Ui.body(activity, "Target: ${level.target}").apply {
            layoutParams = spacedParams(activity, 4)
        })
        stepsText = Ui.small(activity, "").apply {
            layoutParams = spacedParams(activity, 4)
        }
        root.addView(stepsText)
        val logText = logView(activity).apply {
            layoutParams = spacedParams(activity, 12)
        }
        root.addView(logText)
        log = MiniLog(logText, 8)
        for (op in level.ops) {
            root.addView(Ui.secondaryButton(activity, op).apply {
                layoutParams = spacedParams(activity, 8)
                setOnClickListener { applyOp(op) }
            })
        }
        root.addView(Ui.secondaryButton(activity, "Reset").apply {
            layoutParams = spacedParams(activity, 12)
            setOnClickListener { reset() }
        })
    }

    private fun reset() {
        current = level.start
        stepsUsed = 0
        won = false
        log.clear()
        log.add("Bahan awal: ${level.start}.")
        render()
    }

    private fun render() {
        currentText.text = current
        stepsText.text = "Langkah dipakai: $stepsUsed/${level.maxSteps}"
    }

    private fun applyOp(op: String) {
        if (won) return
        if (stepsUsed >= level.maxSteps) {
            log.add("Langkah habis. Reset untuk coba lagi.")
            return
        }
        stepsUsed++
        val rule = level.rules.firstOrNull { it.op == op && it.from == current }
        if (rule != null) {
            current = rule.to
            log.add("$op: ${rule.from} → ${rule.to}.")
            render()
            if (current == level.target) {
                won = true
                log.add("Produk jadi: ${level.target}!")
                onWin()
            }
        } else {
            log.add("$op: tidak ada efek pada $current.")
            render()
        }
    }

    override fun stop() {
        // No animation to cancel.
    }
}
