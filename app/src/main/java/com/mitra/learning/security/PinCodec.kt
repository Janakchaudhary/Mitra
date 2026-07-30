package com.mitra.learning.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class EncodedPin(val saltBase64: String, val hashBase64: String)

object PinCodec {
    private const val Iterations = 120_000
    private const val KeyLengthBits = 256

    fun encode(pin: CharArray, random: SecureRandom = SecureRandom()): EncodedPin {
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = derive(pin, salt)
        return EncodedPin(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            hashBase64 = Base64.getEncoder().encodeToString(hash),
        )
    }

    fun verify(pin: CharArray, encoded: EncodedPin): Boolean {
        val salt = Base64.getDecoder().decode(encoded.saltBase64)
        val expected = Base64.getDecoder().decode(encoded.hashBase64)
        val actual = derive(pin, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, Iterations, KeyLengthBits)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
            pin.fill('\u0000')
        }
    }
}
