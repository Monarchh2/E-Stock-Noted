package com.burak.myapplication

import java.util.Locale

object StokCsvExporter {
    private const val UTF8_BOM = "\uFEFF"

    fun toCsv(items: List<UrunModel>): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append("ID,Ürün Kodu,Ürün Adı,Kategori,Adet,Kritik Eşik,Birim Fiyat,Toplam Değer,Durum,Açıklama\n")

        for (item in items) {
            val status = when {
                item.isOutOfStock -> "Tükendi"
                item.isLowStock -> "Kritik"
                else -> "Yeterli"
            }
            val row = listOf(
                item.id.toString(),
                escape(item.kod),
                escape(item.ad),
                escape(item.kategori),
                item.adet.toString(),
                item.minStok.toString(),
                String.format(Locale.US, "%.2f", item.birimFiyat),
                String.format(Locale.US, "%.2f", item.toplamDeger),
                status,
                escape(item.aciklama)
            )
            sb.append(row.joinToString(",")).append('\n')
        }
        return sb.toString()
    }

    private fun escape(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
