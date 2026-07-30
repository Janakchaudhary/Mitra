package com.mitra.learning.data.files

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHasherTest {
    @Test
    fun sha256_matchesKnownDigest() {
        val digest = FileHasher.sha256(ByteArrayInputStream("mitra".toByteArray()))
        assertEquals("ef7c6cba58cf82997b990feec6b78b1cf73b4a0b3a6b1b0c46fac8a56ca70549", digest)
    }
}
