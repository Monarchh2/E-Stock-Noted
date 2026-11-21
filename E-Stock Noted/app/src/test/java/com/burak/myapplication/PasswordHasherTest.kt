package com.burak.myapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hashAndVerify_succeedsForMatchingPassword() {
        val hash = PasswordHasher.hash("gizli123")
        assertTrue(PasswordHasher.isHashed(hash))
        assertTrue(PasswordHasher.verify("gizli123", hash))
        assertFalse(PasswordHasher.verify("yanlis123", hash))
    }

    @Test
    fun hash_usesUniqueSaltPerCall() {
        val hash1 = PasswordHasher.hash("ayniSifre")
        val hash2 = PasswordHasher.hash("ayniSifre")
        assertNotEquals(hash1, hash2)
        assertTrue(PasswordHasher.verify("ayniSifre", hash1))
        assertTrue(PasswordHasher.verify("ayniSifre", hash2))
    }

    @Test
    fun verify_handlesLegacyPlaintextAndBlankStoredValues() {
        assertFalse(PasswordHasher.isHashed("legacy123"))
        assertTrue(PasswordHasher.verify("legacy123", "legacy123"))
        assertFalse(PasswordHasher.verify("wrong", "legacy123"))
        assertFalse(PasswordHasher.verify("anything", ""))
    }
}
