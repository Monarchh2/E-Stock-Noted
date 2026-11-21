package com.burak.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupJsonCodecTest {

    @Test
    fun encodeAndDecode_roundTripsProductsAndSuppliers() {
        val products = listOf(
            UrunModel(
                id = 1,
                ad = "Kablo \"Pro\"",
                kod = "0042",
                kategori = "Elektronik",
                adet = 12,
                minStok = 4,
                birimFiyat = 199.90,
                alisFiyati = 120.00,
                kdvOrani = 20,
                tedarikci = "Tekno AŞ",
                depoKonum = "Raf B-2",
                aciklama = "Satır 1\nSatır 2"
            )
        )
        val suppliers = listOf(
            TedarikciModel(1, "Tekno AŞ", "Ahmet Yılmaz", "05551234567", "info@tekno.com", "1234567890", "Ana tedarikçi")
        )

        val json = BackupJsonCodec.encode(products, suppliers, exportedAt = 123456789L)
        val decoded = BackupJsonCodec.decode(json)

        assertNotNull(decoded)
        assertEquals(BackupJsonCodec.CURRENT_SCHEMA_VERSION, decoded!!.schemaVersion)
        assertEquals(1, decoded.products.size)
        assertEquals("0042", decoded.products[0].kod)
        assertEquals("Kablo \"Pro\"", decoded.products[0].ad)
        assertEquals(120.0, decoded.products[0].alisFiyati, 0.001)
        assertEquals("Raf B-2", decoded.products[0].depoKonum)
        assertEquals(1, decoded.suppliers.size)
        assertEquals("Tekno AŞ", decoded.suppliers[0].firmaAdi)
    }

    @Test
    fun decode_rejectsMalformedUnsupportedOrNegativePayloads() {
        assertNull(BackupJsonCodec.decode(""))
        assertNull(BackupJsonCodec.decode("{invalid json"))
        assertNull(BackupJsonCodec.decode("""{"schemaVersion": 999, "products": []}"""))
        assertNull(
            BackupJsonCodec.decode(
                """{"schemaVersion": 3, "products": [{"ad":"Test","kod":"T1","adet":-5}]}"""
            )
        )
        assertNull(
            BackupJsonCodec.decode(
                """{"schemaVersion": 3, "products": [{"ad":"","kod":"T1","adet":5}]}"""
            )
        )
    }
}
