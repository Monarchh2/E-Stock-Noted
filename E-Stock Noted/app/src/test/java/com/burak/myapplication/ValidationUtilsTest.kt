package com.burak.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validateProductInput_acceptsAlphanumericSkuAndTurkishDecimal() {
        val result = ValidationUtils.validateProductInput(
            adRaw = "  Kablosuz Fare  ",
            kodRaw = "  STK-0042  ",
            kategoriRaw = "Elektronik",
            adetRaw = "15",
            minStokRaw = "3",
            birimFiyatRaw = "249,50",
            aciklamaRaw = "Depo A"
        )
        assertTrue(result.isValid)
        assertEquals("Kablosuz Fare", result.ad)
        assertEquals("STK-0042", result.kod)
        assertEquals("Elektronik", result.kategori)
        assertEquals(15, result.adet)
        assertEquals(3, result.minStok)
        assertEquals(249.50, result.birimFiyat, 0.001)
    }

    @Test
    fun validateProductInput_rejectsEmptyOrInvalidNumericFieldsWithoutCrashing() {
        val result = ValidationUtils.validateProductInput(
            adRaw = "   ",
            kodRaw = "",
            kategoriRaw = "",
            adetRaw = "abc",
            minStokRaw = "-2",
            birimFiyatRaw = "-10",
            aciklamaRaw = ""
        )
        assertFalse(result.isValid)
        assertEquals(ValidationError.REQUIRED, result.adError)
        assertEquals(ValidationError.REQUIRED, result.kodError)
        assertEquals(ValidationError.INVALID_QUANTITY, result.adetError)
        assertEquals(ValidationError.INVALID_MIN_STOCK, result.minStokError)
        assertEquals(ValidationError.INVALID_PRICE, result.birimFiyatError)
    }

    @Test
    fun validateProductInput_usesDefaultsForBlankOptionalFields() {
        val result = ValidationUtils.validateProductInput(
            adRaw = "Kalem",
            kodRaw = "0042",
            kategoriRaw = "",
            adetRaw = "0",
            minStokRaw = "",
            birimFiyatRaw = "",
            aciklamaRaw = ""
        )
        assertTrue(result.isValid)
        assertEquals("0042", result.kod)
        assertEquals(UrunModel.DEFAULT_CATEGORY, result.kategori)
        assertEquals(UrunModel.DEFAULT_MIN_STOCK, result.minStok)
        assertEquals(0.0, result.birimFiyat, 0.001)
    }

    @Test
    fun validateRegistration_validatesAllFields() {
        val valid = ValidationUtils.validateRegistration(
            usernameRaw = "burak",
            emailRaw = "burak@example.com",
            businessRaw = "Depo",
            passwordRaw = "1234",
            confirmPasswordRaw = "1234"
        )
        assertTrue(valid.isValid)
        assertNull(valid.emailError)

        val invalid = ValidationUtils.validateRegistration(
            usernameRaw = "  ",
            emailRaw = "not-an-email",
            businessRaw = "",
            passwordRaw = "123",
            confirmPasswordRaw = "456"
        )
        assertFalse(invalid.isValid)
        assertEquals(ValidationError.REQUIRED, invalid.usernameError)
        assertEquals(ValidationError.INVALID_EMAIL, invalid.emailError)
        assertEquals(ValidationError.REQUIRED, invalid.businessError)
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, invalid.passwordError)
        assertEquals(ValidationError.PASSWORD_MISMATCH, invalid.confirmPasswordError)
    }
}
