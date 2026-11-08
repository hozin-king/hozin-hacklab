package com.hozinking.hacklab

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

// Host for one offline mini-game: a level picker on top, one level
// playable below. Levels are data (MiniGameLevels), so online levels
// merged from the catalog appear here too. Progress uses GameStore,
// separate from the Phase 1 keys.
class MiniGameActivity : Activity() {

    companion object {
        const val EXTRA_LESSON_ID = "lesson_id"
    }

    private lateinit var store: GameStore
    private lateinit var entry: LessonEntry
    private var engine: MiniGameEngine? = null
    private var currentLevelId = ""

    private lateinit var listRoot: LinearLayout
    private lateinit var playRoot: LinearLayout
    private lateinit var playContainer: LinearLayout
    private lateinit var completionBox: LinearLayout
    private lateinit var listNodes: LinearLayout
    private lateinit var progressLine: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: ""
        LessonCatalog.findAsync(this, lessonId) { found ->
            if (isFinishing || isDestroyed) return@findAsync
            if (found == null || found.kind != "minigame") {
                finish()
                return@findAsync
            }
            entry = found
            store = GameStore(this)
            buildUi()
        }
    }

    // Builds the whole screen once the lesson entry is loaded off the UI
    // thread (findAsync). refreshList runs at the end so the level list
    // is set even when onResume already fired before the lookup.
    private fun buildUi() {

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)

        content.addView(Ui.label(this, "FASE 2 — MINI-GAME").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        content.addView(Ui.mono(this, entry.name, 16f, Ui.ACCENT).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        content.addView(Ui.title(this, entry.tagline).apply {
            setPadding(0, 0, 0, Ui.dp(this, 16))
        })
        progressLine = Ui.mono(this, "", 13f, Ui.ACCENT).apply {
            setPadding(0, 0, 0, Ui.dp(this, 12))
        }
        content.addView(progressLine)

        // Level picker.
        listRoot = Ui.panel(this).apply {
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        listRoot.addView(Ui.label(this, "PILIH LEVEL").apply {
            setPadding(0, 0, 0, Ui.dp(this, 8))
        })
        listNodes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listRoot.addView(listNodes)
        content.addView(listRoot)

        // Play area, hidden until a level is picked.
        playRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        playRoot.addView(Ui.secondaryButton(this, "← Daftar level").apply {
            setOnClickListener { backToList() }
        })
        playContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
        }
        playRoot.addView(playContainer)
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
        completionBox.addView(android.widget.TextView(this).apply {
            text = "Level selesai!"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.ACCENT)
        })
        completionBox.addView(Ui.body(this, "Bagus. Lanjut ke level berikutnya atau main lagi."))
        completionBox.addView(Ui.primaryButton(this, "Kembali ke daftar level").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 8)
            layoutParams = params
            setOnClickListener { backToList() }
        })
        playRoot.addView(completionBox)
        content.addView(playRoot)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onDestroy() {
        engine?.stop()
        engine = null
        super.onDestroy()
    }

    private fun refreshList() {
        // May run before buildUi finishes (onResume fires first): skip
        // until the entry and its views exist.
        if (!::entry.isInitialized || !::listNodes.isInitialized || !::progressLine.isInitialized) return
        MiniGameLevels.levelRefsAsync(this, entry.id) { refs ->
            if (isFinishing || isDestroyed) return@levelRefsAsync
            progressLine.text = "${store.doneCount(entry.id, refs.map { it.id })}/${refs.size} LEVEL SELESAI"
            listNodes.removeAllViews()
            for ((index, ref) in refs.withIndex()) {
                val done = store.isLevelDone(entry.id, ref.id)
                val status = if (done) Ui.NodeStatus.DONE else Ui.NodeStatus.ACTIVE
                val marker = if (done) "[OK]" else "[>>]"
                val note = if (done) "Selesai — ketuk untuk main lagi." else "Ketuk untuk main."
                val node = Ui.missionNode(this, status, marker, "Level ${index + 1}", ref.name, note).apply {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (index > 0) params.topMargin = Ui.dp(this, 8)
                    layoutParams = params
                    setOnClickListener { startLevel(ref) }
                }
                listNodes.addView(node)
            }
        }
    }

    private fun startLevel(ref: LevelRef) {
        currentLevelId = ref.id
        completionBox.visibility = View.GONE
        listRoot.visibility = View.GONE
        playRoot.visibility = View.VISIBLE
        playContainer.removeAllViews()
        engine?.stop()
        engine = null
        MiniGameLevels.findLevelAsync(this, entry.id, ref.id) { level ->
            // The player may have gone back to the list, picked another
            // level, or left while the level was loading: never start a
            // hidden or stale engine.
            if (isFinishing || isDestroyed) return@findLevelAsync
            if (currentLevelId != ref.id || playRoot.visibility != View.VISIBLE) return@findLevelAsync
            if (level == null) {
                backToList()
                return@findLevelAsync
            }
            engine = when (entry.id) {
                "code-combat" -> {
                    CodeCombatEngine(this, playContainer, level as CombatLevel) { onLevelWin() }
                }
                "bitburner" -> {
                    BitburnerEngine(this, playContainer, level as HackLevel) { onLevelWin() }
                }
                "codingame" -> {
                    CodinGameEngine(this, playContainer, level as DebugLevel) { onLevelWin() }
                }
                "screeps" -> {
                    ScreepsEngine(this, playContainer, level as BotLevel) { onLevelWin() }
                }
                "opus-magnum" -> {
                    OpusEngine(this, playContainer, level as AlchemyLevel) { onLevelWin() }
                }
                else -> null
            }
            if (engine == null) {
                backToList()
            }
        }
    }

    private fun onLevelWin() {
        store.markLevelDone(entry.id, currentLevelId)
        completionBox.visibility = View.VISIBLE
    }

    private fun backToList() {
        engine?.stop()
        engine = null
        playRoot.visibility = View.GONE
        listRoot.visibility = View.VISIBLE
        refreshList()
    }
}
