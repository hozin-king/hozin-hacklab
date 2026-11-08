package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

// One lesson screen, laid out like a mission briefing (same as a lesson
// step, but without a simulator):
//
// - A strip: FASE 2 and the lesson name in mono.
// - TENTANG GAME INI: short paragraphs about the game.
// - YANG DIPELAJARI: the skills this lesson trains.
// - Info lines: language and difficulty, plus mini-game progress.
// - Primary action: "MAIN" for a mini-game (opens the playable game),
//   "Buka situs" for an online link entry. Mini-games also keep a
//   secondary "Buka situs resmi" that opens the browser. Nothing here
//   is a WebView.
class GameActivity : Activity() {

    companion object {
        const val EXTRA_LESSON_ID = "lesson_id"
    }

    private lateinit var store: GameStore
    private lateinit var entry: LessonEntry
    private lateinit var progressLine: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: ""
        LessonCatalog.findAsync(this, lessonId) { found ->
            if (isFinishing || isDestroyed) return@findAsync
            if (found == null) {
                finish()
                return@findAsync
            }
            entry = found
            store = GameStore(this)
            buildUi()
        }
    }

    // Builds the whole screen once the lesson entry is loaded off the UI
    // thread (findAsync). refreshProgress runs at the end so the progress
    // line is set even when onResume already fired before the lookup.
    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(Ui.label(this, "FASE 2").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.mono(this, entry.name, 16f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.title(this, entry.tagline).apply {
            setPadding(0, 0, 0, Ui.dp(this, 16))
        })

        // About the game.
        root.addView(Ui.label(this, "TENTANG GAME INI").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        for (paragraph in entry.about) {
            root.addView(Ui.body(this, paragraph).apply {
                setPadding(0, 0, 0, Ui.dp(this, 12))
            })
        }

        val skillsCard = Ui.panel(this).apply {
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 4)
            layoutParams = params
        }
        skillsCard.addView(Ui.label(this, "YANG DIPELAJARI").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        skillsCard.addView(Ui.body(this, entry.skills))
        root.addView(skillsCard)

        root.addView(Ui.body(this, "Bahasa: ${entry.language}").apply {
            setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 2))
        })
        root.addView(Ui.body(this, "Tingkat: ${entry.level}").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        progressLine = Ui.mono(this, "", 13f, Ui.ACCENT).apply {
            setPadding(0, Ui.dp(this, 8), 0, 0)
        }
        root.addView(progressLine)
        root.addView(Ui.small(this, onlineNote()).apply {
            setPadding(0, Ui.dp(this, 4), 0, 0)
        })

        if (entry.kind == "minigame") {
            val playButton = Ui.primaryButton(this, "MAIN").apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = Ui.dp(this, 16)
                layoutParams = params
                setOnClickListener {
                    startActivity(Intent(this@GameActivity, MiniGameActivity::class.java).apply {
                        putExtra(MiniGameActivity.EXTRA_LESSON_ID, entry.id)
                    })
                }
            }
            root.addView(playButton)
            val siteButton = Ui.secondaryButton(this, "Buka situs resmi").apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = Ui.dp(this, 8)
                layoutParams = params
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)))
                }
            }
            root.addView(siteButton)
        } else {
            val siteButton = Ui.primaryButton(this, "Buka situs").apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = Ui.dp(this, 16)
                layoutParams = params
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)))
                }
            }
            root.addView(siteButton)
        }
        refreshProgress()
    }

    override fun onResume() {
        super.onResume()
        refreshProgress()
    }

    private fun refreshProgress() {
        // May run before buildUi finishes (onResume fires first): skip
        // until the entry and its views exist.
        if (!::entry.isInitialized || !::progressLine.isInitialized) return
        if (entry.kind != "minigame") {
            progressLine.text = ""
            return
        }
        MiniGameLevels.levelRefsAsync(this, entry.id) { refs ->
            if (isFinishing || isDestroyed) return@levelRefsAsync
            progressLine.text =
                "${store.doneCount(entry.id, refs.map { it.id })}/${refs.size} LEVEL SELESAI"
        }
    }

    private fun onlineNote(): String {
        return if (entry.kind == "minigame") {
            "Bisa dimainkan offline di HP ini."
        } else {
            "Pelajaran online: dibuka di browser dan butuh internet."
        }
    }
}
