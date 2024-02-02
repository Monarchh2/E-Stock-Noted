
package com.burak.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

// Kullanıcı Giriş Ekranı Aktivitesi
class KullaniciGirisActivity : AppCompatActivity() {

    // Ekran üzerindeki elemanların tanımlanması
    private lateinit var etUsername: EditText // Kullanıcı adı giriş alanı
    private lateinit var etPassword: EditText // Şifre giriş alanı
    private lateinit var btnKullaniciGiris: Button // Giriş butonu
    private lateinit var btnKullaniciKayit: Button // Kayıt butonu

    // Aktivitenin oluşturulma aşaması
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kullanici_giris) // XML layout dosyasının belirlenmesi

        // XML layout dosyasındaki elemanların tanımlanması
        etUsername = findViewById(R.id.KGirisAd) // Kullanıcı adı giriş alanı
        etPassword = findViewById(R.id.KKGirisSifre) // Şifre giriş alanı
        btnKullaniciGiris = findViewById(R.id.KullaniciGirisbutton) // Giriş butonu
        btnKullaniciKayit = findViewById(R.id.KullaniciGiris_kayitbut) // Kayıt butonu

        // Giriş butonuna tıklanıldığında gerçekleşecek işlemler
        btnKullaniciGiris.setOnClickListener {
            val username = etUsername.text.toString() // Kullanıcı adı
            val password = etPassword.text.toString() // Şifre

            // Kullanıcının bilgilerini kontrol et
            if (checkUser(username, password)) {
                val intent = Intent(this, StokBilgiActivity::class.java)
                startActivity(intent) // Yeni aktiviteye geçiş
                finish() // Bu aktiviteyi kapat
            } else {
                // Kullanıcı adı veya şifre yanlışsa kullanıcıya bir uyarı gösterilebilir.
                Toast.makeText(this, "Kullanıcı adı veya şifre yanlış.", Toast.LENGTH_SHORT).show()
            }
        }

        // Kayıt butonuna tıklanıldığında gerçekleşecek işlemler
        btnKullaniciKayit.setOnClickListener {
            val intent = Intent(this, KullaniciKayitActivity::class.java)
            startActivity(intent) // Yeni aktiviteye geçiş
        }
    }

    // Kullanıcının bilgilerini kontrol etme işlemi
    private fun checkUser(username: String, password: String): Boolean {
        val dbHelper = DatabaseHelper(this)
        return dbHelper.checkUser(username, password)
    }
}
