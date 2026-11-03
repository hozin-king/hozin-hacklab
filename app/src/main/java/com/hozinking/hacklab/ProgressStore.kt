package com.hozinking.hacklab

import android.content.Context

/**
 * Learning progress, stored locally in SharedPreferences.
 *
 * - One boolean per step: true once its mission predicate was met.
 * - One "active step": the first step that is not finished yet.
 *   When every step is finished, the active step stays on the last one
 *   so the home screen can still open it.
 *
 * Nothing here ever leaves the device: no network, no account.
 */
class ProgressStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("hacklab_progress", Context.MODE_PRIVATE)

    fun isCompleted(index: Int): Boolean {
        return prefs.getBoolean(key(index), false)
    }

    fun completedCount(): Int {
        var count = 0
        for (i in 0 until Steps.COUNT) {
            if (isCompleted(i)) count++
        }
        return count
    }

    /** The first unfinished step, or the last step when all are finished. */
    fun activeStep(): Int {
        val saved = prefs.getInt(KEY_ACTIVE, 0)
        if (saved < 0 || saved >= Steps.COUNT) return 0
        // If the saved step is already done, advance to the next unfinished one.
        for (i in saved until Steps.COUNT) {
            if (!isCompleted(i)) return i
        }
        return Steps.COUNT - 1
    }

    /**
     * Marks a step finished and moves the active step forward.
     * Calling this for an already-finished step is a harmless no-op.
     */
    fun markCompleted(index: Int) {
        if (index < 0 || index >= Steps.COUNT) return
        val next = if (index + 1 < Steps.COUNT) index + 1 else Steps.COUNT - 1
        prefs.edit()
            .putBoolean(key(index), true)
            .putInt(KEY_ACTIVE, next)
            .apply()
    }

    private fun key(index: Int): String {
        return "step_done_$index"
    }

    companion object {
        private const val KEY_ACTIVE = "active_step"
    }
}
