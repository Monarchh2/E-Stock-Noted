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

data class LockoutRecord(
    val consecutiveFailures: Int = 0,
    val lockoutCount: Int = 0,
    val lockedUntilMillis: Long = 0L
)

/**
 * Persistence abstraction for brute-force lockout state so lockouts survive app restarts.
 */
interface LockoutStateStore {
    fun get(key: String): LockoutRecord?
    fun put(key: String, record: LockoutRecord)
    fun remove(key: String)
    fun clear()
}

class InMemoryLockoutStore : LockoutStateStore {
    private val map = mutableMapOf<String, LockoutRecord>()

    @Synchronized
    override fun get(key: String): LockoutRecord? = map[key]

    @Synchronized
    override fun put(key: String, record: LockoutRecord) {
        map[key] = record
    }

    @Synchronized
    override fun remove(key: String) {
        map.remove(key)
    }

    @Synchronized
    override fun clear() {
        map.clear()
    }
}

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
 * Brute-force protection with escalating lockouts per normalized username.
 * Backed by LockoutStateStore (SQLite in production, InMemoryLockoutStore in tests).
 */
class LoginRateLimiter(
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val baseLockoutMillis: Long = DEFAULT_LOCKOUT_MILLIS,
    private val store: LockoutStateStore = InMemoryLockoutStore(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    @Synchronized
    fun checkStatus(username: String): LockoutStatus {
        val key = normalizeKey(username)
        val record = store.get(key) ?: return LockoutStatus(isLocked = false)
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
            store.put(
                key,
                record.copy(consecutiveFailures = 0, lockedUntilMillis = 0L)
            )
            return LockoutStatus(isLocked = false, failedAttempts = 0)
        }
        return LockoutStatus(isLocked = false, failedAttempts = record.consecutiveFailures)
    }

    @Synchronized
    fun recordFailure(username: String): LockoutStatus {
        val key = normalizeKey(username)
        val existing = store.get(key) ?: LockoutRecord()
        val now = clock()
        if (existing.lockedUntilMillis > now) {
            val remainingSec = ((existing.lockedUntilMillis - now) + 999L) / 1000L
            return LockoutStatus(true, remainingSec, existing.consecutiveFailures)
        }
        val baseFailures = if (existing.lockedUntilMillis > 0L) 0 else existing.consecutiveFailures
        val nextFailures = baseFailures + 1
        if (nextFailures >= maxFailures) {
            val nextLockoutCount = existing.lockoutCount + 1
            val multiplier = when {
                nextLockoutCount <= 1 -> 1L
                nextLockoutCount == 2 -> 3L
                else -> 5L
            }
            val lockedUntil = now + (baseLockoutMillis * multiplier)
            store.put(
                key,
                LockoutRecord(
                    consecutiveFailures = nextFailures,
                    lockoutCount = nextLockoutCount,
                    lockedUntilMillis = lockedUntil
                )
            )
            val remainingSec = ((lockedUntil - now) + 999L) / 1000L
            return LockoutStatus(true, remainingSec, nextFailures)
        }
        store.put(
            key,
            existing.copy(consecutiveFailures = nextFailures, lockedUntilMillis = 0L)
        )
        return LockoutStatus(false, 0L, nextFailures)
    }

    @Synchronized
    fun recordSuccess(username: String) {
        store.remove(normalizeKey(username))
    }

    @Synchronized
    fun clearAll() {
        store.clear()
    }

    private fun normalizeKey(username: String): String =
        username.trim().lowercase(Locale.ROOT)

    companion object {
        const val DEFAULT_MAX_FAILURES = 5
        const val DEFAULT_LOCKOUT_MILLIS = 60_000L
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
 * Verifies that an active session has not expired and that the account is still active in SQLite,
 * syncing live role changes immediately.
 */
object SessionGuard {
    fun verifyActiveSession(
        sessionManager: SessionManager,
        database: DatabaseHelper,
        nowMillis: Long = System.currentTimeMillis()
    ): UserModel? {
        if (!sessionManager.isLoggedIn() || sessionManager.isSessionExpired(nowMillis)) {
            sessionManager.clearSession()
            return null
        }
        val username = sessionManager.getUsername()
        val liveUser = database.getUserByUsername(username)
        if (liveUser == null || !liveUser.isActive) {
            database.guvenlikKaydiEkle(
                "OTURUM_IPTAL",
                username,
                "Pasif veya silinmiş hesabın oturumu sonlandırıldı",
                false
            )
            sessionManager.clearSession()
            return null
        }
        sessionManager.saveSession(liveUser, nowMillis)
        return liveUser
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
