package com.burak.myapplication

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StockAnalytics {
    private val TR_LOCALE = Locale("tr", "TR")

    fun computeSummary(items: List<UrunModel>): StokOzet {
        var toplamAdet = 0
        var kritikSayisi = 0
        var tukenenSayisi = 0
        var toplamDeger = 0.0

        for (item in items) {
            toplamAdet += item.adet
            toplamDeger += item.toplamDeger
            when {
                item.isOutOfStock -> tukenenSayisi++
                item.isLowStock -> kritikSayisi++
            }
        }

        return StokOzet(
            toplamUrunCesidi = items.size,
            toplamAdet = toplamAdet,
            kritikStokSayisi = kritikSayisi,
            tukenenStokSayisi = tukenenSayisi,
            toplamDeger = toplamDeger
        )
    }

    fun extractCategories(items: List<UrunModel>): List<String> {
        return items
            .map { it.kategori.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase(TR_LOCALE) }
    }

    fun filterAndSort(
        items: List<UrunModel>,
        query: String,
        filtre: StokFiltre,
        siralama: StokSiralama,
        kategori: String? = null
    ): List<UrunModel> {
        val normalizedQuery = query.trim().lowercase(TR_LOCALE)
        val normalizedCategory = kategori?.trim()?.takeIf { it.isNotEmpty() }

        val filtered = items.filter { item ->
            val matchesFilter = when (filtre) {
                StokFiltre.TUMU -> true
                StokFiltre.KRITIK -> item.isLowStock
                StokFiltre.TUKENEN -> item.isOutOfStock
            }
            if (!matchesFilter) return@filter false

            if (normalizedCategory != null && !item.kategori.equals(normalizedCategory, ignoreCase = true)) {
                return@filter false
            }

            if (normalizedQuery.isEmpty()) return@filter true

            item.ad.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                item.kod.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                item.kategori.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                item.aciklama.lowercase(TR_LOCALE).contains(normalizedQuery)
        }

        return when (siralama) {
            StokSiralama.AD_ARTAN -> filtered.sortedBy { it.ad.lowercase(TR_LOCALE) }
            StokSiralama.ADET_ARTAN -> filtered.sortedWith(
                compareBy<UrunModel> { it.adet }.thenBy { it.ad.lowercase(TR_LOCALE) }
            )
            StokSiralama.ADET_AZALAN -> filtered.sortedWith(
                compareByDescending<UrunModel> { it.adet }.thenBy { it.ad.lowercase(TR_LOCALE) }
            )
            StokSiralama.SON_GUNCELLEME -> filtered.sortedByDescending { it.guncellemeTarihi }
        }
    }

    fun filterMovements(
        movements: List<StokHareketModel>,
        query: String,
        tip: HareketTipi? = null
    ): List<StokHareketModel> {
        val normalizedQuery = query.trim().lowercase(TR_LOCALE)
        return movements.filter { move ->
            if (tip != null && move.hareketTipi != tip) return@filter false
            if (normalizedQuery.isEmpty()) return@filter true

            move.urunAdi.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                move.urunKodu.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                move.yapanKullanici.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                move.not.lowercase(TR_LOCALE).contains(normalizedQuery)
        }
    }

    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(TR_LOCALE).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "₺${formatter.format(amount)}"
    }

    fun formatDateTime(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", TR_LOCALE)
        return formatter.format(Date(timestampMillis))
    }
}
