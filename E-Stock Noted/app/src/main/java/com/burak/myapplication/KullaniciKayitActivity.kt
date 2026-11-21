package com.burak.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KullaniciKayitActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBusiness: EditText
    private lateinit var btnKayitEt: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kullanici_kayit)

        etUsername = findViewById(R.id.editTextUsername)
        etPassword = findViewById(R.id.editTextPassword)
        etEmail = findViewById(R.id.editTextEmail)
        etBusiness = findViewById(R.id.editTextBusiness)
        btnKayitEt = findViewById(R.id.buttonKayitEt)

        btnKayitEt.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            val email = etEmail.text.toString()
            val business = etBusiness.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty() && business.isNotEmpty()) {
                val dbHelper = DatabaseHelper(this)
                val userId = dbHelper.addUser(username, password, email, business)
                if (userId > 0) {
                    // Kullanıcı başarıyla kaydedildi.
                    showToast("Kullanıcı başarıyla kaydedildi.")
                } else {
                    // Kullanıcı kaydedilemedi. Hata mesajı gösterebilirsiniz.
                    showToast("Kullanıcı kaydedilemedi. Lütfen tekrar deneyin.")
                }
            } else {
                // Kullanıcı adı, şifre, email ve business boş olmamalıdır. Kullanıcıya bir uyarı gösterebilirsiniz.
                showToast("Tüm alanları doldurunuz.")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}