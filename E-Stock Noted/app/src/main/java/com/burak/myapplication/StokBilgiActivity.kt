package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.burak.myapplication.databinding.ActivityStokBilgiBinding
import com.burak.myapplication.databinding.DialogChangePasswordBinding
import com.burak.myapplication.databinding.DialogStockAdjustBinding
import com.burak.myapplication.databinding.DialogUpdateDeleteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class StokBilgiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStokBilgiBinding
    private lateinit var database: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: StokListAdapter

    private var allItems: List<UrunModel> = emptyList()
    private var currentQuery: String = ""
    private var currentFilter: StokFiltre = StokFiltre.TUMU
    private var currentCategory: String? = null
    private var currentSort: StokSiralama = StokSiralama.SON_GUNCELLEME
    private var canManageStock: Boolean = false
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityWindowHelper.applySecureFlag(this)

        sessionManager = SessionManager(this)
        database = DatabaseHelper(this)
        val activeUser = SessionGuard.verifyActiveSession(sessionManager, database)
        if (activeUser == null) {
            redirectToMain()
            return
        }

        binding = ActivityStokBilgiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        canManageStock = activeUser.role.canManageStock
        isAdmin = activeUser.role.canManageUsers

        setupToolbar()
        setupRecyclerView()
        setupFiltersAndSearch()
        setupFab()

        if (activeUser.mustChangePassword) {
            Toast.makeText(this, R.string.msg_must_change_password, Toast.LENGTH_LONG).show()
            showChangePasswordDialog(mandatory = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::sessionManager.isInitialized || !::database.isInitialized) return
        val activeUser = SessionGuard.verifyActiveSession(sessionManager, database)
        if (activeUser == null) {
            Toast.makeText(this, R.string.msg_session_expired, Toast.LENGTH_LONG).show()
            redirectToMain()
            return
        }
        val roleChanged =
            canManageStock != activeUser.role.canManageStock || isAdmin != activeUser.role.canManageUsers
        if (roleChanged) {
            recreate()
            return
        }
        refreshData()
    }

    private fun setupToolbar() {
        val roleLabel = when (sessionManager.getRole()) {
            UserRole.ADMIN -> getString(R.string.role_admin)
            UserRole.DEPO -> getString(R.string.role_depo)
            UserRole.USER -> getString(R.string.role_user_readonly)
        }
        binding.toolbar.subtitle = getString(
            R.string.subtitle_session_format,
            sessionManager.getUsername(),
            roleLabel
        )

        binding.toolbar.menu.findItem(R.id.action_admin_panel)?.isVisible = isAdmin

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            sessionManager.touchActivity()
            when (menuItem.itemId) {
                R.id.action_reports -> {
                    startActivity(Intent(this, RaporlarActivity::class.java))
                    true
                }
                R.id.action_history -> {
                    startActivity(Intent(this, StokHareketActivity::class.java))
                    true
                }
                R.id.action_suppliers -> {
                    startActivity(Intent(this, TedarikciActivity::class.java))
                    true
                }
                R.id.action_export_pdf -> {
                    exportPdfReport()
                    true
                }
                R.id.action_export_csv -> {
                    exportCsvReport()
                    true
                }
                R.id.action_admin_panel -> {
                    startActivity(Intent(this, AdminYonetimActivity::class.java))
                    true
                }
                R.id.action_change_password -> {
                    showChangePasswordDialog()
                    true
                }
                R.id.action_logout -> {
                    confirmLogout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StokListAdapter(
            canManageStock = canManageStock,
            onItemClick = { urun ->
                sessionManager.touchActivity()
                if (canManageStock) {
                    showUpdateDeleteDialog(urun)
                } else {
                    showReadOnlyDetailDialog(urun)
                }
            },
            onAdjustQuantity = { urun, delta ->
                sessionManager.touchActivity()
                if (canManageStock) {
                    adjustStockQuantity(urun, delta)
                }
            },
            onBulkAdjust = { urun ->
                sessionManager.touchActivity()
                if (canManageStock) {
                    showBulkStockAdjustDialog(urun)
                }
            }
        )
        binding.stklist.layoutManager = LinearLayoutManager(this)
        binding.stklist.adapter = adapter
    }

    private fun setupFiltersAndSearch() {
        binding.etSearch.doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty()
            applyFilterAndSort()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipLow -> StokFiltre.KRITIK
                R.id.chipOut -> StokFiltre.TUKENEN
                else -> StokFiltre.TUMU
            }
            applyFilterAndSort()
        }

        binding.btnCategoryFilter.setOnClickListener { showCategoryFilterDialog() }
        binding.btnSort.setOnClickListener { showSortDialog() }
    }

    private fun setupFab() {
        binding.fabAddProduct.visibility = if (canManageStock) View.VISIBLE else View.GONE
        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(this, StokActivity::class.java))
        }
    }

    private fun refreshData() {
        allItems = database.urunGetir()
        val categories = StockAnalytics.extractCategories(allItems)
        if (currentCategory != null && categories.none { it.equals(currentCategory, ignoreCase = true) }) {
            currentCategory = null
            binding.btnCategoryFilter.setText(R.string.btn_category_filter)
        }
        updateKpiSummary(allItems)
        applyFilterAndSort()
    }

    private fun updateKpiSummary(items: List<UrunModel>) {
        val summary = StockAnalytics.computeSummary(items)
        binding.tvKpiProducts.text = summary.toplamUrunCesidi.toString()
        binding.tvKpiUnits.text = summary.toplamAdet.toString()
        binding.tvKpiCritical.text = summary.toplamUyariSayisi.toString()
        binding.tvKpiValue.text = StockAnalytics.formatCurrency(summary.toplamDeger)

        val criticalColor = when {
            summary.tukenenStokSayisi > 0 -> R.color.status_out
            summary.kritikStokSayisi > 0 -> R.color.status_low
            else -> R.color.text_on_navy
        }
        binding.tvKpiCritical.setTextColor(ContextCompat.getColor(this, criticalColor))
    }

    private fun applyFilterAndSort() {
        val visibleItems = StockAnalytics.filterAndSort(
            items = allItems,
            query = currentQuery,
            filtre = currentFilter,
            siralama = currentSort,
            kategori = currentCategory
        )
        adapter.submitList(visibleItems)

        if (visibleItems.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.stklist.visibility = View.GONE
            if (allItems.isEmpty()) {
                binding.tvEmptyTitle.setText(R.string.empty_stock_title)
                binding.tvEmptyDesc.setText(
                    if (canManageStock) R.string.empty_stock_desc_admin else R.string.empty_stock_desc_user
                )
            } else {
                binding.tvEmptyTitle.setText(R.string.empty_search_title)
                binding.tvEmptyDesc.setText(R.string.empty_search_desc)
            }
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.stklist.visibility = View.VISIBLE
        }
    }

    private fun adjustStockQuantity(urun: UrunModel, delta: Int) {
        if (SecuritySanitizer.safeAddQuantity(urun.adet, delta) == null) {
            Toast.makeText(this, R.string.msg_stock_cannot_be_negative, Toast.LENGTH_SHORT).show()
            return
        }
        val note = getString(if (delta > 0) R.string.note_quick_increase else R.string.note_quick_decrease)
        val success = database.stokMiktarGuncelle(
            urun = urun,
            delta = delta,
            not = note,
            yapanKullanici = sessionManager.getUsername()
        )
        if (success) {
            val msgRes = if (delta > 0) R.string.msg_stock_adjusted_in else R.string.msg_stock_adjusted_out
            Toast.makeText(this, getString(msgRes, urun.ad), Toast.LENGTH_SHORT).show()
            refreshData()
        }
    }

    private fun showBulkStockAdjustDialog(urun: UrunModel) {
        val dialogBinding = DialogStockAdjustBinding.inflate(layoutInflater)
        dialogBinding.tvAdjustProductTitle.text =
            getString(R.string.subtitle_session_format, urun.ad, urun.kod)

        fun updatePreview() {
            val isIncoming = dialogBinding.chipAdjustIn.isChecked
            val amount = ValidationUtils.parsePositiveInt(
                dialogBinding.etAdjustAmount.text?.toString().orEmpty()
            ) ?: 0
            val delta = if (isIncoming) amount else -amount
            val newQty = SecuritySanitizer.safeAddQuantity(urun.adet, delta)
                ?: if (isIncoming) Int.MAX_VALUE else 0
            dialogBinding.tvAdjustPreview.text =
                getString(R.string.label_adjust_preview_format, urun.adet, newQty)
        }

        updatePreview()
        dialogBinding.etAdjustAmount.doAfterTextChanged { updatePreview() }
        dialogBinding.chipGroupAdjustType.setOnCheckedStateChangeListener { _, _ -> updatePreview() }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_adjust_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_apply, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val isIncoming = dialogBinding.chipAdjustIn.isChecked
                val result = ValidationUtils.validateStockAdjustment(
                    currentQty = urun.adet,
                    amountRaw = dialogBinding.etAdjustAmount.text?.toString().orEmpty(),
                    isIncoming = isIncoming,
                    noteRaw = dialogBinding.etAdjustNote.text?.toString().orEmpty()
                )

                dialogBinding.tilAdjustAmount.error = errorMessage(result.amountError)
                if (!result.isValid) return@setOnClickListener

                val defaultNote = getString(
                    if (isIncoming) R.string.note_quick_increase else R.string.note_quick_decrease
                )
                val finalNote = result.note.ifEmpty { defaultNote }

                val success = database.stokMiktarGuncelle(
                    urun = urun,
                    delta = result.delta,
                    not = finalNote,
                    yapanKullanici = sessionManager.getUsername()
                )
                if (success) {
                    val deltaLabel = if (result.delta >= 0) {
                        getString(R.string.movement_delta_positive, result.delta)
                    } else {
                        getString(R.string.movement_delta_negative, result.delta)
                    }
                    Toast.makeText(
                        this,
                        getString(R.string.msg_bulk_stock_updated, urun.ad, deltaLabel),
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    refreshData()
                }
            }
        }

        dialog.show()
    }

    private fun showCategoryFilterDialog() {
        val categories = StockAnalytics.extractCategories(allItems)
        val options = arrayOf(getString(R.string.filter_all_categories)) + categories.toTypedArray()
        val checkedIndex = if (currentCategory == null) {
            0
        } else {
            val idx = categories.indexOfFirst { it.equals(currentCategory, ignoreCase = true) }
            if (idx >= 0) idx + 1 else 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_category_title)
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                if (which == 0) {
                    currentCategory = null
                    binding.btnCategoryFilter.setText(R.string.btn_category_filter)
                } else {
                    currentCategory = categories[which - 1]
                    binding.btnCategoryFilter.text = currentCategory
                }
                applyFilterAndSort()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.sort_recent),
            getString(R.string.sort_name_asc),
            getString(R.string.sort_qty_asc),
            getString(R.string.sort_qty_desc)
        )
        val sortModes = arrayOf(
            StokSiralama.SON_GUNCELLEME,
            StokSiralama.AD_ARTAN,
            StokSiralama.ADET_ARTAN,
            StokSiralama.ADET_AZALAN
        )
        val checkedIndex = sortModes.indexOf(currentSort).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_sort_title)
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                currentSort = sortModes[which]
                applyFilterAndSort()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showChangePasswordDialog(mandatory: Boolean = false) {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_change_password_title)
            .setView(dialogBinding.root)
            .setCancelable(!mandatory)
            .setPositiveButton(R.string.btn_update, null)

        if (mandatory) {
            builder.setNegativeButton(R.string.menu_logout) { _, _ ->
                sessionManager.clearSession()
                redirectToMain()
            }
        } else {
            builder.setNegativeButton(R.string.btn_cancel, null)
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(!mandatory)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = ValidationUtils.validatePasswordChange(
                    currentPasswordRaw = dialogBinding.etCurrentPassword.text?.toString().orEmpty(),
                    newPasswordRaw = dialogBinding.etNewPassword.text?.toString().orEmpty(),
                    confirmPasswordRaw = dialogBinding.etNewPasswordConfirm.text?.toString().orEmpty()
                )

                dialogBinding.tilCurrentPassword.error = errorMessage(result.currentPasswordError)
                dialogBinding.tilNewPassword.error = errorMessage(result.newPasswordError)
                dialogBinding.tilNewPasswordConfirm.error = errorMessage(result.confirmPasswordError)

                if (!result.isValid) return@setOnClickListener

                val updated = database.changePassword(
                    username = sessionManager.getUsername(),
                    currentPassword = result.currentPassword,
                    newPassword = result.newPassword
                )
                if (updated) {
                    sessionManager.markPasswordChanged()
                    database.guvenlikKaydiEkle("SIFRE_DEGISIM", sessionManager.getUsername(), "Şifre güncellendi", true)
                    Toast.makeText(this, R.string.msg_password_changed, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    database.guvenlikKaydiEkle("SIFRE_DEGISIM", sessionManager.getUsername(), "Mevcut şifre hatalı", false)
                    dialogBinding.tilCurrentPassword.error =
                        getString(R.string.msg_current_password_wrong)
                }
            }
        }

        dialog.show()
    }

    private fun showReadOnlyDetailDialog(urun: UrunModel) {
        val description = urun.aciklama.ifBlank { getString(R.string.label_no_description) }
        val supplier = urun.tedarikci.ifBlank { "-" }
        val message = getString(
            R.string.dialog_detail_commercial_format,
            urun.ad,
            urun.kod,
            urun.kategori,
            urun.depoKonum,
            supplier,
            urun.adet,
            urun.minStok,
            StockAnalytics.formatCurrency(urun.alisFiyati),
            StockAnalytics.formatCurrency(urun.birimFiyat),
            urun.kdvOrani,
            StockAnalytics.formatPercent(urun.karMarjiYuzde),
            StockAnalytics.formatCurrency(urun.toplamDeger),
            description
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_detail_title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_close, null)
            .show()
    }

    private fun showUpdateDeleteDialog(secilenUrun: UrunModel) {
        val dialogBinding = DialogUpdateDeleteBinding.inflate(layoutInflater)

        dialogBinding.actvEditKategori.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, UrunModel.DEFAULT_CATEGORIES)
        )
        val suppliers = database.tedarikcileriGetir().map { it.firmaAdi }
        dialogBinding.actvEditTedarikci.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suppliers)
        )

        dialogBinding.etEditAd.setText(secilenUrun.ad)
        dialogBinding.etEditKod.setText(secilenUrun.kod)
        dialogBinding.actvEditKategori.setText(secilenUrun.kategori, false)
        dialogBinding.etEditAdet.setText(secilenUrun.adet.toString())
        dialogBinding.etEditMinStok.setText(secilenUrun.minStok.toString())
        dialogBinding.etEditAlisFiyati.setText(
            if (secilenUrun.alisFiyati == 0.0) "" else secilenUrun.alisFiyati.toString()
        )
        dialogBinding.etEditBirimFiyat.setText(
            if (secilenUrun.birimFiyat == 0.0) "" else secilenUrun.birimFiyat.toString()
        )
        dialogBinding.etEditKdvOrani.setText(secilenUrun.kdvOrani.toString())
        dialogBinding.etEditDepoKonum.setText(secilenUrun.depoKonum)
        dialogBinding.actvEditTedarikci.setText(secilenUrun.tedarikci, false)
        dialogBinding.etEditAciklama.setText(secilenUrun.aciklama)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_edit_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_update, null)
            .setNegativeButton(R.string.btn_delete) { _, _ ->
                confirmDeleteProduct(secilenUrun)
            }
            .setNeutralButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = ValidationUtils.validateProductInput(
                    adRaw = dialogBinding.etEditAd.text?.toString().orEmpty(),
                    kodRaw = dialogBinding.etEditKod.text?.toString().orEmpty(),
                    kategoriRaw = dialogBinding.actvEditKategori.text?.toString().orEmpty(),
                    adetRaw = dialogBinding.etEditAdet.text?.toString().orEmpty(),
                    minStokRaw = dialogBinding.etEditMinStok.text?.toString().orEmpty(),
                    birimFiyatRaw = dialogBinding.etEditBirimFiyat.text?.toString().orEmpty(),
                    aciklamaRaw = dialogBinding.etEditAciklama.text?.toString().orEmpty(),
                    alisFiyatiRaw = dialogBinding.etEditAlisFiyati.text?.toString().orEmpty(),
                    kdvOraniRaw = dialogBinding.etEditKdvOrani.text?.toString().orEmpty(),
                    tedarikciRaw = dialogBinding.actvEditTedarikci.text?.toString().orEmpty(),
                    depoKonumRaw = dialogBinding.etEditDepoKonum.text?.toString().orEmpty()
                )

                dialogBinding.tilEditAd.error = errorMessage(result.adError)
                dialogBinding.tilEditKod.error = errorMessage(result.kodError)
                dialogBinding.tilEditAdet.error = errorMessage(result.adetError)
                dialogBinding.tilEditMinStok.error = errorMessage(result.minStokError)
                dialogBinding.tilEditAlisFiyati.error = errorMessage(result.alisFiyatiError)
                dialogBinding.tilEditBirimFiyat.error = errorMessage(result.birimFiyatError)
                dialogBinding.tilEditKdvOrani.error = errorMessage(result.kdvOraniError)

                if (!result.isValid) return@setOnClickListener

                if (database.urunKoduMevcutMu(result.kod, excludeId = secilenUrun.id)) {
                    dialogBinding.tilEditKod.error = getString(R.string.msg_product_exists)
                    return@setOnClickListener
                }

                database.updateUrun(
                    secilenUrun = secilenUrun,
                    yeniUrunKod = result.kod,
                    yeniUrunAd = result.ad,
                    yeniKategori = result.kategori,
                    yeniUrunAdet = result.adet,
                    yeniMinStok = result.minStok,
                    yeniBirimFiyat = result.birimFiyat,
                    yeniUrunAciklama = result.aciklama,
                    yapanKullanici = sessionManager.getUsername(),
                    hareketNotu = getString(R.string.note_manual_update),
                    yeniAlisFiyati = result.alisFiyati,
                    yeniKdvOrani = result.kdvOrani,
                    yeniTedarikci = result.tedarikci,
                    yeniDepoKonum = result.depoKonum
                )
                Toast.makeText(this, R.string.msg_product_updated, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                refreshData()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteProduct(urun: UrunModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_confirm_title)
            .setMessage(getString(R.string.dialog_delete_confirm_msg, urun.ad, urun.kod))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                database.deleteUrun(
                    secilenUrun = urun,
                    yapanKullanici = sessionManager.getUsername(),
                    hareketNotu = getString(R.string.note_product_deleted)
                )
                Toast.makeText(this, R.string.msg_product_deleted, Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun exportPdfReport() {
        if (allItems.isEmpty()) {
            Toast.makeText(this, R.string.msg_export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val pdfFile = File(exportDir, "e_stock_noted_rapor.pdf")
            PdfReportGenerator.generateInventoryReport(
                outputFile = pdfFile,
                items = allItems,
                generatedBy = sessionManager.getUsername()
            )
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pdf_subject))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_pdf_chooser)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.msg_pdf_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportCsvReport() {
        if (allItems.isEmpty()) {
            Toast.makeText(this, R.string.msg_export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(exportDir, "e_stock_noted_rapor.csv")
            csvFile.writeText(StokCsvExporter.toCsv(allItems), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                csvFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_csv_subject))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_csv_chooser)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_msg)
            .setPositiveButton(R.string.menu_logout) { _, _ ->
                database.guvenlikKaydiEkle("CIKIS", sessionManager.getUsername(), "Oturum kapatıldı", true)
                sessionManager.clearSession()
                redirectToMain()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun redirectToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun errorMessage(error: ValidationError?): String? = when (error) {
        ValidationError.REQUIRED -> getString(R.string.err_required_field)
        ValidationError.PASSWORD_TOO_WEAK -> getString(R.string.err_password_weak)
        ValidationError.PASSWORD_REUSED -> getString(R.string.err_password_reused)
        ValidationError.PASSWORD_MISMATCH -> getString(R.string.err_password_mismatch)
        ValidationError.INVALID_QUANTITY -> getString(R.string.err_invalid_quantity)
        ValidationError.QUANTITY_OVERFLOW -> getString(R.string.err_quantity_overflow)
        ValidationError.INSUFFICIENT_STOCK -> getString(R.string.err_insufficient_stock)
        ValidationError.INVALID_MIN_STOCK -> getString(R.string.err_invalid_min_stock)
        ValidationError.INVALID_PRICE -> getString(R.string.err_invalid_price)
        ValidationError.INVALID_VAT -> getString(R.string.err_invalid_vat)
        else -> null
    }
}
