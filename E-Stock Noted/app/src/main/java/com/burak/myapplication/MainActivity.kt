package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.burak.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, StokBilgiActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKullaniciGiris.setOnClickListener {
            startActivity(Intent(this, KullaniciGirisActivity::class.java))
        }

        binding.btnYetkiliGiris.setOnClickListener {
            startActivity(Intent(this, YetkiliGirisActivity::class.java))
        }
    }
}