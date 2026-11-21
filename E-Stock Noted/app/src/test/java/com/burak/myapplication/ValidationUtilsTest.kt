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
            aciklamaRaw = "Depo A",
            alisFiyatiRaw = "180,25",
            kdvOraniRaw = "20",
            tedarikciRaw = "Tekno AŞ",
            depoKonumRaw = "Raf A-1"
        )
        assertTrue(result.isValid)
        assertEquals("Kablosuz Fare", result.ad)
        assertEquals("STK-0042", result.kod)
        assertEquals("Elektronik", result.kategori)
        assertEquals(15, result.adet)
        assertEquals(3, result.minStok)
        assertEquals(249.50, result.birimFiyat, 0.001)
        assertEquals(180.25, result.alisFiyati, 0.001)
        assertEquals(20, result.kdvOrani)
        assertEquals("Tekno AŞ", result.tedarikci)
        assertEquals("Raf A-1", result.depoKonum)
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
            aciklamaRaw = "",
            alisFiyatiRaw = "-5",
            kdvOraniRaw = "150"
        )
        assertFalse(result.isValid)
        assertEquals(ValidationError.REQUIRED, result.adError)
        assertEquals(ValidationError.REQUIRED, result.kodError)
        assertEquals(ValidationError.INVALID_QUANTITY, result.adetError)
        assertEquals(ValidationError.INVALID_MIN_STOCK, result.minStokError)
        assertEquals(ValidationError.INVALID_PRICE, result.birimFiyatError)
        assertEquals(ValidationError.INVALID_PRICE, result.alisFiyatiError)
        assertEquals(ValidationError.INVALID_VAT, result.kdvOraniError)
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
        assertEquals(UrunModel.DEFAULT_VAT_RATE, result.kdvOrani)
        assertEquals(UrunModel.DEFAULT_WAREHOUSE, result.depoKonum)
        assertEquals(0.0, result.birimFiyat, 0.001)
    }

    @Test
    fun validateRegistration_enforcesStrongPasswordPolicy() {
        val valid = ValidationUtils.validateRegistration(
            usernameRaw = "burak",
            emailRaw = "burak@example.com",
            businessRaw = "Depo",
            passwordRaw = "Guclu1234",
            confirmPasswordRaw = "Guclu1234"
        )
        assertTrue(valid.isValid)
        assertNull(valid.emailError)

        val invalid = ValidationUtils.validateRegistration(
            usernameRaw = "  ",
            emailRaw = "not-an-email",
            businessRaw = "",
            passwordRaw = "zayif",
            confirmPasswordRaw = "farkli"
        )
        assertFalse(invalid.isValid)
        assertEquals(ValidationError.REQUIRED, invalid.usernameError)
        assertEquals(ValidationError.INVALID_EMAIL, invalid.emailError)
        assertEquals(ValidationError.REQUIRED, invalid.businessError)
        assertEquals(ValidationError.PASSWORD_TOO_WEAK, invalid.passwordError)
        assertEquals(ValidationError.PASSWORD_MISMATCH, invalid.confirmPasswordError)
    }

    @Test
    fun validateStockAdjustment_handlesIncomingOutgoingAndInsufficientStock() {
        val incoming = ValidationUtils.validateStockAdjustment(10, "25", true, "  Fatura #12 ")
        assertTrue(incoming.isValid)
        assertEquals(25, incoming.delta)
        assertEquals("Fatura #12", incoming.note)

        val outgoingOk = ValidationUtils.validateStockAdjustment(10, "4", false, "Sevk")
        assertTrue(outgoingOk.isValid)
        assertEquals(-4, outgoingOk.delta)

        val outgoingTooMuch = ValidationUtils.validateStockAdjustment(10, "15", false, "")
        assertFalse(outgoingTooMuch.isValid)
        assertEquals(ValidationError.INSUFFICIENT_STOCK, outgoingTooMuch.amountError)

        val zeroOrBad = ValidationUtils.validateStockAdjustment(10, "0", true, "")
        assertFalse(zeroOrBad.isValid)
        assertEquals(ValidationError.INVALID_QUANTITY, zeroOrBad.amountError)
    }

    @Test
    fun validatePasswordChange_enforcesStrongRules() {
        val ok = ValidationUtils.validatePasswordChange("12345", "YeniSifre2026", "YeniSifre2026")
        assertTrue(ok.isValid)

        val bad = ValidationUtils.validatePasswordChange("", "1234", "5678")
        assertFalse(bad.isValid)
        assertEquals(ValidationError.REQUIRED, bad.currentPasswordError)
        assertEquals(ValidationError.PASSWORD_TOO_WEAK, bad.newPasswordError)
        assertEquals(ValidationError.PASSWORD_MISMATCH, bad.confirmPasswordError)
    }

    @Test
    fun validateSupplier_checksRequiredNameAndOptionalEmail() {
        val ok = ValidationUtils.validateSupplier("Tekno AŞ", "Ali", "0555", "ali@tekno.com", "123", "")
        assertTrue(ok.isValid)

        val bad = ValidationUtils.validateSupplier("  ", "", "", "bad-mail", "", "")
        assertFalse(bad.isValid)
        assertEquals(ValidationError.REQUIRED, bad.firmaAdiError)
        assertEquals(ValidationError.INVALID_EMAIL, bad.epostaError)
    }
}
