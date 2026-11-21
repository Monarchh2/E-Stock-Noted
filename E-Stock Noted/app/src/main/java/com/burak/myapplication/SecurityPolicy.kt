package com.burak.myapplication

import android.app.Activity
import android.view.WindowManager
import java.util.Locale

enum class PasswordPolicyViolation {
    TOO_SHORT,
    MISSING_UPPERCASE,
    MISSING_LOWERCASE,
    MISSING_DIGIT
}

data class LockoutStatus(
    val isLocked: Boolean,
    val remainingSeconds: Long = 0L,
    val failedAttempts: Int = 0
)

/**
 * Enforces strong password complexity rules.
 */
object PasswordPolicy {
    const val MIN_STRONG_LENGTH = 8

    fun checkViolations(password: String): List<PasswordPolicyViolation> {
        val violations = mutableListOf<PasswordPolicyViolation>()
        if (password.length < MIN_STRONG_LENGTH) {
            violations.add(PasswordPolicyViolation.TOO_SHORT)
        }
        if (password.none { it.isUpperCase() }) {
            violations.add(PasswordPolicyViolation.MISSING_UPPERCASE)
        }
        if (password.none { it.isLowerCase() }) {
            violations.add(PasswordPolicyViolation.MISSING_LOWERCASE)
        }
        if (password.none { it.isDigit() }) {
            violations.add(PasswordPolicyViolation.MISSING_DIGIT)
        }
        return violations
    }

    fun isStrong(password: String): Boolean = checkViolations(password).isEmpty()
}

/**
 * In-memory brute-force protection with escalating lockouts per normalized username.
 * Injectable clock allows deterministic testing without sleeps.
 */
class LoginRateLimiter(
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val baseLockoutMillis: Long = DEFAULT_LOCKOUT_MILLIS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class AttemptRecord(
        var consecutiveFailures: Int = 0,
        var lockoutCount: Int = 0,
        var lockedUntilMillis: Long = 0L
    )

    private val records = mutableMapOf<String, AttemptRecord>()

    @Synchronized
    fun checkStatus(username: String): LockoutStatus {
        val key = normalizeKey(username)
        val record = records[key] ?: return LockoutStatus(isLocked = false)
        val now = clock()
        if (record.lockedUntilMillis > now) {
            val remainingSec = ((record.lockedUntilMillis - now) + 999L) / 1000L
            return LockoutStatus(
                isLocked = true,
                remainingSeconds = remainingSec,
                failedAttempts = record.consecutiveFailures
            )
        }
        if (record.lockedUntilMillis > 0L) {
            // Lockout expired; reset consecutive count for next cycle while preserving lockoutCount escalation
            record.lockedUntilMillis = 0L
            record.consecutiveFailures = 0
        }
        return LockoutStatus(isLocked = false, failedAttempts = record.consecutiveFailures)
    }

    @Synchronized
    fun recordFailure(username: String): LockoutStatus {
        val key = normalizeKey(username)
        val record = records.getOrPut(key) { AttemptRecord() }
        val now = clock()
        if (record.lockedUntilMillis > now) {
            val remainingSec = ((record.lockedUntilMillis - now) + 999L) / 1000L
            return LockoutStatus(true, remainingSec, record.consecutiveFailures)
        }
        record.consecutiveFailures += 1
        if (record.consecutiveFailures >= maxFailures) {
            record.lockoutCount += 1
            val multiplier = when {
                record.lockoutCount <= 1 -> 1L
                record.lockoutCount == 2 -> 3L
                else -> 5L
            }
            record.lockedUntilMillis = now + (baseLockoutMillis * multiplier)
            val remainingSec = ((record.lockedUntilMillis - now) + 999L) / 1000L
            return LockoutStatus(true, remainingSec, record.consecutiveFailures)
        }
        return LockoutStatus(false, 0L, record.consecutiveFailures)
    }

    @Synchronized
    fun recordSuccess(username: String) {
        records.remove(normalizeKey(username))
    }

    @Synchronized
    fun clearAll() {
        records.clear()
    }

    private fun normalizeKey(username: String): String =
        username.trim().lowercase(Locale.ROOT)

    companion object {
        const val DEFAULT_MAX_FAILURES = 5
        const val DEFAULT_LOCKOUT_MILLIS = 60_000L

        val shared: LoginRateLimiter = LoginRateLimiter()
    }
}

/**
 * Centralized RBAC guard to prevent privilege escalation across operations.
 */
object AccessGuard {
    fun requireStockWrite(role: UserRole) {
        if (!role.canManageStock) {
            throw SecurityException("Yetkisiz işlem: stok değiştirme izni yok ($role).")
        }
    }

    fun requireAdmin(role: UserRole) {
        if (!role.canManageUsers) {
            throw SecurityException("Yetkisiz işlem: yönetici yetkisi gerekli ($role).")
        }
    }
}

/**
 * Input sanitization, CSV formula injection neutralization, and overflow-safe math.
 */
object SecuritySanitizer {
    const val MAX_SHORT_TEXT = 120
    const val MAX_LONG_TEXT = 500
    private val DANGEROUS_CSV_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')

    fun sanitizeText(input: String, maxLength: Int = MAX_SHORT_TEXT): String {
        val cleaned = buildString(input.length) {
            for (ch in input) {
                if (ch == '\n' || !ch.isISOControl()) {
                    append(ch)
                }
            }
        }.trim()
        return if (cleaned.length > maxLength) cleaned.substring(0, maxLength) else cleaned
    }

    /**
     * Neutralizes spreadsheet formula injection (DDE / Excel / Calc) by prefixing dangerous leading chars with `'`.
     */
    fun sanitizeCsvCell(value: String): String {
        val cleaned = sanitizeText(value, MAX_LONG_TEXT)
        if (cleaned.isEmpty()) return ""
        return if (cleaned[0] in DANGEROUS_CSV_PREFIXES) {
            "'$cleaned"
        } else {
            cleaned
        }
    }

    /**
     * Computes current + delta without integer overflow or negative stock.
     * Returns null if result would be negative or exceed Int.MAX_VALUE.
     */
    fun safeAddQuantity(current: Int, delta: Int): Int? {
        if (current < 0) return null
        val sum = current.toLong() + delta.toLong()
        if (sum < 0L || sum > Int.MAX_VALUE.toLong()) return null
        return sum.toInt()
    }
}

object SecurityWindowHelper {
    fun applySecureFlag(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
