package com.burak.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.burak.myapplication.databinding.ActivityKullaniciKayitBinding

class KullaniciKayitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKullaniciKayitBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKullaniciKayitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnRegisterSubmit.setOnClickListener {
            attemptRegister()
        }
    }

    private fun attemptRegister() {
        val result = ValidationUtils.validateRegistration(
            usernameRaw = binding.etUsername.text?.toString().orEmpty(),
            emailRaw = binding.etEmail.text?.toString().orEmpty(),
            businessRaw = binding.etBusiness.text?.toString().orEmpty(),
            passwordRaw = binding.etPassword.text?.toString().orEmpty(),
            confirmPasswordRaw = binding.etPasswordConfirm.text?.toString().orEmpty()
        )

        binding.tilUsername.error = errorMessage(result.usernameError)
        binding.tilEmail.error = errorMessage(result.emailError)
        binding.tilBusiness.error = errorMessage(result.businessError)
        binding.tilPassword.error = errorMessage(result.passwordError)
        binding.tilPasswordConfirm.error = errorMessage(result.confirmPasswordError)

        if (!result.isValid) return

        if (dbHelper.userExists(result.username)) {
            binding.tilUsername.error = getString(R.string.msg_register_duplicate)
            return
        }

        val insertedId = dbHelper.addUser(
            username = result.username,
            password = result.password,
            email = result.email,
            business = result.business,
            role = UserRole.USER
        )

        if (insertedId > 0) {
            Toast.makeText(this, R.string.msg_register_success, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.msg_register_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun errorMessage(error: ValidationError?): String? = when (error) {
        ValidationError.REQUIRED -> getString(R.string.err_required_field)
        ValidationError.INVALID_EMAIL -> getString(R.string.err_invalid_email)
        ValidationError.PASSWORD_TOO_SHORT -> getString(R.string.err_password_short)
        ValidationError.PASSWORD_MISMATCH -> getString(R.string.err_password_mismatch)
        else -> null
    }
}