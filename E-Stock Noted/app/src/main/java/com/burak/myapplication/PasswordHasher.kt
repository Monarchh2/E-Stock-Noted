package com.burak.myapplication

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing with constant-time verification.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2"
    const val CURRENT_ITERATIONS = 65_536
    private const val MIN_ALLOWED_ITERATIONS = 10_000
    private const val MAX_ALLOWED_ITERATIONS = 500_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        val derived = deriveKey(password, salt, CURRENT_ITERATIONS)
        return "$PREFIX$$CURRENT_ITERATIONS$${salt.toHex()}$${derived.toHex()}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || password.isEmpty()) return false
        val parts = storedHash.split("$")
        if (parts.firstOrNull() == PREFIX) {
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            if (iterations !in MIN_ALLOWED_ITERATIONS..MAX_ALLOWED_ITERATIONS) return false
            val salt = parts[2].fromHexOrNull() ?: return false
            val expected = parts[3].fromHexOrNull() ?: return false
            if (salt.size < 8 || expected.size != KEY_LENGTH_BITS / 8) return false
            val actual = deriveKey(password, salt, iterations)
            return MessageDigest.isEqual(expected, actual)
        }
        // Legacy plaintext fallback for rows migrated from DB version 1
        return MessageDigest.isEqual(
            password.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun isHashed(storedValue: String): Boolean = storedValue.startsWith("$PREFIX$")

    fun needsRehash(storedValue: String): Boolean {
        if (!isHashed(storedValue)) return true
        val parts = storedValue.split("$")
        if (parts.size != 4) return true
        val iterations = parts[1].toIntOrNull() ?: return true
        return iterations < CURRENT_ITERATIONS
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHexOrNull(): ByteArray? {
        if (isEmpty() || length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { index ->
                substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
