package com.hozinking.hacklab

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The lab simulator screen. One mission strip on top, one container the
 * lab builder fills (a fake page, a fake login form, a terminal, or the
 * DDoS console), and one completion box that appears only when the lab
 * flag is really earned — the same rule as the Fase 1 terminal: never
 * early, never for free.
 */
class LabSimulatorActivity : Activity() {

    companion object {
        const val EXTRA_LAB_INDEX = "lab_index"
    }

    private lateinit var store: LabStore
    private lateinit var lab: LabDef
    private var session: LabSession? = null
    private var flagShown = false
    private lateinit var completionBox: LinearLayout
    private lateinit var completionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(EXTRA_LAB_INDEX, 0)
        val found = Labs.get(index)
        if (found == null) {
            finish()
            return
        }
        lab = found
        store = LabStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        setContentView(root)

        // Mission strip: one line, always visible.
        val missionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        missionStrip.addView(Ui.mono(this, "LAB ${index + 1}/07", 13f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.rightMargin = Ui.dp(this, 8)
            layoutParams = params
        })
        missionStrip.addView(Ui.body(this, lab.mission))
        root.addView(missionStrip)

        // The lab itself fills all remaining space.
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
        }
        root.addView(container)

        // Completion box, hidden until the flag is really earned.
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
            text = "Lab selesai!"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.ACCENT)
        })
        completionText = Ui.body(this, "")
        completionBox.addView(completionText)
        completionBox.addView(Ui.primaryButton(this, "Kembali ke lab").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { onCompletionPressed() }
        })
        root.addView(completionBox)

        session = when (lab.id) {
            "xss" -> buildXssLab(this, container, lab) { showFlag() }
            "css" -> buildCssLab(this, container, lab) { showFlag() }
            "sqli" -> buildSqliLab(this, container, lab) { showFlag() }
            "wifi" -> buildWifiLab(this, container, lab) { showFlag() }
            "ddos" -> buildDdosLab(this, container, lab) { showFlag() }
            "server" -> buildServerLab(this, container, lab) { showFlag() }
            "crypto" -> buildCryptoLab(this, container, lab) { showFlag() }
            else -> null
        }
    }

    override fun onDestroy() {
        session?.stop()
        session = null
        super.onDestroy()
    }

    private fun showFlag() {
        if (flagShown) return
        flagShown = true
        completionText.text = "Flag direbut: ${lab.flag}. Ingat: skill ini " +
            "hanya dipakai di lab sendiri atau target yang memberi izin."
        completionBox.visibility = View.VISIBLE
    }

    private fun onCompletionPressed() {
        store.markLabDone(lab.id)
        finish()
    }
}
