package com.burak.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommercialAnalyticsTest {

    private val items = listOf(
        UrunModel(
            id = 1,
            ad = "Laptop",
            kod = "ELK-01",
            kategori = "Elektronik",
            adet = 10,
            minStok = 3,
            birimFiyat = 20000.0,
            alisFiyati = 15000.0,
            kdvOrani = 20,
            tedarikci = "Tekno AŞ"
        ),
        UrunModel(
            id = 2,
            ad = "Mouse",
            kod = "ELK-02",
            kategori = "Elektronik",
            adet = 2,
            minStok = 5,
            birimFiyat = 500.0,
            alisFiyati = 300.0,
            kdvOrani = 20,
            tedarikci = "Tekno AŞ"
        ),
        UrunModel(
            id = 3,
            ad = "Klasör",
            kod = "OFS-01",
            kategori = "Ofis",
            adet = 0,
            minStok = 10,
            birimFiyat = 100.0,
            alisFiyati = 60.0,
            kdvOrani = 10,
            tedarikci = "Ofis Ltd"
        )
    )

    @Test
    fun commercialMetrics_computesProfitMarginVatAndReorderQuantities() {
        val laptop = items[0]
        assertEquals(5000.0, laptop.birimKar, 0.001)
        assertEquals(50000.0, laptop.tahminiToplamKar, 0.001)
        assertEquals(25.0, laptop.karMarjiYuzde, 0.001)
        assertEquals(24000.0, laptop.kdvDahilSatisFiyati, 0.001)
        assertEquals(0, laptop.onerilenSiparisMiktari)

        val mouse = items[1]
        assertEquals(8, mouse.onerilenSiparisMiktari) // minStok*2 - adet = 10 - 2

        val folder = items[2]
        assertEquals(20, folder.onerilenSiparisMiktari) // 20 - 0

        val summary = StockAnalytics.computeSummary(items)
        assertEquals(150600.0, summary.toplamMaliyet, 0.001)
        assertEquals(201000.0, summary.toplamDeger, 0.001)
        assertEquals(50400.0, summary.tahminiToplamKar, 0.001)
        assertEquals(1, summary.yeterliStokSayisi)
    }

    @Test
    fun categoryDistributionAndTopRanking_produceAccurateChartData() {
        val dist = StockAnalytics.computeCategoryDistribution(items)
        assertEquals(2, dist.size)
        assertEquals("Elektronik", dist[0].kategori)
        assertEquals(100.0, dist.sumOf { it.oranYuzde }, 0.01)

        val top2 = StockAnalytics.computeTopValuedProducts(items, 2)
        assertEquals(listOf("ELK-01", "ELK-02"), top2.map { it.kod })

        val reorder = StockAnalytics.computeReorderList(items)
        assertEquals(listOf("OFS-01", "ELK-02"), reorder.map { it.kod })
    }

    @Test
    fun dailyMovementTrend_bucketsLastSevenDaysCorrectly() {
        val now = 1_760_000_000_000L
        val oneDay = 24L * 60L * 60L * 1000L
        val moves = listOf(
            StokHareketModel(1, 1, "Laptop", "ELK-01", HareketTipi.GIRIS, 10, 10, "", "admin", now),
            StokHareketModel(2, 1, "Laptop", "ELK-01", HareketTipi.CIKIS, -3, 7, "", "admin", now),
            StokHareketModel(3, 2, "Mouse", "ELK-02", HareketTipi.GIRIS, 5, 5, "", "admin", now - (2 * oneDay)),
            // Older than 7 days -> excluded from SON_7_GUN
            StokHareketModel(4, 2, "Mouse", "ELK-02", HareketTipi.GIRIS, 99, 99, "", "admin", now - (15 * oneDay))
        )

        val trend7d = StockAnalytics.computeDailyMovementTrend(moves, RaporZamanAraligi.SON_7_GUN, now)
        assertEquals(7, trend7d.size)
        assertEquals(15, trend7d.sumOf { it.girisMiktari })
        assertEquals(3, trend7d.sumOf { it.cikisMiktari })
        assertTrue(trend7d.last().girisMiktari == 10 && trend7d.last().cikisMiktari == 3)
    }
}
