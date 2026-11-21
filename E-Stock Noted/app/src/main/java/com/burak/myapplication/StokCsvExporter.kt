package com.burak.myapplication

import java.util.Locale

object StokCsvExporter {
    private const val UTF8_BOM = "\uFEFF"

    fun toCsv(items: List<UrunModel>): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append(
            "ID,Ürün Kodu,Ürün Adı,Kategori,Adet,Kritik Eşik,Alış Fiyatı,Satış Fiyatı,KDV %,Kâr Marjı %,Toplam Maliyet,Toplam Değer,Tahmini Kâr,Tedarikçi,Depo/Raf,Durum,Açıklama\n"
        )

        for (item in items) {
            val status = when {
                item.isOutOfStock -> "Tükendi"
                item.isLowStock -> "Kritik"
                else -> "Yeterli"
            }
            val row = listOf(
                item.id.toString(),
                escape(SecuritySanitizer.sanitizeCsvCell(item.kod)),
                escape(SecuritySanitizer.sanitizeCsvCell(item.ad)),
                escape(SecuritySanitizer.sanitizeCsvCell(item.kategori)),
                item.adet.toString(),
                item.minStok.toString(),
                String.format(Locale.US, "%.2f", item.alisFiyati),
                String.format(Locale.US, "%.2f", item.birimFiyat),
                item.kdvOrani.toString(),
                String.format(Locale.US, "%.1f", item.karMarjiYuzde),
                String.format(Locale.US, "%.2f", item.toplamMaliyet),
                String.format(Locale.US, "%.2f", item.toplamDeger),
                String.format(Locale.US, "%.2f", item.tahminiToplamKar),
                escape(SecuritySanitizer.sanitizeCsvCell(item.tedarikci)),
                escape(SecuritySanitizer.sanitizeCsvCell(item.depoKonum)),
                status,
                escape(SecuritySanitizer.sanitizeCsvCell(item.aciklama))
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
