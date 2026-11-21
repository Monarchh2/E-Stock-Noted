package com.burak.myapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.burak.myapplication.databinding.ActivityStokHareketBinding

class StokHareketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokHareketBinding
    private lateinit var database: DatabaseHelper
    private val adapter = StokHareketAdapter()

    private var allMovements: List<StokHareketModel> = emptyList()
    private var currentQuery: String = ""
    private var currentType: HareketTipi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityWindowHelper.applySecureFlag(this)
        binding = ActivityStokHareketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = DatabaseHelper(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.rvMovements.layoutManager = LinearLayoutManager(this)
        binding.rvMovements.adapter = adapter

        binding.etSearchMovements.doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty()
            applyFilter()
        }

        binding.chipGroupMoveType.setOnCheckedStateChangeListener { _, checkedIds ->
            currentType = when (checkedIds.firstOrNull()) {
                R.id.chipMoveIn -> HareketTipi.GIRIS
                R.id.chipMoveOut -> HareketTipi.CIKIS
                R.id.chipMoveUpdate -> HareketTipi.GUNCELLEME
                R.id.chipMoveDelete -> HareketTipi.SILME
                else -> null
            }
            applyFilter()
        }

        allMovements = database.hareketleriGetir()
        applyFilter()
    }

    private fun applyFilter() {
        val visible = StockAnalytics.filterMovements(
            movements = allMovements,
            query = currentQuery,
            tip = currentType
        )
        adapter.submitList(visible)

        if (visible.isEmpty()) {
            binding.layoutEmptyMovements.visibility = View.VISIBLE
            binding.rvMovements.visibility = View.GONE
            if (allMovements.isEmpty()) {
                binding.tvEmptyMoveTitle.setText(R.string.empty_movements_title)
                binding.tvEmptyMoveDesc.setText(R.string.empty_movements_desc)
            } else {
                binding.tvEmptyMoveTitle.setText(R.string.empty_search_title)
                binding.tvEmptyMoveDesc.setText(R.string.empty_search_desc)
            }
        } else {
            binding.layoutEmptyMovements.visibility = View.GONE
            binding.rvMovements.visibility = View.VISIBLE
        }
    }
}
