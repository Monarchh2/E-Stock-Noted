package com.burak.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class StokCsvExporterTest {

    @Test
    fun toCsv_includesBomCommercialColumnsEscapesQuotesAndPreservesStringSku() {
        val items = listOf(
            UrunModel(
                id = 1,
                ad = "Kablo, \"Pro\"",
                kod = "0042",
                kategori = "Elektronik",
                adet = 4,
                minStok = 5,
                birimFiyat = 100.0,
                alisFiyati = 60.0,
                kdvOrani = 20,
                tedarikci = "Tekno AŞ",
                depoKonum = "Raf A-2",
                aciklama = "Raf 1, Kutu 2"
            )
        )

        val csv = StokCsvExporter.toCsv(items)
        assertTrue(csv.startsWith("\uFEFFID,Ürün Kodu,Ürün Adı,Kategori,Adet,Kritik Eşik,Alış Fiyatı,Satış Fiyatı"))
        assertTrue(
            csv.contains(
                "1,0042,\"Kablo, \"\"Pro\"\"\",Elektronik,4,5,60.00,100.00,20,40.0,240.00,400.00,160.00,Tekno AŞ,Raf A-2,Kritik,\"Raf 1, Kutu 2\""
            )
        )
    }
}
