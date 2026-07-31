package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult
import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryPolicyTest {
    @Test
    fun correctWithoutHintAddsEightPercent() {
        assertEquals(0.58f, MasteryPolicy.update(0.50f, AttemptResult.CORRECT, 0), 0.0001f)
    }

    @Test
    fun incorrectReducesMasteryButNeverBelowZero() {
        assertEquals(0f, MasteryPolicy.update(0.01f, AttemptResult.INCORRECT, 0), 0.0001f)
    }

    @Test
    fun masteryNeverExceedsOne() {
        assertEquals(1f, MasteryPolicy.update(0.98f, AttemptResult.CORRECT, 0), 0.0001f)
    }
}
