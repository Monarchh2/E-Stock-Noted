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
        SecurityWindowHelper.applySecureFlag(this)

        binding = ActivityKullaniciGirisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { attemptLogin() }
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

        val rateLimiter = dbHelper.createRateLimiter()
        val lockStatus = rateLimiter.checkStatus(username)
        if (lockStatus.isLocked) {
            Toast.makeText(
                this,
                getString(R.string.msg_account_locked, lockStatus.remainingSeconds),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val user = dbHelper.authenticateUser(
            username = username,
            password = password,
            allowedRoles = setOf(UserRole.USER, UserRole.DEPO)
        )
        if (user != null) {
            rateLimiter.recordSuccess(username)
            dbHelper.guvenlikKaydiEkle("GIRIS", user.username, "Kullanıcı girişi (${user.role.name})", true)
            sessionManager.saveSession(user)
            Toast.makeText(this, R.string.msg_login_success, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StokBilgiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            val afterFail = rateLimiter.recordFailure(username)
            dbHelper.guvenlikKaydiEkle(
                if (afterFail.isLocked) "HESAP_KILIT" else "HATALI_GIRIS",
                username,
                "Kullanıcı giriş denemesi başarısız (${afterFail.failedAttempts})",
                false
            )
            if (afterFail.isLocked) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_account_locked, afterFail.remainingSeconds),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, R.string.msg_login_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
