package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult

/** Simple local review scheduler: 1, 3, 7, 14, then 30 days. */
object SpacedReviewPolicy {
    private val intervals = intArrayOf(1, 3, 7, 14, 30)
    private const val DAY_MS = 86_400_000L

    data class ReviewUpdate(
        val nextReviewAt: Long?,
        val intervalDays: Int,
        val consecutiveSuccesses: Int,
    )

    fun update(
        result: AttemptResult,
        currentIntervalDays: Int,
        currentSuccesses: Int,
        nowMillis: Long,
    ): ReviewUpdate = when (result) {
        AttemptResult.CORRECT -> {
            val successes = currentSuccesses + 1
            val interval = intervals[(successes - 1).coerceIn(0, intervals.lastIndex)]
            ReviewUpdate(nowMillis + interval * DAY_MS, interval, successes)
        }
        AttemptResult.PARTIAL -> ReviewUpdate(nowMillis + DAY_MS, 1, 0)
        AttemptResult.INCORRECT -> ReviewUpdate(nowMillis + DAY_MS, 1, 0)
        AttemptResult.SKIPPED -> ReviewUpdate(nowMillis + DAY_MS, currentIntervalDays.coerceAtLeast(1), 0)
        AttemptResult.UNKNOWN -> ReviewUpdate(null, currentIntervalDays, currentSuccesses)
    }
}
