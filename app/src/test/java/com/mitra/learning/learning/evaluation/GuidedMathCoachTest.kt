package com.mitra.learning.learning.evaluation

import com.mitra.learning.learning.model.ArithmeticWork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedMathCoachTest {
    @Test
    fun `addition with carry checks each column`() {
        val work = ArithmeticWork(27, 18, "+", regrouping = true)
        assertEquals(GuidedMathExpected(5, 1, 4, "કેરી"), GuidedMathCoach.expected(work))
        assertFalse(GuidedMathCoach.check(work, 5, 0, 4).correct)
        assertTrue(GuidedMathCoach.check(work, 5, 1, 4).correct)
    }

    @Test
    fun `subtraction with borrow checks reduced tens`() {
        val work = ArithmeticWork(42, 17, "−", regrouping = true)
        assertEquals(GuidedMathExpected(5, 1, 2, "ઉધાર"), GuidedMathCoach.expected(work))
        assertEquals(GuidedMathStep.TENS, GuidedMathCoach.check(work, 5, 1, 3).step)
        assertTrue(GuidedMathCoach.check(work, 5, 1, 2).correct)
    }
}
