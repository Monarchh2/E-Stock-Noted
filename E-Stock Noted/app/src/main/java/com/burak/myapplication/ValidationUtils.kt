package com.burak.myapplication

data class ProductValidationResult(
    val ad: String = "",
    val kod: String = "",
    val kategori: String = UrunModel.DEFAULT_CATEGORY,
    val adet: Int = 0,
    val minStok: Int = UrunModel.DEFAULT_MIN_STOCK,
    val birimFiyat: Double = 0.0,
    val aciklama: String = "",
    val adError: ValidationError? = null,
    val kodError: ValidationError? = null,
    val adetError: ValidationError? = null,
    val minStokError: ValidationError? = null,
    val birimFiyatError: ValidationError? = null
) {
    val isValid: Boolean
        get() = adError == null &&
            kodError == null &&
            adetError == null &&
            minStokError == null &&
            birimFiyatError == null
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

enum class ValidationError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    INVALID_QUANTITY,
    INVALID_MIN_STOCK,
    INVALID_PRICE
}

object ValidationUtils {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private const val MIN_PASSWORD_LENGTH = 4

    fun parseNonNegativeInt(raw: String): Int? {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return null
        return if (parsed >= 0) parsed else null
    }

    fun parseNonNegativeDouble(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return 0.0
        val normalized = trimmed.replace(',', '.')
        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (!parsed.isNaN() && !parsed.isInfinite() && parsed >= 0.0) parsed else null
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
        aciklamaRaw: String
    ): ProductValidationResult {
        val ad = adRaw.trim()
        val kod = kodRaw.trim()
        val kategori = kategoriRaw.trim().ifEmpty { UrunModel.DEFAULT_CATEGORY }
        val aciklama = aciklamaRaw.trim()

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

        return ProductValidationResult(
            ad = ad,
            kod = kod,
            kategori = kategori,
            adet = adet ?: 0,
            minStok = minStok ?: UrunModel.DEFAULT_MIN_STOCK,
            birimFiyat = birimFiyat ?: 0.0,
            aciklama = aciklama,
            adError = adError,
            kodError = kodError,
            adetError = adetError,
            minStokError = minStokError,
            birimFiyatError = birimFiyatError
        )
    }

    fun validateRegistration(
        usernameRaw: String,
        emailRaw: String,
        businessRaw: String,
        passwordRaw: String,
        confirmPasswordRaw: String
    ): RegistrationValidationResult {
        val username = usernameRaw.trim()
        val email = emailRaw.trim()
        val business = businessRaw.trim()

        val usernameError = if (username.isEmpty()) ValidationError.REQUIRED else null
        val emailError = when {
            email.isEmpty() -> ValidationError.REQUIRED
            !isValidEmail(email) -> ValidationError.INVALID_EMAIL
            else -> null
        }
        val businessError = if (business.isEmpty()) ValidationError.REQUIRED else null
        val passwordError = when {
            passwordRaw.isBlank() -> ValidationError.REQUIRED
            passwordRaw.length < MIN_PASSWORD_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
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
}
