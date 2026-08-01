package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedReviewPolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `correct answers expand review interval`() {
        val first = SpacedReviewPolicy.update(AttemptResult.CORRECT, 0, 0, now)
        val second = SpacedReviewPolicy.update(AttemptResult.CORRECT, first.intervalDays, first.consecutiveSuccesses, now)
        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertTrue(second.nextReviewAt!! > first.nextReviewAt!!)
    }

    @Test
    fun `incorrect answer resets review`() {
        val update = SpacedReviewPolicy.update(AttemptResult.INCORRECT, 14, 4, now)
        assertEquals(1, update.intervalDays)
        assertEquals(0, update.consecutiveSuccesses)
    }
}
