package com.burak.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockAnalyticsTest {

    private val sampleItems = listOf(
        UrunModel(
            id = 1,
            ad = "Kablosuz Fare",
            kod = "STK-001",
            kategori = "Elektronik",
            adet = 20,
            minStok = 5,
            birimFiyat = 150.0,
            aciklama = "Optik",
            guncellemeTarihi = 1000L
        ),
        UrunModel(
            id = 2,
            ad = "USB-C Kablo",
            kod = "0042",
            kategori = "Elektronik",
            adet = 3,
            minStok = 5,
            birimFiyat = 80.0,
            aciklama = "Örgülü",
            guncellemeTarihi = 3000L
        ),
        UrunModel(
            id = 3,
            ad = "A4 Kağıt",
            kod = "OFS-10",
            kategori = "Ofis",
            adet = 0,
            minStok = 10,
            birimFiyat = 120.0,
            aciklama = "500 yaprak",
            guncellemeTarihi = 2000L
        )
    )

    @Test
    fun computeSummary_calculatesAllKpisAccurately() {
        val summary = StockAnalytics.computeSummary(sampleItems)
        assertEquals(3, summary.toplamUrunCesidi)
        assertEquals(23, summary.toplamAdet)
        assertEquals(1, summary.kritikStokSayisi)
        assertEquals(1, summary.tukenenStokSayisi)
        assertEquals(2, summary.toplamUyariSayisi)
        assertEquals(3240.0, summary.toplamDeger, 0.001)
    }

    @Test
    fun filterAndSort_filtersByQueryAndStatusAndSortsCorrectly() {
        val lowOnly = StockAnalytics.filterAndSort(
            sampleItems, "", StokFiltre.KRITIK, StokSiralama.AD_ARTAN
        )
        assertEquals(listOf("0042"), lowOnly.map { it.kod })
        assertTrue(lowOnly.first().isLowStock)
        assertFalse(lowOnly.first().isOutOfStock)

        val outOnly = StockAnalytics.filterAndSort(
            sampleItems, "", StokFiltre.TUKENEN, StokSiralama.AD_ARTAN
        )
        assertEquals(listOf("OFS-10"), outOnly.map { it.kod })
        assertTrue(outOnly.first().isOutOfStock)

        val bySku = StockAnalytics.filterAndSort(
            sampleItems, "stk-001", StokFiltre.TUMU, StokSiralama.AD_ARTAN
        )
        assertEquals(1, bySku.size)
        assertEquals("Kablosuz Fare", bySku.first().ad)

        val byRecent = StockAnalytics.filterAndSort(
            sampleItems, "", StokFiltre.TUMU, StokSiralama.SON_GUNCELLEME
        )
        assertEquals(listOf(2L, 3L, 1L), byRecent.map { it.id })

        val byQtyDesc = StockAnalytics.filterAndSort(
            sampleItems, "", StokFiltre.TUMU, StokSiralama.ADET_AZALAN
        )
        assertEquals(listOf(1L, 2L, 3L), byQtyDesc.map { it.id })
    }
}
