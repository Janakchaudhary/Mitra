package com.mitra.learning.study.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenAnswerNormalizerTest {
    @Test
    fun `normalizes Gujarati spoken number`() {
        assertEquals(38, SpokenAnswerNormalizer.numeric("અડત્રીસ છે"))
    }

    @Test
    fun `normalizes compact and separated spelling`() {
        assertEquals("cat", SpokenAnswerNormalizer.spelling("CAT"))
        assertEquals("cat", SpokenAnswerNormalizer.spelling("C A T"))
        assertEquals("cat", SpokenAnswerNormalizer.spelling("see ay tee"))
        assertEquals("cat", SpokenAnswerNormalizer.spelling("સી એ ટી"))
    }
}
