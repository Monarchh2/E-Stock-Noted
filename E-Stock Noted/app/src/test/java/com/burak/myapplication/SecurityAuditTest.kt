package com.burak.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Comprehensive security test suite covering brute-force protection, CSV formula injection,
 * strong password policy, cryptographic hash integrity, RBAC privilege escalation,
 * input sanitization, and integer/numeric overflow defenses.
 */
class SecurityAuditTest {

    @Test
    fun bruteForceRateLimiter_locksAfterFiveFailuresAndEscalatesAndResets() {
        var fakeNow = 1_000_000L
        val limiter = LoginRateLimiter(
            maxFailures = 5,
            baseLockoutMillis = 60_000L,
            clock = { fakeNow }
        )

        repeat(4) { attempt ->
            val status = limiter.recordFailure("Admin")
            assertFalse("Attempt ${attempt + 1} should not lock", status.isLocked)
        }

        // 5th failure with different casing/spacing triggers 60s lockout
        val locked = limiter.recordFailure("  admin  ")
        assertTrue(locked.isLocked)
        assertEquals(60L, locked.remainingSeconds)

        // Still locked 30s later
        fakeNow += 30_000L
        val midStatus = limiter.checkStatus("ADMIN")
        assertTrue(midStatus.isLocked)
        assertEquals(30L, midStatus.remainingSeconds)

        // Unlocked after 60s
        fakeNow += 31_000L
        assertFalse(limiter.checkStatus("admin").isLocked)

        // Second lockout escalates to 3x (180s)
        repeat(4) { limiter.recordFailure("admin") }
        val secondLock = limiter.recordFailure("admin")
        assertTrue(secondLock.isLocked)
        assertEquals(180L, secondLock.remainingSeconds)

        // Successful login clears lockout history completely
        fakeNow += 181_000L
        limiter.recordSuccess("admin")
        assertEquals(0, limiter.checkStatus("admin").failedAttempts)
    }

    @Test
    fun persistentLockoutStore_survivesAppRestartSimulation() {
        var fakeNow = 5_000_000L
        val persistentStore = InMemoryLockoutStore()

        // First app process hits 5 failures and gets locked
        val limiterBeforeRestart = LoginRateLimiter(store = persistentStore, clock = { fakeNow })
        repeat(5) { limiterBeforeRestart.recordFailure("depoUser") }
        assertTrue(limiterBeforeRestart.checkStatus("depoUser").isLocked)

        // Simulated force-stop & relaunch: brand-new LoginRateLimiter backed by same store
        fakeNow += 15_000L
        val limiterAfterRestart = LoginRateLimiter(store = persistentStore, clock = { fakeNow })
        val status = limiterAfterRestart.checkStatus("DEPOUSER")
        assertTrue("Lockout must persist across restart", status.isLocked)
        assertEquals(45L, status.remainingSeconds)
    }

    @Test
    fun csvFormulaInjection_neutralizesAllDangerousPrefixes() {
        val payloads = listOf(
            "=CMD|' /C calc'!A0",
            "+SUM(A1:A10)",
            "-2+3+cmd|' /C calc'!A0",
            "@SUM(1+1)*cmd|' /C calc'!A0"
        )
        for (payload in payloads) {
            val sanitized = SecuritySanitizer.sanitizeCsvCell(payload)
            assertTrue("Payload not neutralized: $sanitized", sanitized.startsWith("'"))
        }

        val maliciousProduct = UrunModel(
            id = 99,
            ad = "=HYPERLINK(\"http://evil.test\",\"Click\")",
            kod = "+1+1",
            kategori = "@Genel",
            adet = 5,
            minStok = 2,
            birimFiyat = 10.0,
            tedarikci = "-BadSupplier",
            aciklama = "=1+2"
        )
        val csv = StokCsvExporter.toCsv(listOf(maliciousProduct))
        assertTrue(csv.contains("'=HYPERLINK"))
        assertTrue(csv.contains("'+1+1"))
        assertTrue(csv.contains("'@Genel"))
        assertTrue(csv.contains("'-BadSupplier"))
        assertTrue(csv.contains("'=1+2"))
    }

    @Test
    fun passwordPolicy_enforcesLengthUppercaseLowercaseAndDigit() {
        assertTrue(PasswordPolicy.isStrong("GucluSifre2026"))
        assertFalse(PasswordPolicy.isStrong("Short1A"))
        assertFalse(PasswordPolicy.isStrong("kucukharf123"))
        assertFalse(PasswordPolicy.isStrong("BUYUKHARF123"))
        assertFalse(PasswordPolicy.isStrong("RakamsizSifre"))

        val violations = PasswordPolicy.checkViolations("abc")
        assertTrue(PasswordPolicyViolation.TOO_SHORT in violations)
        assertTrue(PasswordPolicyViolation.MISSING_UPPERCASE in violations)
        assertTrue(PasswordPolicyViolation.MISSING_DIGIT in violations)
    }

    @Test
    fun passwordHasher_rejectsTamperedMalformedAndWeakIterationHashes() {
        val validHash = PasswordHasher.hash("GucluSifre2026")
        assertTrue(validHash.startsWith("pbkdf2$${PasswordHasher.CURRENT_ITERATIONS}$"))
        assertFalse(PasswordHasher.needsRehash(validHash))
        assertTrue(PasswordHasher.verify("GucluSifre2026", validHash))

        // Tamper last hex character
        val lastChar = if (validHash.last() == 'a') 'b' else 'a'
        val tampered = validHash.dropLast(1) + lastChar
        assertFalse(PasswordHasher.verify("GucluSifre2026", tampered))

        // Reject malformed / out-of-range iterations / empty inputs
        assertFalse(PasswordHasher.verify("GucluSifre2026", "pbkdf2\$100\$0011223344556677\$0011"))
        assertFalse(PasswordHasher.verify("GucluSifre2026", "pbkdf2\$99999999\$0011223344556677\$0011"))
        assertFalse(PasswordHasher.verify("GucluSifre2026", "pbkdf2\$65536\$nothex\$nothex"))
        assertFalse(PasswordHasher.verify("", validHash))

        // Legacy 10k hash flagged for rehash
        assertTrue(PasswordHasher.needsRehash("pbkdf2\$10000\$00112233445566778899aabbccddeeff\$00"))
        assertTrue(PasswordHasher.needsRehash("legacyPlaintext"))
    }

    @Test
    fun rbacAccessGuard_blocksPrivilegeEscalation() {
        // USER cannot write stock or access admin panel
        assertThrowsSecurity { AccessGuard.requireStockWrite(UserRole.USER) }
        assertThrowsSecurity { AccessGuard.requireAdmin(UserRole.USER) }

        // DEPO can write stock but cannot access admin user management
        AccessGuard.requireStockWrite(UserRole.DEPO)
        assertThrowsSecurity { AccessGuard.requireAdmin(UserRole.DEPO) }

        // ADMIN passes both
        AccessGuard.requireStockWrite(UserRole.ADMIN)
        AccessGuard.requireAdmin(UserRole.ADMIN)
    }

    @Test
    fun inputSanitizerAndOverflowProtection_handlesMaliciousAndExtremeInputs() {
        // Strips null bytes and control characters, truncates oversized payloads
        val dirty = "Admin\u0000\u0007' OR '1'='1" + "A".repeat(300)
        val clean = SecuritySanitizer.sanitizeText(dirty, 50)
        assertFalse(clean.contains('\u0000'))
        assertFalse(clean.contains('\u0007'))
        assertEquals(50, clean.length)

        // Integer overflow and negative stock guards
        assertNull(SecuritySanitizer.safeAddQuantity(Int.MAX_VALUE, 1))
        assertNull(SecuritySanitizer.safeAddQuantity(5, -6))
        assertEquals(0, SecuritySanitizer.safeAddQuantity(5, -5))
        assertEquals(15, SecuritySanitizer.safeAddQuantity(10, 5))

        // Bulk stock adjustment rejects overflow
        val overflowCheck = ValidationUtils.validateStockAdjustment(Int.MAX_VALUE - 2, "10", true, "test")
        assertFalse(overflowCheck.isValid)
        assertEquals(ValidationError.QUANTITY_OVERFLOW, overflowCheck.amountError)

        // Non-finite and negative prices rejected
        assertNull(ValidationUtils.parseNonNegativeDouble("NaN"))
        assertNull(ValidationUtils.parseNonNegativeDouble("Infinity"))
        assertNull(ValidationUtils.parseNonNegativeDouble("-0.01"))
        assertNull(ValidationUtils.parseNonNegativeDouble("999999999999"))
        assertNotNull(ValidationUtils.parseNonNegativeDouble("125,75"))
    }

    private fun assertThrowsSecurity(block: () -> Unit) {
        try {
            block()
            fail("Expected SecurityException was not thrown")
        } catch (_: SecurityException) {
            // expected
        }
    }
}
