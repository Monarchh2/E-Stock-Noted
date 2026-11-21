package com.burak.myapplication

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StockAnalytics {
    private val TR_LOCALE = Locale("tr", "TR")
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun computeSummary(items: List<UrunModel>): StokOzet {
        var toplamAdet = 0
        var kritikSayisi = 0
        var tukenenSayisi = 0
        var toplamDeger = 0.0
        var toplamMaliyet = 0.0

        for (item in items) {
            toplamAdet += item.adet
            toplamDeger += item.toplamDeger
            toplamMaliyet += item.toplamMaliyet
            when {
                item.isOutOfStock -> tukenenSayisi++
                item.isLowStock -> kritikSayisi++
            }
        }

        val tahminiKar = toplamDeger - toplamMaliyet
        val ortalamaMarj = if (toplamDeger > 0.0) (tahminiKar / toplamDeger) * 100.0 else 0.0

        return StokOzet(
            toplamUrunCesidi = items.size,
            toplamAdet = toplamAdet,
            kritikStokSayisi = kritikSayisi,
            tukenenStokSayisi = tukenenSayisi,
            toplamDeger = toplamDeger,
            toplamMaliyet = toplamMaliyet,
            tahminiToplamKar = tahminiKar,
            ortalamaKarMarji = ortalamaMarj
        )
    }

    fun computeCategoryDistribution(items: List<UrunModel>): List<KategoriAnaliz> {
        if (items.isEmpty()) return emptyList()

        val grouped = items.groupBy { it.kategori.ifBlank { UrunModel.DEFAULT_CATEGORY } }
        val grandValue = items.sumOf { it.toplamDeger }
        val grandQty = items.sumOf { it.adet }

        return grouped.map { (category, list) ->
            val catValue = list.sumOf { it.toplamDeger }
            val catQty = list.sumOf { it.adet }
            val ratio = when {
                grandValue > 0.0 -> (catValue / grandValue) * 100.0
                grandQty > 0 -> (catQty.toDouble() / grandQty.toDouble()) * 100.0
                else -> (list.size.toDouble() / items.size.toDouble()) * 100.0
            }
            KategoriAnaliz(
                kategori = category,
                urunSayisi = list.size,
                toplamAdet = catQty,
                toplamDeger = catValue,
                oranYuzde = ratio
            )
        }.sortedByDescending { it.oranYuzde }
    }

    fun computeTopValuedProducts(items: List<UrunModel>, limit: Int = 5): List<UrunModel> {
        return items
            .sortedWith(
                compareByDescending<UrunModel> { it.toplamDeger }
                    .thenByDescending { it.adet }
                    .thenBy { it.ad.lowercase(TR_LOCALE) }
            )
            .take(limit)
    }

    fun computeReorderList(items: List<UrunModel>): List<UrunModel> {
        return items
            .filter { it.adet <= it.minStok }
            .sortedWith(compareBy<UrunModel> { it.adet }.thenBy { it.tedarikci.lowercase(TR_LOCALE) })
    }

    fun filterMovementsByRange(
        movements: List<StokHareketModel>,
        range: RaporZamanAraligi,
        nowMillis: Long = System.currentTimeMillis()
    ): List<StokHareketModel> {
        val cutoff = when (range) {
            RaporZamanAraligi.SON_7_GUN -> nowMillis - (7L * ONE_DAY_MILLIS)
            RaporZamanAraligi.SON_30_GUN -> nowMillis - (30L * ONE_DAY_MILLIS)
            RaporZamanAraligi.TUMU -> Long.MIN_VALUE
        }
        return movements.filter { it.tarih >= cutoff }
    }

    fun computeDailyMovementTrend(
        movements: List<StokHareketModel>,
        range: RaporZamanAraligi,
        nowMillis: Long = System.currentTimeMillis()
    ): List<GunlukHareketOzet> {
        val ranged = filterMovementsByRange(movements, range, nowMillis)
        val dayFormat = SimpleDateFormat("dd MMM", TR_LOCALE)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        if (range == RaporZamanAraligi.SON_7_GUN) {
            val byKey = mutableMapOf<String, Pair<Int, Int>>()
            for (move in ranged) {
                val key = keyFormat.format(Date(move.tarih))
                val cur = byKey[key] ?: (0 to 0)
                byKey[key] = when {
                    move.miktarDegisimi > 0 -> (cur.first + move.miktarDegisimi) to cur.second
                    move.miktarDegisimi < 0 -> cur.first to (cur.second + kotlin.math.abs(move.miktarDegisimi))
                    else -> cur
                }
            }

            val result = mutableListOf<GunlukHareketOzet>()
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            cal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until 7) {
                val date = cal.time
                val key = keyFormat.format(date)
                val pair = byKey[key] ?: (0 to 0)
                result.add(
                    GunlukHareketOzet(
                        gunEtiketi = dayFormat.format(date),
                        girisMiktari = pair.first,
                        cikisMiktari = pair.second
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return result
        }

        if (ranged.isEmpty()) return emptyList()

        val grouped = linkedMapOf<String, MutableList<StokHareketModel>>()
        for (move in ranged.sortedBy { it.tarih }) {
            val key = keyFormat.format(Date(move.tarih))
            grouped.getOrPut(key) { mutableListOf() }.add(move)
        }

        return grouped.entries.toList().takeLast(7).map { (_, dayMoves) ->
            var giris = 0
            var cikis = 0
            for (m in dayMoves) {
                if (m.miktarDegisimi > 0) giris += m.miktarDegisimi
                else if (m.miktarDegisimi < 0) cikis += kotlin.math.abs(m.miktarDegisimi)
            }
            GunlukHareketOzet(
                gunEtiketi = dayFormat.format(Date(dayMoves.first().tarih)),
                girisMiktari = giris,
                cikisMiktari = cikis
            )
        }
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
                item.tedarikci.lowercase(TR_LOCALE).contains(normalizedQuery) ||
                item.depoKonum.lowercase(TR_LOCALE).contains(normalizedQuery) ||
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

    fun formatPercent(value: Double): String = String.format(TR_LOCALE, "%%%.1f", value)

    fun formatDateTime(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", TR_LOCALE)
        return formatter.format(Date(timestampMillis))
    }
}
