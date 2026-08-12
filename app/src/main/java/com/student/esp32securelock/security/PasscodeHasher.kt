package com.student.esp32securelock.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Pure-Kotlin passcode hashing using PBKDF2-HMAC-SHA256.
 *
 * The output is a [HashedPasscode] containing:
 *  - base64(random salt)
 *  - base64(derived key)
 *  - iteration count
 *
 * Constants (iterations, key length) are encapsulated so the caller never
 * deals with raw crypto parameters.
 *
 * The passcode itself is never logged, never stored in a field on this
 * object, and is only fed to the PBKDF2 derivation routine which immediately
 * copies it into a char[] that is zeroed after use.
 */
object PasscodeHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class HashedPasscode(
        val salt: String,
        val hash: String,
        val iterations: Int
    )

    fun hash(passcode: String): HashedPasscode {
        require(passcode.isNotEmpty()) { "Passcode cannot be empty" }

        val saltBytes = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(passcode, saltBytes, ITERATIONS)
        return HashedPasscode(
            salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP),
            hash = Base64.encodeToString(derived, Base64.NO_WRAP),
            iterations = ITERATIONS
        )
    }

    fun verify(passcode: String, stored: HashedPasscode): Boolean {
        if (passcode.isEmpty()) return false
        val saltBytes = Base64.decode(stored.salt, Base64.NO_WRAP)
        val candidate = derive(passcode, saltBytes, stored.iterations)
        val expected = Base64.decode(stored.hash, Base64.NO_WRAP)
        return constantTimeEquals(candidate, expected)
    }

    private fun derive(passcode: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = CharArray(passcode.length)
        passcode.toCharArray(chars, 0, 0, passcode.length)
        try {
            val spec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            return factory.generateSecret(spec).encoded
        } finally {
            // Best-effort wipe of the in-memory copy.
            for (i in chars.indices) chars[i] = '\u0000'
        }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
