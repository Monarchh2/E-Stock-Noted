package com.burak.myapplication

import android.R
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.burak.myapplication.databinding.ActivityStokBinding



class StokActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokBinding

    private lateinit var dataBase: DatabaseHelper

    private lateinit var kaydet: Button
    private lateinit var listele: Button
    private lateinit var urunAd: EditText
    private lateinit var urunKod: EditText
    private lateinit var urunAdet: EditText
    private lateinit var urunAciklama: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStokBinding.inflate(layoutInflater)
        setContentView(binding.root)

        kaydet = binding.kaydet
        listele = binding.listele
        urunAd = binding.urunAd
        urunKod = binding.urunKod
        urunAdet = binding.urunAdet
        urunAciklama = binding.urunAciklama

        dataBase = DatabaseHelper(this)


        kaydet.setOnClickListener {
            urunKontrolVeKaydet()
        }

        listele.setOnClickListener {
            val intent = Intent(this, StokBilgiActivity::class.java)
            startActivity(intent)
        }


    }


    private fun urunKontrolVeKaydet() {
        val urunAdi = urunAd.text.toString()
        val urunKodu = urunKod.text.toString()
        val urunAdeti = urunAdet.text.toString().toInt()
        val urunAciklama = urunAciklama.text.toString()

        if (dataBase.urunKontrol(urunAdi, urunKodu)) {
            mesajGoster("Ürün Bulunmakta.", Toast.LENGTH_SHORT)
        } else {
            mesajGoster("Ürün Eklendi.", Toast.LENGTH_SHORT)
            temizle()
            dataBase.urunKaydet(urunAdi, urunKodu, urunAdeti, urunAciklama)
        }
    }


    private fun temizle() {
        urunAd.setText("")
        urunKod.setText("")
        urunAdet.setText("")
        urunAciklama.setText("")
    }


    private fun mesajGoster(mesaj: String, sure: Int) {
        val toast = Toast.makeText(this, mesaj, sure)
        toast.show()
    }
}