package com.burak.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.burak.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // Kullanıcı Giriş butonuna tıklandığında çalışacak metod
    fun onKulGirisClick(view: View) {
        val intent = Intent(this, KullaniciGirisActivity::class.java)
        startActivity(intent)
    }

    // Kullanıcı Kayıt butonuna tıklandığında çalışacak metod
    fun onYetGirisClick(view: View) {
        val intent = Intent(this, YetkiliGirisActivity::class.java)
        startActivity(intent)
    }
}