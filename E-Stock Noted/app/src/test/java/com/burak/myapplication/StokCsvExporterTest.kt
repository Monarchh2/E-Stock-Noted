package com.burak.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class StokCsvExporterTest {

    @Test
    fun toCsv_includesBomHeaderEscapesQuotesAndPreservesStringSku() {
        val items = listOf(
            UrunModel(
                id = 1,
                ad = "Kablo, \"Pro\"",
                kod = "0042",
                kategori = "Elektronik",
                adet = 4,
                minStok = 5,
                birimFiyat = 99.5,
                aciklama = "Raf 1, Kutu 2"
            )
        )

        val csv = StokCsvExporter.toCsv(items)
        assertTrue(csv.startsWith("\uFEFFID,Ürün Kodu,Ürün Adı"))
        assertTrue(csv.contains("1,0042,\"Kablo, \"\"Pro\"\"\",Elektronik,4,5,99.50,398.00,Kritik,\"Raf 1, Kutu 2\""))
    }
}
