package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.burak.myapplication.databinding.ActivityKullaniciGirisBinding

class KullaniciGirisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKullaniciGirisBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKullaniciGirisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, KullaniciKayitActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        binding.tilUsername.error = if (username.isEmpty()) getString(R.string.err_required_field) else null
        binding.tilPassword.error = if (password.isBlank()) getString(R.string.err_required_field) else null

        if (username.isEmpty() || password.isBlank()) return

        val user = dbHelper.authenticateUser(username, password, requiredRole = UserRole.USER)
        if (user != null) {
            sessionManager.saveSession(user)
            Toast.makeText(this, R.string.msg_login_success, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StokBilgiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.msg_login_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
