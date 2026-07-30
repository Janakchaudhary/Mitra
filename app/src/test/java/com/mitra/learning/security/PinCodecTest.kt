package com.mitra.learning.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCodecTest {
    @Test
    fun encodeAndVerify() {
        val encoded = PinCodec.encode("1234".toCharArray())
        assertTrue(PinCodec.verify("1234".toCharArray(), encoded))
        assertFalse(PinCodec.verify("9999".toCharArray(), encoded))
    }
}
