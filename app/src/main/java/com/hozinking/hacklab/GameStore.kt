package com.hozinking.hacklab

import android.content.Context

// Mini-game level progress, stored locally in SharedPreferences.
// Keys are new ("level_done_<game>_<level>") and never touch the
// Phase 1 keys (step_done_*, active_step), so existing progress stays.
class GameStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("hacklab_games", Context.MODE_PRIVATE)

    fun isLevelDone(gameId: String, levelId: String): Boolean {
        return prefs.getBoolean(key(gameId, levelId), false)
    }

    fun markLevelDone(gameId: String, levelId: String) {
        prefs.edit().putBoolean(key(gameId, levelId), true).apply()
    }

    fun doneCount(gameId: String, levelIds: List<String>): Int {
        var count = 0
        for (id in levelIds) {
            if (isLevelDone(gameId, id)) count++
        }
        return count
    }

    private fun key(gameId: String, levelId: String): String {
        return "level_done_${gameId}_$levelId"
    }
}
