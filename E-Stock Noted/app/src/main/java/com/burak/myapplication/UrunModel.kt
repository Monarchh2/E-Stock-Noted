package com.burak.myapplication

enum class UserRole {
    ADMIN,
    USER;

    companion object {
        fun fromString(value: String?): UserRole {
            return if (value.equals("ADMIN", ignoreCase = true)) ADMIN else USER
        }
    }
}

data class UserModel(
    val username: String,
    val email: String,
    val business: String,
    val role: UserRole = UserRole.USER,
    val createdAt: Long = System.currentTimeMillis()
)

data class UrunModel(
    val id: Long,
    val ad: String,
    val kod: String,
    val kategori: String = DEFAULT_CATEGORY,
    val adet: Int,
    val minStok: Int = DEFAULT_MIN_STOCK,
    val birimFiyat: Double = 0.0,
    val aciklama: String = "",
    val guncellemeTarihi: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean
        get() = adet <= 0

    val isLowStock: Boolean
        get() = adet > 0 && adet <= minStok

    val toplamDeger: Double
        get() = adet * birimFiyat

    companion object {
        const val DEFAULT_CATEGORY = "Genel"
        const val DEFAULT_MIN_STOCK = 5

        val DEFAULT_CATEGORIES = listOf(
            "Genel",
            "Elektronik",
            "Gıda",
            "Ofis",
            "Giyim",
            "Hırdavat",
            "Diğer"
        )
    }
}

enum class HareketTipi {
    GIRIS,
    CIKIS,
    GUNCELLEME,
    SILME;

    companion object {
        fun fromString(value: String?): HareketTipi {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GUNCELLEME
        }
    }
}

data class StokHareketModel(
    val id: Long,
    val urunId: Long,
    val urunAdi: String,
    val urunKodu: String,
    val hareketTipi: HareketTipi,
    val miktarDegisimi: Int,
    val yeniAdet: Int,
    val not: String,
    val yapanKullanici: String,
    val tarih: Long
)

data class StokOzet(
    val toplamUrunCesidi: Int,
    val toplamAdet: Int,
    val kritikStokSayisi: Int,
    val tukenenStokSayisi: Int,
    val toplamDeger: Double
) {
    val toplamUyariSayisi: Int
        get() = kritikStokSayisi + tukenenStokSayisi
}

enum class StokFiltre {
    TUMU,
    KRITIK,
    TUKENEN
}

enum class StokSiralama {
    AD_ARTAN,
    ADET_ARTAN,
    ADET_AZALAN,
    SON_GUNCELLEME
}