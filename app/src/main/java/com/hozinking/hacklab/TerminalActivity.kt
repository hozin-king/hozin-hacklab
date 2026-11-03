package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
 * The terminal simulator screen. One mission banner, one scrollable
 * monospace log, one input row, one hint button. The prompt is green
 * (the accent color); everything else in the log is plain white.
 *
 * A mission is only marked finished when the step predicate is really
 * true — the completion box appears only on that transition, never early.
 */
class TerminalActivity : Activity() {

    companion object {
        const val EXTRA_STEP_INDEX = "step_index"
    }

    private lateinit var store: ProgressStore
    private lateinit var step: StepDef
    private val engine = TerminalEngine()

    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private var logSpans = SpannableStringBuilder()
    private lateinit var input: EditText
    private lateinit var completionBox: LinearLayout
    private lateinit var completionText: TextView
    private lateinit var completionButton: Button
    private var completionShown = false

    // Blinking block cursor after the pending prompt. The blink only ever
    // touches the very last character of the log, and it is always stripped
    // before new text is appended, so it can never corrupt a command line.
    private val cursorHandler = Handler(Looper.getMainLooper())
    private var cursorVisible = false
    private val cursorRunnable = object : Runnable {
        override fun run() {
            if (cursorVisible) {
                stripCursor()
            } else {
                logSpans.append("█")
                cursorVisible = true
                logView.text = logSpans
            }
            cursorHandler.postDelayed(this, 530)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(EXTRA_STEP_INDEX, 0)
        val found = Steps.get(index)
        if (found == null) {
            finish()
            return
        }
        step = found
        store = ProgressStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        setContentView(root)

        // Mission strip: one line, always visible. The terminal below it
        // is the hero of this screen and takes all remaining space.
        val missionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        missionStrip.addView(Ui.mono(this, "MISI ${index + 1}/06", 13f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.rightMargin = Ui.dp(this, 8)
            layoutParams = params
        })
        missionStrip.addView(Ui.body(this, step.mission))
        root.addView(missionStrip)

        // Scrollable log: the terminal itself.
        logView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 15f
            setTextColor(Ui.TEXT)
            setBackgroundColor(Ui.CARD)
            setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12))
        }
        logScroll = ScrollView(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
            addView(logView)
        }
        root.addView(logScroll)

        // Completion box, hidden until the mission predicate turns true.
        completionBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
        }
        completionBox.addView(TextView(this).apply {
            text = "Misi selesai!"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.ACCENT)
        })
        completionText = Ui.body(this, "")
        completionBox.addView(completionText)
        completionButton = Ui.primaryButton(this, "").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { onCompletionPressed() }
        }
        completionBox.addView(completionButton)
        root.addView(completionBox)

        // Input row: text field + send button.
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
        }
        input = EditText(this).apply {
            hint = "Ketik perintah…"
            typeface = Typeface.MONOSPACE
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
                } else {
                    false
                }
            }
        }
        inputRow.addView(input)
        val sendButton = Ui.primaryButton(this, "Kirim").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { sendCommand() }
        }
        inputRow.addView(sendButton)
        root.addView(inputRow)

        val hintButton = Ui.secondaryButton(this, "Petunjuk").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { showHint() }
        }
        root.addView(hintButton)

        appendLine("HackLab Terminal — simulator offline.")
        appendLine("Ketik help untuk melihat perintah.")
        printPrompt()
        scrollToBottom()
        startCursorBlink()
    }

    override fun onResume() {
        super.onResume()
        startCursorBlink()
    }

    override fun onPause() {
        stopCursorBlink()
        super.onPause()
    }

    private fun startCursorBlink() {
        stripCursor()
        cursorHandler.removeCallbacks(cursorRunnable)
        cursorHandler.postDelayed(cursorRunnable, 530)
    }

    private fun stopCursorBlink() {
        cursorHandler.removeCallbacks(cursorRunnable)
        stripCursor()
    }

    private fun stripCursor() {
        if (!cursorVisible) return
        if (logSpans.length > 0 && logSpans[logSpans.length - 1] == '█') {
            logSpans.delete(logSpans.length - 1, logSpans.length)
            logView.text = logSpans
        }
        cursorVisible = false
    }

    private fun sendCommand() {
        val raw = input.text.toString()
        if (raw.trim().isEmpty()) return
        input.setText("")
        stripCursor()
        // The prompt line is already printed; the command completes it.
        logSpans.append(raw.trim()).append("\n")
        when (val result = engine.execute(raw)) {
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
        checkMission()
        scrollToBottom()
    }

    private fun showHint() {
        // Close the pending prompt line, print the hint, open a new prompt.
        stripCursor()
        logSpans.append("\n")
        logSpans.append("Petunjuk: ${step.hint}\n")
        logView.text = logSpans
        printPrompt()
        scrollToBottom()
    }

    private fun checkMission() {
        if (completionShown) return
        if (store.isCompleted(step.index)) return
        if (!step.check(engine)) return
        completionShown = true
        if (step.index == Steps.COUNT - 1) {
            completionText.text = "Jalur selesai! Kamu sudah menamatkan Terminal & " +
                "Linux Dasar. Ingat: skill ini hanya dipakai di lab sendiri " +
                "atau target yang memberi izin."
            completionButton.text = "Kembali ke beranda"
        } else {
            completionText.text = "Langkah ${step.index + 1} selesai. " +
                "Kamu bisa lanjut ke langkah berikutnya."
            completionButton.text = "Lanjut ke langkah berikutnya"
        }
        completionBox.visibility = View.VISIBLE
    }

    private fun onCompletionPressed() {
        store.markCompleted(step.index)
        if (step.index < Steps.COUNT - 1) {
            startActivity(Intent(this, StepActivity::class.java).apply {
                putExtra(StepActivity.EXTRA_STEP_INDEX, step.index + 1)
            })
        }
        finish()
    }

    private fun appendLine(text: String) {
        logSpans.append(text).append("\n")
        logView.text = logSpans
    }

    /** Appends a fresh green prompt for the current directory. */
    private fun printPrompt() {
        val start = logSpans.length
        logSpans.append("pemula@hacklab:${engine.promptDir()}$ ")
        logSpans.setSpan(
            ForegroundColorSpan(Ui.ACCENT),
            start,
            logSpans.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        logView.text = logSpans
    }

    private fun scrollToBottom() {
        logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
