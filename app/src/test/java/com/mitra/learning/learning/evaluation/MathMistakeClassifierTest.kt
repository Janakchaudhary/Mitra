package com.mitra.learning.learning.evaluation

import com.mitra.learning.learning.model.ArithmeticWork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathMistakeClassifierTest {
    @Test
    fun `detects forgotten carry`() {
        val mistake = MathMistakeClassifier.classify(
            ArithmeticWork(27, 18, "+", regrouping = true),
            childAnswer = 35,
            expected = 45,
        )
        assertEquals(MathMistakeCode.FORGOT_CARRY, mistake.code)
        assertTrue(mistake.hintGujarati.contains("કેરી"))
    }

    @Test
    fun `detects forgotten borrowing`() {
        val mistake = MathMistakeClassifier.classify(
            ArithmeticWork(42, 27, "−", regrouping = true),
            childAnswer = 25,
            expected = 15,
        )
        assertEquals(MathMistakeCode.FORGOT_BORROW, mistake.code)
        assertTrue(mistake.hintGujarati.contains("ઉધાર"))
    }
}
