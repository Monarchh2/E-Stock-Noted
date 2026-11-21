package com.burak.myapplication

data class ProductValidationResult(
    val ad: String = "",
    val kod: String = "",
    val kategori: String = UrunModel.DEFAULT_CATEGORY,
    val adet: Int = 0,
    val minStok: Int = UrunModel.DEFAULT_MIN_STOCK,
    val birimFiyat: Double = 0.0,
    val alisFiyati: Double = 0.0,
    val kdvOrani: Int = UrunModel.DEFAULT_VAT_RATE,
    val tedarikci: String = "",
    val depoKonum: String = UrunModel.DEFAULT_WAREHOUSE,
    val aciklama: String = "",
    val adError: ValidationError? = null,
    val kodError: ValidationError? = null,
    val adetError: ValidationError? = null,
    val minStokError: ValidationError? = null,
    val birimFiyatError: ValidationError? = null,
    val alisFiyatiError: ValidationError? = null,
    val kdvOraniError: ValidationError? = null
) {
    val isValid: Boolean
        get() = adError == null &&
            kodError == null &&
            adetError == null &&
            minStokError == null &&
            birimFiyatError == null &&
            alisFiyatiError == null &&
            kdvOraniError == null
}

data class RegistrationValidationResult(
    val username: String = "",
    val email: String = "",
    val business: String = "",
    val password: String = "",
    val usernameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val businessError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null
) {
    val isValid: Boolean
        get() = usernameError == null &&
            emailError == null &&
            businessError == null &&
            passwordError == null &&
            confirmPasswordError == null
}

data class StockAdjustmentValidationResult(
    val amount: Int = 0,
    val delta: Int = 0,
    val note: String = "",
    val amountError: ValidationError? = null
) {
    val isValid: Boolean
        get() = amountError == null
}

data class PasswordChangeValidationResult(
    val currentPassword: String = "",
    val newPassword: String = "",
    val currentPasswordError: ValidationError? = null,
    val newPasswordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null
) {
    val isValid: Boolean
        get() = currentPasswordError == null &&
            newPasswordError == null &&
            confirmPasswordError == null
}

data class SupplierValidationResult(
    val firmaAdi: String = "",
    val yetkiliKisi: String = "",
    val telefon: String = "",
    val eposta: String = "",
    val vergiNo: String = "",
    val notlar: String = "",
    val firmaAdiError: ValidationError? = null,
    val epostaError: ValidationError? = null
) {
    val isValid: Boolean
        get() = firmaAdiError == null && epostaError == null
}

enum class ValidationError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_WEAK,
    PASSWORD_REUSED,
    PASSWORD_MISMATCH,
    INVALID_QUANTITY,
    QUANTITY_OVERFLOW,
    INSUFFICIENT_STOCK,
    INVALID_MIN_STOCK,
    INVALID_PRICE,
    INVALID_VAT
}

object ValidationUtils {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private const val MAX_PRICE = 1_000_000_000.0

    fun parseNonNegativeInt(raw: String): Int? {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return null
        return if (parsed >= 0) parsed else null
    }

    fun parsePositiveInt(raw: String): Int? {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return null
        return if (parsed > 0) parsed else null
    }

    fun parseNonNegativeDouble(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return 0.0
        val normalized = trimmed.replace(',', '.')
        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (!parsed.isNaN() && !parsed.isInfinite() && parsed in 0.0..MAX_PRICE) {
            parsed
        } else {
            null
        }
    }

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    fun validateProductInput(
        adRaw: String,
        kodRaw: String,
        kategoriRaw: String,
        adetRaw: String,
        minStokRaw: String,
        birimFiyatRaw: String,
        aciklamaRaw: String,
        alisFiyatiRaw: String = "",
        kdvOraniRaw: String = "",
        tedarikciRaw: String = "",
        depoKonumRaw: String = ""
    ): ProductValidationResult {
        val ad = SecuritySanitizer.sanitizeText(adRaw)
        val kod = SecuritySanitizer.sanitizeText(kodRaw, 60)
        val kategori = SecuritySanitizer.sanitizeText(kategoriRaw, 60).ifEmpty { UrunModel.DEFAULT_CATEGORY }
        val tedarikci = SecuritySanitizer.sanitizeText(tedarikciRaw, 80)
        val depoKonum = SecuritySanitizer.sanitizeText(depoKonumRaw, 60).ifEmpty { UrunModel.DEFAULT_WAREHOUSE }
        val aciklama = SecuritySanitizer.sanitizeText(aciklamaRaw, SecuritySanitizer.MAX_LONG_TEXT)

        val adError = if (ad.isEmpty()) ValidationError.REQUIRED else null
        val kodError = if (kod.isEmpty()) ValidationError.REQUIRED else null

        val adet = parseNonNegativeInt(adetRaw)
        val adetError = if (adet == null) ValidationError.INVALID_QUANTITY else null

        val minStok = if (minStokRaw.isBlank()) {
            UrunModel.DEFAULT_MIN_STOCK
        } else {
            parseNonNegativeInt(minStokRaw)
        }
        val minStokError = if (minStok == null) ValidationError.INVALID_MIN_STOCK else null

        val birimFiyat = parseNonNegativeDouble(birimFiyatRaw)
        val birimFiyatError = if (birimFiyat == null) ValidationError.INVALID_PRICE else null

        val alisFiyati = parseNonNegativeDouble(alisFiyatiRaw)
        val alisFiyatiError = if (alisFiyati == null) ValidationError.INVALID_PRICE else null

        val kdvOrani = if (kdvOraniRaw.isBlank()) {
            UrunModel.DEFAULT_VAT_RATE
        } else {
            parseNonNegativeInt(kdvOraniRaw)?.takeIf { it in 0..100 }
        }
        val kdvOraniError = if (kdvOrani == null) ValidationError.INVALID_VAT else null

        return ProductValidationResult(
            ad = ad,
            kod = kod,
            kategori = kategori,
            adet = adet ?: 0,
            minStok = minStok ?: UrunModel.DEFAULT_MIN_STOCK,
            birimFiyat = birimFiyat ?: 0.0,
            alisFiyati = alisFiyati ?: 0.0,
            kdvOrani = kdvOrani ?: UrunModel.DEFAULT_VAT_RATE,
            tedarikci = tedarikci,
            depoKonum = depoKonum,
            aciklama = aciklama,
            adError = adError,
            kodError = kodError,
            adetError = adetError,
            minStokError = minStokError,
            birimFiyatError = birimFiyatError,
            alisFiyatiError = alisFiyatiError,
            kdvOraniError = kdvOraniError
        )
    }

    fun validateRegistration(
        usernameRaw: String,
        emailRaw: String,
        businessRaw: String,
        passwordRaw: String,
        confirmPasswordRaw: String
    ): RegistrationValidationResult {
        val username = SecuritySanitizer.sanitizeText(usernameRaw, 60)
        val email = SecuritySanitizer.sanitizeText(emailRaw, 120)
        val business = SecuritySanitizer.sanitizeText(businessRaw, 80)

        val usernameError = if (username.isEmpty()) ValidationError.REQUIRED else null
        val emailError = when {
            email.isEmpty() -> ValidationError.REQUIRED
            !isValidEmail(email) -> ValidationError.INVALID_EMAIL
            else -> null
        }
        val businessError = if (business.isEmpty()) ValidationError.REQUIRED else null
        val passwordError = when {
            passwordRaw.isBlank() -> ValidationError.REQUIRED
            !PasswordPolicy.isStrong(passwordRaw) -> ValidationError.PASSWORD_TOO_WEAK
            else -> null
        }
        val confirmPasswordError = when {
            confirmPasswordRaw.isBlank() -> ValidationError.REQUIRED
            passwordRaw != confirmPasswordRaw -> ValidationError.PASSWORD_MISMATCH
            else -> null
        }

        return RegistrationValidationResult(
            username = username,
            email = email,
            business = business,
            password = passwordRaw,
            usernameError = usernameError,
            emailError = emailError,
            businessError = businessError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
    }

    fun validateStockAdjustment(
        currentQty: Int,
        amountRaw: String,
        isIncoming: Boolean,
        noteRaw: String
    ): StockAdjustmentValidationResult {
        val amount = parsePositiveInt(amountRaw)
            ?: return StockAdjustmentValidationResult(amountError = ValidationError.INVALID_QUANTITY)

        if (!isIncoming && amount > currentQty) {
            return StockAdjustmentValidationResult(amountError = ValidationError.INSUFFICIENT_STOCK)
        }
        val delta = if (isIncoming) amount else -amount
        if (SecuritySanitizer.safeAddQuantity(currentQty, delta) == null) {
            return StockAdjustmentValidationResult(amountError = ValidationError.QUANTITY_OVERFLOW)
        }
        return StockAdjustmentValidationResult(
            amount = amount,
            delta = delta,
            note = SecuritySanitizer.sanitizeText(noteRaw, 200)
        )
    }

    fun validatePasswordChange(
        currentPasswordRaw: String,
        newPasswordRaw: String,
        confirmPasswordRaw: String
    ): PasswordChangeValidationResult {
        val currentError = if (currentPasswordRaw.isBlank()) ValidationError.REQUIRED else null
        val newError = when {
            newPasswordRaw.isBlank() -> ValidationError.REQUIRED
            newPasswordRaw == currentPasswordRaw -> ValidationError.PASSWORD_REUSED
            !PasswordPolicy.isStrong(newPasswordRaw) -> ValidationError.PASSWORD_TOO_WEAK
            else -> null
        }
        val confirmError = when {
            confirmPasswordRaw.isBlank() -> ValidationError.REQUIRED
            newPasswordRaw != confirmPasswordRaw -> ValidationError.PASSWORD_MISMATCH
            else -> null
        }

        return PasswordChangeValidationResult(
            currentPassword = currentPasswordRaw,
            newPassword = newPasswordRaw,
            currentPasswordError = currentError,
            newPasswordError = newError,
            confirmPasswordError = confirmError
        )
    }

    fun validateSupplier(
        firmaAdiRaw: String,
        yetkiliRaw: String,
        telefonRaw: String,
        epostaRaw: String,
        vergiNoRaw: String,
        notlarRaw: String
    ): SupplierValidationResult {
        val firma = SecuritySanitizer.sanitizeText(firmaAdiRaw, 100)
        val yetkili = SecuritySanitizer.sanitizeText(yetkiliRaw, 80)
        val telefon = SecuritySanitizer.sanitizeText(telefonRaw, 30)
        val eposta = SecuritySanitizer.sanitizeText(epostaRaw, 120)
        val vergiNo = SecuritySanitizer.sanitizeText(vergiNoRaw, 30)
        val notlar = SecuritySanitizer.sanitizeText(notlarRaw, 250)

        val firmaError = if (firma.isEmpty()) ValidationError.REQUIRED else null
        val epostaError = if (eposta.isNotEmpty() && !isValidEmail(eposta)) {
            ValidationError.INVALID_EMAIL
        } else {
            null
        }

        return SupplierValidationResult(
            firmaAdi = firma,
            yetkiliKisi = yetkili,
            telefon = telefon,
            eposta = eposta,
            vergiNo = vergiNo,
            notlar = notlar,
            firmaAdiError = firmaError,
            epostaError = epostaError
        )
    }
}
