package com.burak.myapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.burak.myapplication.databinding.ActivityStokHareketBinding

class StokHareketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokHareketBinding
    private lateinit var database: DatabaseHelper
    private val adapter = StokHareketAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStokHareketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = DatabaseHelper(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.rvMovements.layoutManager = LinearLayoutManager(this)
        binding.rvMovements.adapter = adapter

        val movements = database.hareketleriGetir()
        adapter.submitList(movements)

        if (movements.isEmpty()) {
            binding.layoutEmptyMovements.visibility = View.VISIBLE
            binding.rvMovements.visibility = View.GONE
        } else {
            binding.layoutEmptyMovements.visibility = View.GONE
            binding.rvMovements.visibility = View.VISIBLE
        }
    }
}
