package com.burak.myapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ActivityAdminYonetimBinding
import com.burak.myapplication.databinding.DialogAddStaffBinding
import com.burak.myapplication.databinding.ItemSimpleCardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class AdminYonetimActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminYonetimBinding
    private lateinit var database: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var staffAdapter: StaffAdapter
    private val auditAdapter = AuditLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityWindowHelper.applySecureFlag(this)

        sessionManager = SessionManager(this)
        database = DatabaseHelper(this)
        try {
            AccessGuard.requireAdmin(sessionManager.getRole())
        } catch (_: SecurityException) {
            database.guvenlikKaydiEkle(
                "YETKISIZ_ERISIM",
                sessionManager.getUsername(),
                "Yönetim paneline yetkisiz erişim engellendi",
                false
            )
            Toast.makeText(this, R.string.msg_admin_only, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!sessionManager.isLoggedIn() || sessionManager.isSessionExpired()) {
            sessionManager.clearSession()
            finish()
            return
        }
        sessionManager.touchActivity()

        binding = ActivityAdminYonetimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        staffAdapter = StaffAdapter { user ->
            toggleStaffStatus(user)
        }
        binding.rvStaff.layoutManager = LinearLayoutManager(this)
        binding.rvStaff.adapter = staffAdapter

        binding.rvAuditLog.layoutManager = LinearLayoutManager(this)
        binding.rvAuditLog.adapter = auditAdapter

        binding.btnAddStaff.setOnClickListener { showAddStaffDialog() }
        binding.btnExportJsonBackup.setOnClickListener { exportJsonBackup() }

        binding.chipGroupAdminTab.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipTabAudit -> {
                    binding.layoutStaffTab.visibility = View.GONE
                    binding.layoutAuditTab.visibility = View.VISIBLE
                    binding.layoutBackupTab.visibility = View.GONE
                    refreshAuditLog()
                }
                R.id.chipTabBackup -> {
                    binding.layoutStaffTab.visibility = View.GONE
                    binding.layoutAuditTab.visibility = View.GONE
                    binding.layoutBackupTab.visibility = View.VISIBLE
                    refreshBackupSummary()
                }
                else -> {
                    binding.layoutStaffTab.visibility = View.VISIBLE
                    binding.layoutAuditTab.visibility = View.GONE
                    binding.layoutBackupTab.visibility = View.GONE
                    refreshStaff()
                }
            }
        }

        refreshStaff()
    }

    private fun refreshStaff() {
        staffAdapter.submitList(database.kullanicilariGetir())
    }

    private fun refreshAuditLog() {
        val logs = database.guvenlikKayitlariniGetir()
        auditAdapter.submitList(logs)
        binding.tvEmptyAudit.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshBackupSummary() {
        val productCount = database.urunGetir().size
        val supplierCount = database.tedarikcileriGetir().size
        val movementCount = database.hareketleriGetir(limit = 1000).size
        binding.tvBackupSummary.text = getString(
            R.string.label_backup_summary_format,
            productCount,
            supplierCount,
            movementCount
        )
    }

    private fun toggleStaffStatus(user: UserModel) {
        if (user.username.equals(sessionManager.getUsername(), ignoreCase = true)) {
            Toast.makeText(this, R.string.msg_cannot_deactivate_self, Toast.LENGTH_SHORT).show()
            return
        }
        val nextActive = !user.isActive
        database.kullaniciDurumGuncelle(user.username, nextActive)
        database.guvenlikKaydiEkle(
            "HESAP_DURUM",
            sessionManager.getUsername(),
            "${user.username} -> ${if (nextActive) "AKTIF" else "PASIF"}",
            true
        )
        Toast.makeText(this, R.string.msg_staff_status_updated, Toast.LENGTH_SHORT).show()
        refreshStaff()
    }

    private fun showAddStaffDialog() {
        val dlgBinding = DialogAddStaffBinding.inflate(layoutInflater)
        val roleLabels = listOf(
            getString(R.string.role_admin),
            getString(R.string.role_depo),
            getString(R.string.role_user_readonly)
        )
        val roleValues = listOf(UserRole.ADMIN, UserRole.DEPO, UserRole.USER)
        dlgBinding.actvStaffRole.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roleLabels)
        )
        dlgBinding.actvStaffRole.setText(roleLabels[1], false)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_staff_title)
            .setView(dlgBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val res = ValidationUtils.validateRegistration(
                    usernameRaw = dlgBinding.etStaffUsername.text?.toString().orEmpty(),
                    emailRaw = dlgBinding.etStaffEmail.text?.toString().orEmpty(),
                    businessRaw = dlgBinding.etStaffBusiness.text?.toString().orEmpty(),
                    passwordRaw = dlgBinding.etStaffPassword.text?.toString().orEmpty(),
                    confirmPasswordRaw = dlgBinding.etStaffPasswordConfirm.text?.toString().orEmpty()
                )
                dlgBinding.tilStaffUsername.error = errText(res.usernameError)
                dlgBinding.tilStaffEmail.error = errText(res.emailError)
                dlgBinding.tilStaffBusiness.error = errText(res.businessError)
                dlgBinding.tilStaffPassword.error = errText(res.passwordError)
                dlgBinding.tilStaffPasswordConfirm.error = errText(res.confirmPasswordError)

                if (!res.isValid) return@setOnClickListener
                if (database.userExists(res.username)) {
                    dlgBinding.tilStaffUsername.error = getString(R.string.msg_register_duplicate)
                    return@setOnClickListener
                }

                val selectedIdx = roleLabels.indexOf(dlgBinding.actvStaffRole.text?.toString()).coerceAtLeast(0)
                val selectedRole = roleValues[selectedIdx]

                val id = database.addUser(
                    username = res.username,
                    password = res.password,
                    email = res.email,
                    business = res.business,
                    role = selectedRole
                )
                if (id > 0) {
                    database.guvenlikKaydiEkle(
                        "PERSONEL_EKLE",
                        sessionManager.getUsername(),
                        "${res.username} (${selectedRole.name}) oluşturuldu",
                        true
                    )
                    Toast.makeText(this, R.string.msg_staff_added, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    refreshStaff()
                }
            }
        }
        dialog.show()
    }

    private fun exportJsonBackup() {
        try {
            val products = database.urunGetir()
            val suppliers = database.tedarikcileriGetir()
            val json = BackupJsonCodec.encode(products, suppliers)

            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val backupFile = File(exportDir, "e_stock_noted_yedek.json")
            backupFile.writeText(json, Charsets.UTF_8)

            database.guvenlikKaydiEkle(
                "YEDEKLEME",
                sessionManager.getUsername(),
                "${products.size} ürün, ${suppliers.size} tedarikçi yedeklendi",
                true
            )

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                backupFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_backup_chooser)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.msg_backup_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun errText(error: ValidationError?): String? = when (error) {
        ValidationError.REQUIRED -> getString(R.string.err_required_field)
        ValidationError.INVALID_EMAIL -> getString(R.string.err_invalid_email)
        ValidationError.PASSWORD_TOO_WEAK -> getString(R.string.err_password_weak)
        ValidationError.PASSWORD_MISMATCH -> getString(R.string.err_password_mismatch)
        else -> null
    }

    private class StaffAdapter(
        private val onToggleActive: (UserModel) -> Unit
    ) : ListAdapter<UserModel, StaffAdapter.VH>(DIFF) {

        class VH(val binding: ItemSimpleCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = getItem(position)
            val ctx = holder.itemView.context
            with(holder.binding) {
                tvCardTitle.text = ctx.getString(R.string.label_user_role_format, u.username, u.role.name)
                tvCardBadge.text = ctx.getString(
                    if (u.isActive) R.string.label_user_status_active else R.string.label_user_status_inactive
                )
                val (fg, bg) = if (u.isActive) {
                    R.color.status_ok to R.color.status_ok_bg
                } else {
                    R.color.status_out to R.color.status_out_bg
                }
                tvCardBadge.setTextColor(ContextCompat.getColor(ctx, fg))
                tvCardBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
                tvCardSubtitle.text = ctx.getString(R.string.subtitle_session_format, u.business, u.email)
                tvCardMeta.visibility = View.GONE
                root.setOnClickListener { onToggleActive(u) }
            }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<UserModel>() {
                override fun areItemsTheSame(a: UserModel, b: UserModel) = a.username == b.username
                override fun areContentsTheSame(a: UserModel, b: UserModel) = a == b
            }
        }
    }

    private class AuditLogAdapter :
        ListAdapter<SecurityAuditLogModel, AuditLogAdapter.VH>(DIFF) {

        class VH(val binding: ItemSimpleCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val log = getItem(position)
            val ctx = holder.itemView.context
            with(holder.binding) {
                tvCardTitle.text = ctx.getString(R.string.subtitle_session_format, log.olayTipi, log.kullaniciAdi)
                tvCardBadge.text = ctx.getString(
                    if (log.basarili) R.string.badge_audit_ok else R.string.badge_audit_warn
                )
                val (fg, bg) = if (log.basarili) {
                    R.color.status_ok to R.color.status_ok_bg
                } else {
                    R.color.status_out to R.color.status_out_bg
                }
                tvCardBadge.setTextColor(ContextCompat.getColor(ctx, fg))
                tvCardBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
                tvCardSubtitle.text = log.detay
                tvCardMeta.text = StockAnalytics.formatDateTime(log.tarih)
                tvCardMeta.visibility = View.VISIBLE
            }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<SecurityAuditLogModel>() {
                override fun areItemsTheSame(a: SecurityAuditLogModel, b: SecurityAuditLogModel) = a.id == b.id
                override fun areContentsTheSame(a: SecurityAuditLogModel, b: SecurityAuditLogModel) = a == b
            }
        }
    }
}
