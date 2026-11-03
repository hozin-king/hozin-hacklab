package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

/**
 * Home screen. Exactly one learning path, exactly one primary action.
 * No placeholder paths, no "coming soon" cards: anything shown here works.
 */
class MainActivity : Activity() {

    private lateinit var store: ProgressStore
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var actionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProgressStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(Ui.title(this, "HackLab"))
        root.addView(Ui.small(this, "Belajar hacking dan coding dari nol. Sepenuhnya offline.").apply {
            setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 20))
        })

        // The one and only path card.
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.CARD)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        card.addView(Ui.heading(this, "Terminal & Linux Dasar"))
        card.addView(Ui.body(this, "Belajar terminal dari nol lewat simulator: 6 langkah dari pwd sampai menemukan flag.").apply {
            setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12))
        })
        progressText = Ui.body(this, "")
        card.addView(progressText)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = Steps.COUNT
            progressTintList = ColorStateList.valueOf(Ui.ACCENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 8)
            )
        }
        card.addView(progressBar)
        actionButton = Ui.primaryButton(this, "").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
            setOnClickListener { openActiveStep() }
        }
        card.addView(actionButton)
        root.addView(card)

        root.addView(Ui.small(this, "Tidak ada internet, tidak ada akun, tidak ada data yang dikirim. Semua progres tersimpan di HP ini.").apply {
            setPadding(0, Ui.dp(this, 20), 0, 0)
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val completed = store.completedCount()
        val active = store.activeStep()
        progressText.text = "$completed dari ${Steps.COUNT} langkah selesai"
        progressBar.progress = completed
        actionButton.text = when {
            completed == 0 -> "Mulai belajar"
            completed < Steps.COUNT -> "Lanjutkan"
            else -> "Lihat langkah terakhir"
        }
    }

    private fun openActiveStep() {
        val active = store.activeStep()
        startActivity(Intent(this, StepActivity::class.java).apply {
            putExtra(StepActivity.EXTRA_STEP_INDEX, active)
        })
    }
}
