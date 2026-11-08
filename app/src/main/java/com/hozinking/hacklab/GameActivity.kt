package com.hozinking.hacklab

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * One game screen, laid out like a mission briefing (same as a lesson
 * step, but without a simulator):
 *
 * - A strip: GAME 02/05 and the game name in mono.
 * - TENTANG GAME INI: short paragraphs about the game.
 * - YANG DIPELAJARI: the skills this game trains.
 * - Info lines: language and difficulty.
 * - Exactly one primary action: "Buka situs resmi" — opens the game's
 *   official site in the browser. Games need internet; Phase 1 stays
 *   fully offline. Nothing here is a WebView.
 */
class GameActivity : Activity() {

    companion object {
        const val EXTRA_GAME_INDEX = "game_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(EXTRA_GAME_INDEX, 0)
        val game = Games.get(index)
        if (game == null) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(Ui.label(this, "GAME ${index + 1}/${Games.COUNT} — FASE 2").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.mono(this, game.name, 16f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.title(this, game.tagline).apply {
            setPadding(0, 0, 0, Ui.dp(this, 16))
        })

        // About the game.
        root.addView(Ui.label(this, "TENTANG GAME INI").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        for (paragraph in game.about) {
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
        skillsCard.addView(Ui.body(this, game.skills))
        root.addView(skillsCard)

        root.addView(Ui.body(this, "Bahasa: ${game.language}").apply {
            setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 2))
        })
        root.addView(Ui.body(this, "Tingkat: ${game.level}").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.small(this, "Situs game terbuka di browser dan butuh internet.").apply {
            setPadding(0, Ui.dp(this, 4), 0, 0)
        })

        val openButton = Ui.primaryButton(this, "Buka situs resmi").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 16)
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(game.url)))
            }
        }
        root.addView(openButton)
    }
}
