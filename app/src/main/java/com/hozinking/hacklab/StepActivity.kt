package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * One lesson screen, laid out like a mission briefing:
 *
 * - A strip: MISI 03/06, the step state, and the focus command in mono.
 * - BRIEFING: the step's short lessons, unchanged.
 * - TARGET: the mission, unchanged.
 * - TONTON VIDEO: the step's companion video. Watching it never affects
 *   progress, so it is shown even for locked steps as a preview.
 * - Exactly one primary action: "Buka simulator". A locked step shows
 *   only an explanation — never a dead button.
 */
class StepActivity : Activity() {

    companion object {
        const val EXTRA_STEP_INDEX = "step_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(EXTRA_STEP_INDEX, 0)
        val step = Steps.get(index)
        if (step == null) {
            finish()
            return
        }
        val store = ProgressStore(this)
        val locked = index > store.activeStep() && !store.isCompleted(index)
        val done = store.isCompleted(index)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        // Mission strip: number, state, focus command.
        val stateText = when {
            done -> "SELESAI"
            locked -> "TERKUNCI"
            else -> "AKTIF"
        }
        val stateColor = when {
            done -> Ui.ACCENT
            locked -> Ui.SECONDARY
            else -> Ui.TEXT
        }
        root.addView(Ui.label(this, "MISI ${index + 1}/06 — $stateText").apply {
            setTextColor(stateColor)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.mono(this, step.focus, 16f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.title(this, step.title).apply {
            setPadding(0, 0, 0, Ui.dp(this, 16))
        })

        // Briefing: the lessons, unchanged.
        root.addView(Ui.label(this, "BRIEFING").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        for (paragraph in step.lessons) {
            root.addView(Ui.body(this, paragraph).apply {
                setPadding(0, 0, 0, Ui.dp(this, 12))
            })
        }

        if (locked) {
            root.addView(Ui.body(this, "Langkah ini masih terkunci.").apply {
                setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4))
            })
            root.addView(Ui.small(this, "Selesaikan langkah sebelumnya dulu, baru langkah ini terbuka."))
        } else {
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
            missionCard.addView(Ui.body(this, step.mission))
            root.addView(missionCard)

            val openButton = Ui.primaryButton(this, "Buka simulator").apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = Ui.dp(this, 16)
                layoutParams = params
                setOnClickListener {
                    startActivity(Intent(this@StepActivity, TerminalActivity::class.java).apply {
                        putExtra(TerminalActivity.EXTRA_STEP_INDEX, index)
                    })
                }
            }
            root.addView(openButton)
        }

        // Companion video. Progress is untouched by watching.
        val videoHead = Ui.label(this, "TONTON VIDEO").apply {
            setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 8))
        }
        root.addView(videoHead)
        root.addView(Ui.body(this, step.video.title).apply {
            setPadding(0, 0, 0, Ui.dp(this, 2))
        })
        if (step.video.channel.isNotEmpty()) {
            root.addView(Ui.small(this, step.video.channel).apply {
                setPadding(0, 0, 0, Ui.dp(this, 2))
            })
        }
        root.addView(Ui.small(this, step.video.language).apply {
            setPadding(0, 0, 0, Ui.dp(this, 2))
        })
        if (step.video.fallback) {
            root.addView(Ui.small(this, "Video pengantar umum — belum ada video khusus perintah ini.").apply {
                setPadding(0, 0, 0, Ui.dp(this, 8))
            })
        }
        val playButton = Ui.secondaryButton(this, "Putar video").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 4)
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(this@StepActivity, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, step.video.videoId)
                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, step.video.title)
                })
            }
        }
        root.addView(playButton)
    }
}
