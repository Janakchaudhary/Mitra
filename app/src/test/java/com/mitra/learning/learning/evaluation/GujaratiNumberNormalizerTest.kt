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
}
