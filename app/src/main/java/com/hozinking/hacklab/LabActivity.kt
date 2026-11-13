package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * One lab briefing screen, laid out like a mission briefing (same as a
 * lesson step, but without a video):
 *
 * - A strip: FASE 3, the lab name, and the focus command in mono.
 * - TEORI: short paragraphs about the attack and why it works.
 * - TARGET: the mission.
 * - CARA BERTAHAN: the mitigation — every lab teaches the defense too.
 * - Exactly one primary action: "Buka lab". Labs are all open, like
 *   Fase 2 games — no locks. Watching the theory never affects progress.
 */
class LabActivity : Activity() {

    companion object {
        const val EXTRA_LAB_INDEX = "lab_index"
    }

    private lateinit var store: LabStore
    private lateinit var lab: LabDef
    private lateinit var progressLine: android.widget.TextView

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
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(Ui.label(this, "FASE 3 — LAB KEAMANAN").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.mono(this, lab.focus, 16f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.title(this, lab.title).apply {
            setPadding(0, 0, 0, Ui.dp(this, 16))
        })

        root.addView(Ui.label(this, "TEORI").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        for (paragraph in lab.theory) {
            root.addView(Ui.body(this, paragraph).apply {
                setPadding(0, 0, 0, Ui.dp(this, 12))
            })
        }

        val missionCard = Ui.panel(this).apply {
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 4)
            layoutParams = params
        }
        missionCard.addView(Ui.label(this, "TARGET").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        missionCard.addView(Ui.body(this, lab.mission))
        root.addView(missionCard)

        root.addView(Ui.label(this, "CARA BERTAHAN").apply {
            setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 8))
        })
        for (paragraph in lab.mitigation) {
            root.addView(Ui.body(this, paragraph).apply {
                setPadding(0, 0, 0, Ui.dp(this, 12))
            })
        }

        progressLine = Ui.mono(this, "", 13f, Ui.ACCENT).apply {
            setPadding(0, Ui.dp(this, 8), 0, 0)
        }
        root.addView(progressLine)
        root.addView(Ui.small(this, "Simulasi offline melawan target palsu di dalam aplikasi ini. Tidak ada trafik nyata, tidak ada situs nyata.").apply {
            setPadding(0, Ui.dp(this, 4), 0, 0)
        })

        val openButton = Ui.primaryButton(this, "Buka lab").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 16)
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(this@LabActivity, LabSimulatorActivity::class.java).apply {
                    putExtra(LabSimulatorActivity.EXTRA_LAB_INDEX, lab.index)
                })
            }
        }
        root.addView(openButton)
        refreshProgress()
    }

    override fun onResume() {
        super.onResume()
        refreshProgress()
    }

    private fun refreshProgress() {
        if (!::progressLine.isInitialized) return
        progressLine.text = if (store.isLabDone(lab.id)) {
            "LAB SELESAI — flag: ${lab.flag}"
        } else {
            "LAB BELUM SELESAI"
        }
    }
}
