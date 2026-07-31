package com.mitra.learning.learning.evaluation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GujaratiNumberNormalizerTest {
    @Test
    fun parsesGujaratiDigits() {
        assertEquals(15, GujaratiNumberNormalizer.parseInt("૧૫"))
    }

    @Test
    fun parsesEnglishDigits() {
        assertEquals(12, GujaratiNumberNormalizer.parseInt("12"))
    }

    @Test
    fun parsesCommonGujaratiWords() {
        assertEquals(5, GujaratiNumberNormalizer.parseInt("પાંચ"))
        assertEquals(20, GujaratiNumberNormalizer.parseInt("વીસ"))
    }

    @Test
    fun returnsNullForUnknownText() {
        assertNull(GujaratiNumberNormalizer.parseInt("મને ખબર નથી"))
    }
    @Test
    fun parsesStandard2GujaratiWordsThroughHundred() {
        assertEquals(35, GujaratiNumberNormalizer.parseInt("પાંત્રીસ"))
        assertEquals(57, GujaratiNumberNormalizer.parseInt("સત્તાવન"))
        assertEquals(78, GujaratiNumberNormalizer.parseInt("અઠ્યોતેર"))
        assertEquals(100, GujaratiNumberNormalizer.parseInt("સો"))
    }

    @Test
    fun parsesSimpleEnglishNumberWords() {
        assertEquals(27, GujaratiNumberNormalizer.parseInt("twenty seven"))
        assertEquals(80, GujaratiNumberNormalizer.parseInt("eighty"))
        assertEquals(100, GujaratiNumberNormalizer.parseInt("one hundred"))
    }

}
