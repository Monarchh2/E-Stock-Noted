package com.burak.myapplication

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ItemStokBinding

class StokListAdapter(
    private val canManageStock: Boolean,
    private val onItemClick: (UrunModel) -> Unit,
    private val onAdjustQuantity: (UrunModel, Int) -> Unit,
    private val onBulkAdjust: (UrunModel) -> Unit
) : ListAdapter<UrunModel, StokListAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemStokBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStokBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        with(holder.binding) {
            tvUrunAd.text = item.ad
            tvUrunKod.text = context.getString(R.string.label_sku_format, item.kod)
            tvKategori.text = context.getString(
                R.string.subtitle_session_format,
                item.kategori,
                item.depoKonum
            )
            tvAdet.text = context.resources.getQuantityString(
                R.plurals.label_qty_format,
                item.adet,
                item.adet
            )
            tvMinStok.text = context.getString(R.string.label_min_stock_format, item.minStok)

            val unitPrice = StockAnalytics.formatCurrency(item.birimFiyat)
            val totalValue = StockAnalytics.formatCurrency(item.toplamDeger)
            val unitLabel = context.getString(R.string.label_unit_price_format, unitPrice)
            val totalLabel = context.getString(R.string.label_total_value_format, totalValue)
            tvFiyatBilgi.text = if (item.alisFiyati > 0.0 && item.birimFiyat > 0.0) {
                val marginLabel = context.getString(
                    R.string.label_margin_format,
                    StockAnalytics.formatPercent(item.karMarjiYuzde)
                )
                context.getString(R.string.label_price_with_margin_format, unitLabel, totalLabel, marginLabel)
            } else {
                context.getString(R.string.subtitle_session_format, unitLabel, totalLabel)
            }

            if (item.aciklama.isNotBlank()) {
                tvAciklama.text = item.aciklama
                tvAciklama.visibility = View.VISIBLE
            } else {
                tvAciklama.visibility = View.GONE
            }

            val (statusTextRes, statusColorRes, statusBgRes) = when {
                item.isOutOfStock -> Triple(R.string.status_out, R.color.status_out, R.color.status_out_bg)
                item.isLowStock -> Triple(R.string.status_low, R.color.status_low, R.color.status_low_bg)
                else -> Triple(R.string.status_ok, R.color.status_ok, R.color.status_ok_bg)
            }
            tvStatusBadge.text = context.getString(statusTextRes)
            tvStatusBadge.setTextColor(ContextCompat.getColor(context, statusColorRes))
            tvStatusBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, statusBgRes))

            layoutQuickActions.visibility = if (canManageStock) View.VISIBLE else View.GONE
            btnDecrease.isEnabled = item.adet > 0
            btnDecrease.alpha = if (item.adet > 0) 1f else 0.4f

            root.setOnClickListener { onItemClick(item) }
            btnStockAdjust.setOnClickListener { onBulkAdjust(item) }
            btnDecrease.setOnClickListener { onAdjustQuantity(item, -1) }
            btnIncrease.setOnClickListener { onAdjustQuantity(item, +1) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UrunModel>() {
            override fun areItemsTheSame(oldItem: UrunModel, newItem: UrunModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UrunModel, newItem: UrunModel): Boolean =
                oldItem == newItem
        }
    }
}
