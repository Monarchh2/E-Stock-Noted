package com.burak.myapplication

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ActivityStokBilgiBinding

class StokBilgiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokBilgiBinding
    private lateinit var database: DatabaseHelper

    private lateinit var stklist: RecyclerView
    private lateinit var urunList: ArrayList<UrunModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStokBilgiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseHelper(this)
        urunList = ArrayList<UrunModel>()
        stklist = binding.stklist
        stklist.layoutManager = LinearLayoutManager(this)
        showAdapter()
    }

    private fun showAdapter() {
        urunList = database.urunGetir()
        val adapter = StokListAdapter(urunList)
        stklist.adapter = adapter

        adapter.onItemClick = { secilenUrun ->
            // Öğe tıklamasını işleyin, örneğin güncelleme/silme için bir iletişim kutusu gösterin
            showUpdateDeleteDialog(secilenUrun)
        }
    }

    private fun showUpdateDeleteDialog(secilenUrun: UrunModel) {
        val alertDialogBuilder = AlertDialog.Builder(this)


        // Özel bir düzen içinde içerik görüntüleme
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_update_delete, null)
        alertDialogBuilder.setView(dialogLayout)

        val urunKodEditText = dialogLayout.findViewById<EditText>(R.id.urunKodEditText)
        val urunAdEditText = dialogLayout.findViewById<EditText>(R.id.urunAdEditText)
        val urunAdetEditText = dialogLayout.findViewById<EditText>(R.id.urunAdetEditText)
        val urunAciklamaEditText = dialogLayout.findViewById<EditText>(R.id.urunAciklamaEditText)

        // Varolan veriyi EditText'lere yerleştir
        urunKodEditText.setText(secilenUrun.kod.toString())
        urunAdEditText.setText(secilenUrun.ad)
        urunAdetEditText.setText(secilenUrun.adet.toString())
        urunAciklamaEditText.setText(secilenUrun.aciklama)

        // Güncelleme ve Silme düğmelerini ekleyin
        alertDialogBuilder.setPositiveButton("Güncelle") { dialog, which ->
            val yeniUrunKod = urunKodEditText.text.toString().toIntOrNull() ?: 0
            val yeniUrunAd = urunAdEditText.text.toString()
            val yeniUrunAdet = urunAdetEditText.text.toString().toIntOrNull() ?: 0
            val yeniUrunAciklama = urunAciklamaEditText.text.toString()

            database.updateUrun(secilenUrun, yeniUrunKod, yeniUrunAd, yeniUrunAdet, yeniUrunAciklama)
            showAdapter()
        }

        alertDialogBuilder.setNegativeButton("Sil") { dialog, which ->

            database.deleteUrun(secilenUrun)
            showAdapter()
        }

        // İptal düğmesini ekleyin
        alertDialogBuilder.setNeutralButton("İptal") { dialog, which ->
            dialog.cancel()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}
