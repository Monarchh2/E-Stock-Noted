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
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        val derived = deriveKey(password, salt, ITERATIONS)
        return "$PREFIX$$ITERATIONS$${salt.toHex()}$${derived.toHex()}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val parts = storedHash.split("$")
        if (parts.size == 4 && parts[0] == PREFIX) {
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = parts[2].fromHexOrNull() ?: return false
            val expected = parts[3].fromHexOrNull() ?: return false
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
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { index ->
                substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
