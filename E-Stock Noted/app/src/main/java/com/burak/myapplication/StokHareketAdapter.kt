package com.burak.myapplication

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ItemStokHareketBinding

class StokHareketAdapter :
    ListAdapter<StokHareketModel, StokHareketAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemStokHareketBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStokHareketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val move = getItem(position)
        val context = holder.itemView.context

        with(holder.binding) {
            val (labelRes, colorRes, bgRes) = when (move.hareketTipi) {
                HareketTipi.GIRIS -> Triple(R.string.movement_type_in, R.color.status_ok, R.color.status_ok_bg)
                HareketTipi.CIKIS -> Triple(R.string.movement_type_out, R.color.status_low, R.color.status_low_bg)
                HareketTipi.GUNCELLEME -> Triple(R.string.movement_type_update, R.color.status_info, R.color.status_info_bg)
                HareketTipi.SILME -> Triple(R.string.movement_type_delete, R.color.status_out, R.color.status_out_bg)
            }

            tvMoveBadge.text = context.getString(labelRes)
            tvMoveBadge.setTextColor(ContextCompat.getColor(context, colorRes))
            tvMoveBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgRes))

            tvMoveProduct.text = context.getString(
                R.string.subtitle_session_format,
                move.urunAdi,
                move.urunKodu
            )

            tvMoveDelta.text = if (move.miktarDegisimi >= 0) {
                context.getString(R.string.movement_delta_positive, move.miktarDegisimi)
            } else {
                context.getString(R.string.movement_delta_negative, move.miktarDegisimi)
            }
            tvMoveDelta.setTextColor(ContextCompat.getColor(context, colorRes))

            val dateStr = StockAnalytics.formatDateTime(move.tarih)
            val userLabel = move.yapanKullanici.ifBlank { "-" }
            tvMoveMeta.text = context.getString(
                R.string.movement_meta_format,
                userLabel,
                move.yeniAdet,
                dateStr
            )

            if (move.not.isNotBlank()) {
                tvMoveNote.text = move.not
                tvMoveNote.visibility = View.VISIBLE
            } else {
                tvMoveNote.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StokHareketModel>() {
            override fun areItemsTheSame(oldItem: StokHareketModel, newItem: StokHareketModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: StokHareketModel, newItem: StokHareketModel): Boolean =
                oldItem == newItem
        }
    }
}
