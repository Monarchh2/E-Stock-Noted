package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.burak.myapplication.databinding.ActivityYetkiliGirisBinding

class YetkiliGirisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYetkiliGirisBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYetkiliGirisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnAdminLogin.setOnClickListener {
            attemptAdminLogin()
        }
    }

    private fun attemptAdminLogin() {
        val username = binding.etAdminUsername.text?.toString().orEmpty().trim()
        val password = binding.etAdminPassword.text?.toString().orEmpty()

        binding.tilAdminUsername.error =
            if (username.isEmpty()) getString(R.string.err_required_field) else null
        binding.tilAdminPassword.error =
            if (password.isBlank()) getString(R.string.err_required_field) else null

        if (username.isEmpty() || password.isBlank()) return

        val adminUser = dbHelper.authenticateUser(username, password, requiredRole = UserRole.ADMIN)
        if (adminUser != null) {
            sessionManager.saveSession(adminUser)
            Toast.makeText(this, R.string.msg_login_success, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StokBilgiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.msg_admin_login_failed, Toast.LENGTH_SHORT).show()
        }
    }
}