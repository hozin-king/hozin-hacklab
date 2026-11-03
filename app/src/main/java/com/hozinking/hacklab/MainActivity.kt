package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Home screen: a lab mission board, not a generic card.
 *
 * A terminal prompt header, one board listing all six nodes with their
 * focus command and state (done / active / locked), and exactly one
 * primary action: continue to the active node. Tapping a node opens that
 * step directly, so finished steps can be revisited for their video.
 * Locked nodes open their step screen, which explains the lock — never
 * a dead tap.
 */
class MainActivity : Activity() {

    private lateinit var store: ProgressStore
    private lateinit var boardNodes: LinearLayout
    private lateinit var progressLine: android.widget.TextView
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

        // Terminal prompt header: this is a lab, not a landing page.
        root.addView(Ui.prompt(this, "pemula@hacklab:~$ ./hacklab").apply {
            setPadding(0, 0, 0, Ui.dp(this, 2))
        })
        root.addView(Ui.title(this, "HackLab"))
        root.addView(Ui.small(this, "Lab terminal di HP. Materi dan simulator sepenuhnya offline — hanya streaming video yang memakai internet.").apply {
            setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 20))
        })

        // The mission board.
        val board = Ui.panel(this).apply {
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        board.addView(Ui.label(this, "PAPAN MISI — TERMINAL & LINUX DASAR").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        progressLine = Ui.mono(this, "", 13f, Ui.ACCENT).apply {
            setPadding(0, 0, 0, Ui.dp(this, 12))
        }
        board.addView(progressLine)
        boardNodes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        board.addView(boardNodes)
        root.addView(board)

        // The one primary action.
        actionButton = Ui.primaryButton(this, "").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 16)
            layoutParams = params
            setOnClickListener { openActiveStep() }
        }
        root.addView(actionButton)

        root.addView(Ui.small(this, "Semua progres tersimpan di HP ini. Tidak ada akun, tidak ada data yang dikirim — kecuali streaming video dari YouTube saat kamu memutarnya.").apply {
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
        progressLine.text = "$completed/6 NODE SELESAI"

        boardNodes.removeAllViews()
        for (i in 0 until Steps.COUNT) {
            val step = Steps.get(i) ?: continue
            val status = when {
                store.isCompleted(i) -> Ui.NodeStatus.DONE
                i == active -> Ui.NodeStatus.ACTIVE
                else -> Ui.NodeStatus.LOCKED
            }
            val marker = when (status) {
                Ui.NodeStatus.DONE -> "[OK]"
                Ui.NodeStatus.ACTIVE -> "[>>]"
                Ui.NodeStatus.LOCKED -> "[--]"
            }
            val note = when (status) {
                Ui.NodeStatus.DONE -> "Selesai — ketuk untuk mengulang atau menonton video."
                Ui.NodeStatus.ACTIVE -> "Node aktif — lanjutkan di sini."
                Ui.NodeStatus.LOCKED -> "Terkunci — selesaikan node sebelumnya dulu."
            }
            val node = Ui.missionNode(this, status, marker, step.focus, step.title, note).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                if (i > 0) params.topMargin = Ui.dp(this, 8)
                layoutParams = params
                setOnClickListener { openStep(i) }
            }
            boardNodes.addView(node)
        }

        actionButton.text = when {
            completed == 0 -> "Mulai belajar"
            completed < Steps.COUNT -> "Lanjutkan"
            else -> "Lihat langkah terakhir"
        }
    }

    private fun openActiveStep() {
        openStep(store.activeStep())
    }

    private fun openStep(index: Int) {
        startActivity(Intent(this, StepActivity::class.java).apply {
            putExtra(StepActivity.EXTRA_STEP_INDEX, index)
        })
    }
}
