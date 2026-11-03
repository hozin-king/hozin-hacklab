package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * One lesson screen: the step's short lessons, its mission, and the one
 * primary action ("Buka simulator"). A locked step shows only an
 * explanation — never a dead button.
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(Ui.small(this, "Langkah ${index + 1} dari ${Steps.COUNT}").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.title(this, step.title).apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        if (store.isCompleted(index)) {
            root.addView(TextView(this).apply {
                text = "Selesai"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Ui.ACCENT)
                setPadding(0, 0, 0, Ui.dp(this, 12))
            })
        }

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
            val missionCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Ui.CARD)
                setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
            }
            missionCard.addView(Ui.small(this, "MISI").apply {
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
    }
}
