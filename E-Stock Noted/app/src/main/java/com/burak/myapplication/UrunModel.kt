package com.burak.myapplication

enum class UserRole {
    ADMIN,
    DEPO,
    USER;

    val canManageStock: Boolean
        get() = this == ADMIN || this == DEPO

    val canManageUsers: Boolean
        get() = this == ADMIN

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
        }
    }
}

data class UserModel(
    val username: String,
    val email: String,
    val business: String,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val mustChangePassword: Boolean = false,
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
    val alisFiyati: Double = 0.0,
    val kdvOrani: Int = DEFAULT_VAT_RATE,
    val tedarikci: String = "",
    val depoKonum: String = DEFAULT_WAREHOUSE,
    val aciklama: String = "",
    val guncellemeTarihi: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean
        get() = adet <= 0

    val isLowStock: Boolean
        get() = adet > 0 && adet <= minStok

    val toplamDeger: Double
        get() = adet * birimFiyat

    val toplamMaliyet: Double
        get() = adet * alisFiyati

    val birimKar: Double
        get() = birimFiyat - alisFiyati

    val tahminiToplamKar: Double
        get() = adet * birimKar

    val karMarjiYuzde: Double
        get() = if (birimFiyat > 0.0) ((birimFiyat - alisFiyati) / birimFiyat) * 100.0 else 0.0

    val kdvDahilSatisFiyati: Double
        get() = birimFiyat * (1.0 + kdvOrani / 100.0)

    val onerilenSiparisMiktari: Int
        get() = if (adet <= minStok) {
            ((minStok * 2) - adet).coerceAtLeast(1)
        } else {
            0
        }

    companion object {
        const val DEFAULT_CATEGORY = "Genel"
        const val DEFAULT_MIN_STOCK = 5
        const val DEFAULT_VAT_RATE = 20
        const val DEFAULT_WAREHOUSE = "Ana Depo"

        val DEFAULT_CATEGORIES = listOf(
            "Genel",
            "Elektronik",
            "Gıda",
            "Ofis",
            "Giyim",
            "Hırdavat",
            "Sağlık",
            "Diğer"
        )

        val DEFAULT_VAT_RATES = listOf(0, 1, 10, 20)
    }
}

data class TedarikciModel(
    val id: Long,
    val firmaAdi: String,
    val yetkiliKisi: String = "",
    val telefon: String = "",
    val eposta: String = "",
    val vergiNo: String = "",
    val notlar: String = ""
)

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

data class SecurityAuditLogModel(
    val id: Long,
    val olayTipi: String,
    val kullaniciAdi: String,
    val detay: String,
    val basarili: Boolean,
    val tarih: Long
)

data class StokOzet(
    val toplamUrunCesidi: Int,
    val toplamAdet: Int,
    val kritikStokSayisi: Int,
    val tukenenStokSayisi: Int,
    val toplamDeger: Double,
    val toplamMaliyet: Double = 0.0,
    val tahminiToplamKar: Double = 0.0,
    val ortalamaKarMarji: Double = 0.0
) {
    val toplamUyariSayisi: Int
        get() = kritikStokSayisi + tukenenStokSayisi

    val yeterliStokSayisi: Int
        get() = (toplamUrunCesidi - kritikStokSayisi - tukenenStokSayisi).coerceAtLeast(0)
}

data class KategoriAnaliz(
    val kategori: String,
    val urunSayisi: Int,
    val toplamAdet: Int,
    val toplamDeger: Double,
    val oranYuzde: Double
)

data class GunlukHareketOzet(
    val gunEtiketi: String,
    val girisMiktari: Int,
    val cikisMiktari: Int
)

enum class RaporZamanAraligi {
    SON_7_GUN,
    SON_30_GUN,
    TUMU
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