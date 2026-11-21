package com.burak.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.burak.myapplication.databinding.ItemStokBinding

class StokListAdapter(private val urunList: ArrayList<UrunModel>) :
    RecyclerView.Adapter<StokListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStokBinding) : RecyclerView.ViewHolder(binding.root)

    var onItemClick: ((UrunModel) -> Unit)? = null // Öğe tıklama geri çağrısı

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStokBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)

        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(urunList[position])
            }
        }

        return viewHolder
    }

    override fun getItemCount(): Int {
        return urunList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.urunAdTextView.text =
            "Kod: " + urunList[position].kod.toString() + "\n Ad:" + urunList[position].ad + "\n Adet:" + urunList[position].adet + "\n Açıklama:" + urunList[position].aciklama + "\n "
    }
}
