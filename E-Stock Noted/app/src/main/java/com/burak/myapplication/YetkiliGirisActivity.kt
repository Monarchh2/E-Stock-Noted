
package com.burak.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class YetkiliGirisActivity : AppCompatActivity() {
    lateinit var kullaniciAdi: EditText // Kullanıcı adı giriş alanı
    lateinit var sifre: EditText // Şifre giriş alanı
    lateinit var girisButton: Button // Giriş butonu

    // Bu örnek için sabit kullanıcı adı ve şifre
    val dogruKullaniciAdi = "admin"
    val dogruSifre = "12345"

    // Aktivitenin oluşturulma aşaması
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yetkili_giris) // XML layout dosyasının belirlenmesi

        // XML layout dosyasındaki elemanların tanımlanması
        kullaniciAdi = findViewById(R.id.YoneticiEmailAddress) // Kullanıcı adı giriş alanı
        sifre = findViewById(R.id.YoneticiSifre) // Şifre giriş alanı
        girisButton = findViewById(R.id.YoneticiGirisButon) // Giriş butonu

        // Giriş butonuna tıklanıldığında gerçekleşecek işlemler
        girisButton.setOnClickListener {
            girisKontrol()
        }
    }

    // Kullanıcının giriş bilgilerini kontrol etme işlemi
    private fun girisKontrol() {
        val girilenKullaniciAdi = kullaniciAdi.text.toString() // Kullanıcı adı
        val girilenSifre = sifre.text.toString() // Şifre

        // Kullanıcı adı ve şifre kontrolü
        if (girilenKullaniciAdi == dogruKullaniciAdi && girilenSifre == dogruSifre) {
            // Giriş başarılıysa mesaj göster, yeni aktiviteye geçiş yap ve bu aktiviteyi kapat
            Toast.makeText(this, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StokActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Kullanıcı adı veya şifre yanlışsa hata mesajı göster
            Toast.makeText(this, "Yanlış kullanıcı adı veya şifre!", Toast.LENGTH_SHORT).show()
        }
    }
}