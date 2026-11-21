package com.burak.myapplication

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ActivityTedarikciBinding
import com.burak.myapplication.databinding.DialogAddSupplierBinding
import com.burak.myapplication.databinding.ItemSimpleCardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TedarikciActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTedarikciBinding
    private lateinit var database: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SupplierAdapter
    private var canEdit: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityWindowHelper.applySecureFlag(this)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn() || sessionManager.isSessionExpired()) {
            sessionManager.clearSession()
            finish()
            return
        }
        sessionManager.touchActivity()

        binding = ActivityTedarikciBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseHelper(this)
        canEdit = sessionManager.canManageStock()

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SupplierAdapter { supplier ->
            if (canEdit) {
                confirmDeleteSupplier(supplier)
            }
        }
        binding.rvSuppliers.layoutManager = LinearLayoutManager(this)
        binding.rvSuppliers.adapter = adapter

        binding.fabAddSupplier.visibility = if (canEdit) View.VISIBLE else View.GONE
        binding.fabAddSupplier.setOnClickListener {
            showAddSupplierDialog()
        }

        refreshSuppliers()
    }

    private fun refreshSuppliers() {
        val list = database.tedarikcileriGetir()
        adapter.submitList(list)
        if (list.isEmpty()) {
            binding.layoutEmptySuppliers.visibility = View.VISIBLE
            binding.rvSuppliers.visibility = View.GONE
        } else {
            binding.layoutEmptySuppliers.visibility = View.GONE
            binding.rvSuppliers.visibility = View.VISIBLE
        }
    }

    private fun showAddSupplierDialog() {
        val dlgBinding = DialogAddSupplierBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_supplier_title)
            .setView(dlgBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val res = ValidationUtils.validateSupplier(
                    firmaAdiRaw = dlgBinding.etSupName.text?.toString().orEmpty(),
                    yetkiliRaw = dlgBinding.etSupContact.text?.toString().orEmpty(),
                    telefonRaw = dlgBinding.etSupPhone.text?.toString().orEmpty(),
                    epostaRaw = dlgBinding.etSupEmail.text?.toString().orEmpty(),
                    vergiNoRaw = dlgBinding.etSupTax.text?.toString().orEmpty(),
                    notlarRaw = dlgBinding.etSupNotes.text?.toString().orEmpty()
                )
                dlgBinding.tilSupName.error =
                    if (res.firmaAdiError != null) getString(R.string.err_required_field) else null
                dlgBinding.tilSupEmail.error =
                    if (res.epostaError != null) getString(R.string.err_invalid_email) else null

                if (!res.isValid) return@setOnClickListener

                val id = database.tedarikciEkle(
                    res.firmaAdi,
                    res.yetkiliKisi,
                    res.telefon,
                    res.eposta,
                    res.vergiNo,
                    res.notlar
                )
                if (id > 0) {
                    Toast.makeText(this, R.string.msg_supplier_added, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    refreshSuppliers()
                } else {
                    dlgBinding.tilSupName.error = getString(R.string.msg_supplier_exists)
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteSupplier(supplier: TedarikciModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(supplier.firmaAdi)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                database.tedarikciSil(supplier.id)
                Toast.makeText(this, R.string.msg_supplier_deleted, Toast.LENGTH_SHORT).show()
                refreshSuppliers()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private class SupplierAdapter(
        private val onClick: (TedarikciModel) -> Unit
    ) : ListAdapter<TedarikciModel, SupplierAdapter.VH>(DIFF) {

        class VH(val binding: ItemSimpleCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = getItem(position)
            val ctx = holder.itemView.context
            with(holder.binding) {
                tvCardTitle.text = s.firmaAdi
                tvCardBadge.text = s.vergiNo.ifBlank { "Cari" }
                tvCardBadge.setTextColor(ContextCompat.getColor(ctx, R.color.brand_navy))
                tvCardBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.brand_teal_soft))

                val contactParts = listOf(s.yetkiliKisi, s.telefon, s.eposta).filter { it.isNotBlank() }
                tvCardSubtitle.text = contactParts.joinToString(" • ").ifBlank { "-" }
                if (s.notlar.isNotBlank()) {
                    tvCardMeta.text = s.notlar
                    tvCardMeta.visibility = View.VISIBLE
                } else {
                    tvCardMeta.visibility = View.GONE
                }
                root.setOnClickListener { onClick(s) }
            }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<TedarikciModel>() {
                override fun areItemsTheSame(a: TedarikciModel, b: TedarikciModel) = a.id == b.id
                override fun areContentsTheSame(a: TedarikciModel, b: TedarikciModel) = a == b
            }
        }
    }
}
