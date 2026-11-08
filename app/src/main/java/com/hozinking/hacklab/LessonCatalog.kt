package com.hozinking.hacklab

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// One lesson shown in the Phase 2 list. Built-in mini-games and online
// entries share this shape. kind is "minigame" (playable in the app) or
// "link" (opens its url in the browser).
data class LessonEntry(
    val id: String,
    val name: String,
    val tagline: String,
    val about: List<String>,
    val skills: String,
    val language: String,
    val level: String,
    val url: String,
    val icon: String,
    val color: String,
    val kind: String,
    val sortOrder: Int
)

// Reads a JSON string array into a plain list. Bad input yields empty.
internal fun jsonStrings(arr: JSONArray?): List<String> {
    val out = ArrayList<String>()
    if (arr == null) return out
    for (i in 0 until arr.length()) {
        out.add(arr.optString(i, ""))
    }
    return out
}

// The online lesson catalog: fetched from the CMS, cached locally, merged
// with the built-in games. The catalog lives in the user's D1 database
// "stopwatch-db" (shared with the stopwatch, so no new database is needed);
// the endpoint serves it as JSON. The endpoint may not exist yet — every
// failure path falls back silently to cache + built-ins, never an error.
object LessonCatalog {

    const val ENDPOINT = "https://hozinking.my.id/api/hacklab/lessons"

    private const val PREFS = "hacklab_catalog"
    private const val KEY_JSON = "catalog_json"
    private const val KEY_LAST_SYNC = "catalog_last_sync"
    // Do not re-fetch more often than this: a fresh 8-second network
    // attempt on every screen open is what made the home screen stutter.
    private const val SYNC_INTERVAL_MS = 15L * 60L * 1000L

    private val mainHandler = Handler(Looper.getMainLooper())

    // Same as loadMerged, but the JSON parse + merge runs on a
    // background thread and the result is posted to the main thread,
    // so the UI thread never blocks on catalog parsing.
    fun loadMergedAsync(context: Context, onDone: (List<LessonEntry>) -> Unit) {
        Thread {
            val merged = loadMerged(context)
            mainHandler.post { onDone(merged) }
        }.start()
    }

    // Same as find, but the lookup runs on a background thread.
    fun findAsync(context: Context, id: String, onDone: (LessonEntry?) -> Unit) {
        Thread {
            val found = loadMerged(context).firstOrNull { it.id == id }
            mainHandler.post { onDone(found) }
        }.start()
    }

    fun toEntry(game: GameDef): LessonEntry {
        return LessonEntry(
            id = game.id,
            name = game.name,
            tagline = game.tagline,
            about = game.about,
            skills = game.skills,
            language = game.language,
            level = game.level,
            url = game.url,
            icon = game.icon,
            color = game.color,
            kind = "minigame",
            sortOrder = 0
        )
    }

    // Built-ins first, then new online "link" entries in catalog order.
    // An online entry whose id matches a built-in and kind=minigame only
    // adds levels to that game (handled in MiniGameLevels); the built-in
    // entry itself stays. An online minigame with an unknown id is
    // ignored: there is no local engine for it.
    fun loadMerged(context: Context): List<LessonEntry> {
        val builtin = Games.ALL.map { toEntry(it) }
        val builtinIds = builtin.map { it.id }.toSet()
        val out = ArrayList(builtin)
        for (e in parseEntries(cachedJson(context))) {
            if (e.id in builtinIds) continue
            if (e.kind == "link") out.add(e)
        }
        return out
    }

    fun find(context: Context, id: String): LessonEntry? {
        return loadMerged(context).firstOrNull { it.id == id }
    }

    // Raw "levels" array (as JSON text) for one game id from the cached
    // catalog, or null when the catalog has none for that game.
    fun onlineLevelsJson(context: Context, gameId: String): String? {
        val raw = cachedJson(context) ?: return null
        try {
            val arr = JSONObject(raw).optJSONArray("lessons") ?: return null
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("id", "") == gameId && o.optString("kind", "") == "minigame") {
                    val levels = o.optJSONArray("levels") ?: return null
                    return levels.toString()
                }
            }
        } catch (e: Exception) {
            // Corrupt cache: behave as if there are no online levels.
        }
        return null
    }

    // Fetches the catalog on a background thread, caches the raw JSON,
    // then calls onDone on the main thread with the merged list. Any
    // failure (no internet, bad response, bad JSON) is silent: onDone
    // still runs with cache + built-ins. The merge itself runs on the
    // background thread too, never on the UI thread.
    // Throttle: at most one real fetch per 15 minutes. The timestamp is
    // recorded when a fetch starts, so a dead network does not trigger
    // a fresh 8-second attempt on every screen open.
    fun sync(context: Context, onDone: (List<LessonEntry>) -> Unit) {
        Thread {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
            if (now - lastSync >= SYNC_INTERVAL_MS) {
                prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
                try {
                    val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        if (JSONObject(body).has("lessons")) {
                            prefs.edit().putString(KEY_JSON, body).apply()
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Silent: cache + built-ins still work.
                }
            }
            val merged = loadMerged(context)
            mainHandler.post { onDone(merged) }
        }.start()
    }

    private fun cachedJson(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
    }

    private fun parseEntries(json: String?): List<LessonEntry> {
        val out = ArrayList<LessonEntry>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONObject(json).optJSONArray("lessons") ?: return out
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isEmpty()) continue
                val name = o.optString("name", id)
                out.add(
                    LessonEntry(
                        id = id,
                        name = name,
                        tagline = o.optString("tagline", ""),
                        about = jsonStrings(o.optJSONArray("about")),
                        skills = o.optString("skills", ""),
                        language = o.optString("language", ""),
                        level = o.optString("level", ""),
                        url = o.optString("url", ""),
                        icon = o.optString("icon", defaultIcon(name)),
                        color = o.optString("color", "#455A64"),
                        kind = o.optString("kind", "link"),
                        sortOrder = o.optInt("sort_order", 0)
                    )
                )
            }
        } catch (e: Exception) {
            // Bad payload: ignore it entirely.
        }
        out.sortBy { it.sortOrder }
        return out
    }

    private fun defaultIcon(name: String): String {
        val letters = name.filter { it.isLetter() }.take(2).uppercase()
        return if (letters.isEmpty()) "?" else letters
    }
}
