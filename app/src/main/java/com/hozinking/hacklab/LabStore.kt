package com.hozinking.hacklab

import android.content.Context

// Lab progress, stored locally in SharedPreferences.
// Keys are new ("lab_done_<id>") and never touch the Phase 1 keys
// (step_done_*, active_step) or the Phase 2 keys (level_done_*),
// so existing progress stays.
class LabStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("hacklab_labs", Context.MODE_PRIVATE)

    fun isLabDone(labId: String): Boolean {
        return prefs.getBoolean(key(labId), false)
    }

    fun markLabDone(labId: String) {
        prefs.edit().putBoolean(key(labId), true).apply()
    }

    fun doneCount(labIds: List<String>): Int {
        var count = 0
        for (id in labIds) {
            if (isLabDone(id)) count++
        }
        return count
    }

    private fun key(labId: String): String {
        return "lab_done_$labId"
    }
}
