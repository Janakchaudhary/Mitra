package com.mitra.learning.voice

import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceStyleTest {
    @Test
    fun allVoiceStylesUseSafeTtsRanges() {
        VoiceStyle.entries.forEach { style ->
            assertTrue(style.pitch in 0.5f..2.0f)
            assertTrue(style.rate in 0.5f..2.0f)
        }
    }
}
