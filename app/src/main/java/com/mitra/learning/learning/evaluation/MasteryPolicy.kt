package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult
import kotlin.math.max
import kotlin.math.min

object MasteryPolicy {
    fun update(current: Float, result: AttemptResult, hintsUsed: Int): Float {
        val delta = when (result) {
            AttemptResult.CORRECT -> when {
                hintsUsed <= 0 -> 0.08f
                hintsUsed == 1 -> 0.04f
                else -> 0.02f
            }
            AttemptResult.PARTIAL -> 0.01f
            AttemptResult.INCORRECT -> -0.03f
            AttemptResult.SKIPPED -> -0.01f
            AttemptResult.UNKNOWN -> 0f
        }
        return min(1f, max(0f, current + delta))
    }
}
