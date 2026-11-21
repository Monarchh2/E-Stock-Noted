package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.burak.myapplication.databinding.ActivityStokBinding

class StokActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokBinding
    private lateinit var dataBase: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, R.string.msg_admin_only, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityStokBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataBase = DatabaseHelper(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            UrunModel.DEFAULT_CATEGORIES
        )
        binding.actvKategori.setAdapter(categoryAdapter)
        binding.actvKategori.setText(UrunModel.DEFAULT_CATEGORY, false)
        binding.etMinStok.setText(UrunModel.DEFAULT_MIN_STOCK.toString())

        binding.btnKaydet.setOnClickListener {
            urunKontrolVeKaydet()
        }

        binding.btnListele.setOnClickListener {
            val intent = Intent(this, StokBilgiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun urunKontrolVeKaydet() {
        val result = ValidationUtils.validateProductInput(
            adRaw = binding.etUrunAd.text?.toString().orEmpty(),
            kodRaw = binding.etUrunKod.text?.toString().orEmpty(),
            kategoriRaw = binding.actvKategori.text?.toString().orEmpty(),
            adetRaw = binding.etUrunAdet.text?.toString().orEmpty(),
            minStokRaw = binding.etMinStok.text?.toString().orEmpty(),
            birimFiyatRaw = binding.etBirimFiyat.text?.toString().orEmpty(),
            aciklamaRaw = binding.etAciklama.text?.toString().orEmpty()
        )

        binding.tilUrunAd.error = errorMessage(result.adError)
        binding.tilUrunKod.error = errorMessage(result.kodError)
        binding.tilUrunAdet.error = errorMessage(result.adetError)
        binding.tilMinStok.error = errorMessage(result.minStokError)
        binding.tilBirimFiyat.error = errorMessage(result.birimFiyatError)

        if (!result.isValid) return

        if (dataBase.urunKoduMevcutMu(result.kod)) {
            binding.tilUrunKod.error = getString(R.string.msg_product_exists)
            return
        }

        val insertedId = dataBase.urunKaydet(
            ad = result.ad,
            kod = result.kod,
            kategori = result.kategori,
            adet = result.adet,
            minStok = result.minStok,
            birimFiyat = result.birimFiyat,
            aciklama = result.aciklama,
            yapanKullanici = sessionManager.getUsername(),
            hareketNotu = getString(R.string.note_initial_stock)
        )

        if (insertedId > 0) {
            Toast.makeText(this, R.string.msg_product_added, Toast.LENGTH_SHORT).show()
            temizle()
        } else {
            Toast.makeText(this, R.string.msg_product_add_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun temizle() {
        binding.etUrunAd.setText("")
        binding.etUrunKod.setText("")
        binding.actvKategori.setText(UrunModel.DEFAULT_CATEGORY, false)
        binding.etUrunAdet.setText("")
        binding.etMinStok.setText(UrunModel.DEFAULT_MIN_STOCK.toString())
        binding.etBirimFiyat.setText("")
        binding.etAciklama.setText("")
        binding.etUrunAd.requestFocus()
    }

    private fun errorMessage(error: ValidationError?): String? = when (error) {
        ValidationError.REQUIRED -> getString(R.string.err_required_field)
        ValidationError.INVALID_QUANTITY -> getString(R.string.err_invalid_quantity)
        ValidationError.INVALID_MIN_STOCK -> getString(R.string.err_invalid_min_stock)
        ValidationError.INVALID_PRICE -> getString(R.string.err_invalid_price)
        else -> null
    }
}