package com.burak.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 3
        private const val DATABASE_NAME = "Stoklar"

        private const val STOK_TABLE_NAME = "stoklar"
        private const val STOK_COLUMN_ID = "id"
        private const val STOK_COLUMN_AD = "ad"
        private const val STOK_COLUMN_KOD = "kod"
        private const val STOK_COLUMN_KATEGORI = "kategori"
        private const val STOK_COLUMN_ADET = "adet"
        private const val STOK_COLUMN_MIN_STOK = "min_stok"
        private const val STOK_COLUMN_BIRIM_FIYAT = "birim_fiyat"
        private const val STOK_COLUMN_ALIS_FIYATI = "alis_fiyati"
        private const val STOK_COLUMN_KDV_ORANI = "kdv_orani"
        private const val STOK_COLUMN_TEDARIKCI = "tedarikci"
        private const val STOK_COLUMN_DEPO_KONUM = "depo_konum"
        private const val STOK_COLUMN_ACIKLAMA = "aciklama"
        private const val STOK_COLUMN_GUNCELLEME_TARIHI = "guncelleme_tarihi"

        private const val USER_TABLE_NAME = "kullanici"
        private const val USER_USERNAME = "username"
        private const val USER_PASSWORD = "password"
        private const val USER_EMAIL = "email"
        private const val USER_BUSINESS = "business"
        private const val USER_ROLE = "role"
        private const val USER_IS_ACTIVE = "is_active"
        private const val USER_MUST_CHANGE_PWD = "must_change_password"
        private const val USER_CREATED_AT = "created_at"

        private const val MOVEMENT_TABLE_NAME = "stok_hareketleri"
        private const val MOV_ID = "id"
        private const val MOV_URUN_ID = "urun_id"
        private const val MOV_URUN_AD = "urun_ad"
        private const val MOV_URUN_KOD = "urun_kod"
        private const val MOV_TIPI = "hareket_tipi"
        private const val MOV_MIKTAR = "miktar_degisimi"
        private const val MOV_YENI_ADET = "yeni_adet"
        private const val MOV_NOT = "aciklama_not"
        private const val MOV_KULLANICI = "yapan_kullanici"
        private const val MOV_TARIH = "tarih"

        private const val SUPPLIER_TABLE_NAME = "tedarikciler"
        private const val SUP_ID = "id"
        private const val SUP_FIRMA = "firma_adi"
        private const val SUP_YETKILI = "yetkili_kisi"
        private const val SUP_TELEFON = "telefon"
        private const val SUP_EPOSTA = "eposta"
        private const val SUP_VERGI_NO = "vergi_no"
        private const val SUP_NOTLAR = "notlar"

        private const val AUDIT_TABLE_NAME = "security_audit_log"
        private const val AUD_ID = "id"
        private const val AUD_OLAY = "olay_tipi"
        private const val AUD_KULLANICI = "kullanici_adi"
        private const val AUD_DETAY = "detay"
        private const val AUD_BASARILI = "basarili"
        private const val AUD_TARIH = "tarih"

        private const val DEFAULT_ADMIN_USERNAME = "admin"
        private const val DEFAULT_ADMIN_INITIAL_SECRET = "12345"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createStoklarTable = """
            CREATE TABLE IF NOT EXISTS $STOK_TABLE_NAME (
                $STOK_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $STOK_COLUMN_AD TEXT NOT NULL,
                $STOK_COLUMN_KOD TEXT NOT NULL,
                $STOK_COLUMN_KATEGORI TEXT NOT NULL DEFAULT '${UrunModel.DEFAULT_CATEGORY}',
                $STOK_COLUMN_ADET INTEGER NOT NULL DEFAULT 0,
                $STOK_COLUMN_MIN_STOK INTEGER NOT NULL DEFAULT ${UrunModel.DEFAULT_MIN_STOCK},
                $STOK_COLUMN_BIRIM_FIYAT REAL NOT NULL DEFAULT 0.0,
                $STOK_COLUMN_ALIS_FIYATI REAL NOT NULL DEFAULT 0.0,
                $STOK_COLUMN_KDV_ORANI INTEGER NOT NULL DEFAULT ${UrunModel.DEFAULT_VAT_RATE},
                $STOK_COLUMN_TEDARIKCI TEXT NOT NULL DEFAULT '',
                $STOK_COLUMN_DEPO_KONUM TEXT NOT NULL DEFAULT '${UrunModel.DEFAULT_WAREHOUSE}',
                $STOK_COLUMN_ACIKLAMA TEXT NOT NULL DEFAULT '',
                $STOK_COLUMN_GUNCELLEME_TARIHI INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createStoklarTable)

        val createUserTable = """
            CREATE TABLE IF NOT EXISTS $USER_TABLE_NAME (
                $USER_USERNAME TEXT PRIMARY KEY,
                $USER_PASSWORD TEXT NOT NULL,
                $USER_BUSINESS TEXT NOT NULL,
                $USER_EMAIL TEXT NOT NULL,
                $USER_ROLE TEXT NOT NULL DEFAULT 'USER',
                $USER_IS_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $USER_MUST_CHANGE_PWD INTEGER NOT NULL DEFAULT 0,
                $USER_CREATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createUserTable)

        createMovementsTable(db)
        createSuppliersTable(db)
        createAuditTable(db)
        seedDefaultAdminIfNeeded(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_KATEGORI, "TEXT NOT NULL DEFAULT '${UrunModel.DEFAULT_CATEGORY}'")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_MIN_STOK, "INTEGER NOT NULL DEFAULT ${UrunModel.DEFAULT_MIN_STOCK}")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_BIRIM_FIYAT, "REAL NOT NULL DEFAULT 0.0")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_GUNCELLEME_TARIHI, "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, USER_TABLE_NAME, USER_ROLE, "TEXT NOT NULL DEFAULT 'USER'")
            addColumnIfMissing(db, USER_TABLE_NAME, USER_CREATED_AT, "INTEGER NOT NULL DEFAULT 0")
            createMovementsTable(db)
        }
        if (oldVersion < 3) {
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_ALIS_FIYATI, "REAL NOT NULL DEFAULT 0.0")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_KDV_ORANI, "INTEGER NOT NULL DEFAULT ${UrunModel.DEFAULT_VAT_RATE}")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_TEDARIKCI, "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, STOK_TABLE_NAME, STOK_COLUMN_DEPO_KONUM, "TEXT NOT NULL DEFAULT '${UrunModel.DEFAULT_WAREHOUSE}'")
            addColumnIfMissing(db, USER_TABLE_NAME, USER_IS_ACTIVE, "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, USER_TABLE_NAME, USER_MUST_CHANGE_PWD, "INTEGER NOT NULL DEFAULT 0")
            createSuppliersTable(db)
            createAuditTable(db)
            seedDefaultAdminIfNeeded(db)
        }
    }

    private fun createMovementsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $MOVEMENT_TABLE_NAME (
                $MOV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MOV_URUN_ID INTEGER NOT NULL,
                $MOV_URUN_AD TEXT NOT NULL,
                $MOV_URUN_KOD TEXT NOT NULL,
                $MOV_TIPI TEXT NOT NULL,
                $MOV_MIKTAR INTEGER NOT NULL,
                $MOV_YENI_ADET INTEGER NOT NULL,
                $MOV_NOT TEXT NOT NULL DEFAULT '',
                $MOV_KULLANICI TEXT NOT NULL DEFAULT '',
                $MOV_TARIH INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createSuppliersTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $SUPPLIER_TABLE_NAME (
                $SUP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $SUP_FIRMA TEXT NOT NULL UNIQUE,
                $SUP_YETKILI TEXT NOT NULL DEFAULT '',
                $SUP_TELEFON TEXT NOT NULL DEFAULT '',
                $SUP_EPOSTA TEXT NOT NULL DEFAULT '',
                $SUP_VERGI_NO TEXT NOT NULL DEFAULT '',
                $SUP_NOTLAR TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    private fun createAuditTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $AUDIT_TABLE_NAME (
                $AUD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $AUD_OLAY TEXT NOT NULL,
                $AUD_KULLANICI TEXT NOT NULL,
                $AUD_DETAY TEXT NOT NULL DEFAULT '',
                $AUD_BASARILI INTEGER NOT NULL DEFAULT 1,
                $AUD_TARIH INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun addColumnIfMissing(
        db: SQLiteDatabase,
        table: String,
        column: String,
        definition: String
    ) {
        val existingColumns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIdx >= 0) {
                    existingColumns.add(cursor.getString(nameIdx))
                }
            }
        }
        if (column !in existingColumns) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }

    private fun seedDefaultAdminIfNeeded(db: SQLiteDatabase) {
        db.query(
            USER_TABLE_NAME,
            arrayOf(USER_USERNAME),
            "$USER_ROLE = ?",
            arrayOf(UserRole.ADMIN.name),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) return
        }

        val values = ContentValues().apply {
            put(USER_USERNAME, DEFAULT_ADMIN_USERNAME)
            put(USER_PASSWORD, PasswordHasher.hash(DEFAULT_ADMIN_INITIAL_SECRET))
            put(USER_EMAIL, "admin@estocknoted.local")
            put(USER_BUSINESS, "Yönetim")
            put(USER_ROLE, UserRole.ADMIN.name)
            put(USER_IS_ACTIVE, 1)
            put(USER_MUST_CHANGE_PWD, 1)
            put(USER_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(USER_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // --- Products ---

    fun urunKoduMevcutMu(kod: String, excludeId: Long? = null): Boolean {
        val trimmedKod = kod.trim()
        val selection = if (excludeId != null) {
            "LOWER($STOK_COLUMN_KOD) = LOWER(?) AND $STOK_COLUMN_ID != ?"
        } else {
            "LOWER($STOK_COLUMN_KOD) = LOWER(?)"
        }
        val args = if (excludeId != null) {
            arrayOf(trimmedKod, excludeId.toString())
        } else {
            arrayOf(trimmedKod)
        }
        readableDatabase.query(
            STOK_TABLE_NAME,
            arrayOf(STOK_COLUMN_ID),
            selection,
            args,
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun urunKaydet(
        ad: String,
        kod: String,
        kategori: String,
        adet: Int,
        minStok: Int,
        birimFiyat: Double,
        aciklama: String,
        yapanKullanici: String,
        hareketNotu: String,
        alisFiyati: Double = 0.0,
        kdvOrani: Int = UrunModel.DEFAULT_VAT_RATE,
        tedarikci: String = "",
        depoKonum: String = UrunModel.DEFAULT_WAREHOUSE
    ): Long {
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(STOK_COLUMN_AD, ad.trim())
                put(STOK_COLUMN_KOD, kod.trim())
                put(STOK_COLUMN_KATEGORI, kategori.trim().ifEmpty { UrunModel.DEFAULT_CATEGORY })
                put(STOK_COLUMN_ADET, adet)
                put(STOK_COLUMN_MIN_STOK, minStok)
                put(STOK_COLUMN_BIRIM_FIYAT, birimFiyat)
                put(STOK_COLUMN_ALIS_FIYATI, alisFiyati)
                put(STOK_COLUMN_KDV_ORANI, kdvOrani)
                put(STOK_COLUMN_TEDARIKCI, tedarikci.trim())
                put(STOK_COLUMN_DEPO_KONUM, depoKonum.trim().ifEmpty { UrunModel.DEFAULT_WAREHOUSE })
                put(STOK_COLUMN_ACIKLAMA, aciklama.trim())
                put(STOK_COLUMN_GUNCELLEME_TARIHI, now)
            }
            val insertedId = db.insert(STOK_TABLE_NAME, null, values)
            if (insertedId > 0) {
                insertMovement(
                    db = db,
                    urunId = insertedId,
                    urunAdi = ad.trim(),
                    urunKodu = kod.trim(),
                    tip = HareketTipi.GIRIS,
                    miktarDegisimi = adet,
                    yeniAdet = adet,
                    not = hareketNotu,
                    yapanKullanici = yapanKullanici,
                    tarih = now
                )
                db.setTransactionSuccessful()
            }
            insertedId
        } finally {
            db.endTransaction()
        }
    }

    fun urunGetir(): List<UrunModel> {
        val liste = mutableListOf<UrunModel>()
        readableDatabase.query(
            STOK_TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$STOK_COLUMN_GUNCELLEME_TARIHI DESC, $STOK_COLUMN_ID DESC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_ID)
            val adIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_AD)
            val kodIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_KOD)
            val katIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_KATEGORI)
            val adetIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_ADET)
            val minIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_MIN_STOK)
            val fiyatIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_BIRIM_FIYAT)
            val alisIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_ALIS_FIYATI)
            val kdvIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_KDV_ORANI)
            val tedIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_TEDARIKCI)
            val depoIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_DEPO_KONUM)
            val aciklamaIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_ACIKLAMA)
            val tarihIdx = cursor.getColumnIndexOrThrow(STOK_COLUMN_GUNCELLEME_TARIHI)

            while (cursor.moveToNext()) {
                liste.add(
                    UrunModel(
                        id = cursor.getLong(idIdx),
                        ad = cursor.getString(adIdx) ?: "",
                        kod = cursor.getString(kodIdx) ?: "",
                        kategori = cursor.getString(katIdx) ?: UrunModel.DEFAULT_CATEGORY,
                        adet = cursor.getInt(adetIdx),
                        minStok = cursor.getInt(minIdx),
                        birimFiyat = cursor.getDouble(fiyatIdx),
                        alisFiyati = cursor.getDouble(alisIdx),
                        kdvOrani = cursor.getInt(kdvIdx),
                        tedarikci = cursor.getString(tedIdx) ?: "",
                        depoKonum = cursor.getString(depoIdx) ?: UrunModel.DEFAULT_WAREHOUSE,
                        aciklama = cursor.getString(aciklamaIdx) ?: "",
                        guncellemeTarihi = cursor.getLong(tarihIdx)
                    )
                )
            }
        }
        return liste
    }

    fun updateUrun(
        secilenUrun: UrunModel,
        yeniUrunKod: String,
        yeniUrunAd: String,
        yeniKategori: String,
        yeniUrunAdet: Int,
        yeniMinStok: Int,
        yeniBirimFiyat: Double,
        yeniUrunAciklama: String,
        yapanKullanici: String,
        hareketNotu: String,
        yeniAlisFiyati: Double = secilenUrun.alisFiyati,
        yeniKdvOrani: Int = secilenUrun.kdvOrani,
        yeniTedarikci: String = secilenUrun.tedarikci,
        yeniDepoKonum: String = secilenUrun.depoKonum
    ): Int {
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(STOK_COLUMN_KOD, yeniUrunKod.trim())
                put(STOK_COLUMN_AD, yeniUrunAd.trim())
                put(STOK_COLUMN_KATEGORI, yeniKategori.trim().ifEmpty { UrunModel.DEFAULT_CATEGORY })
                put(STOK_COLUMN_ADET, yeniUrunAdet)
                put(STOK_COLUMN_MIN_STOK, yeniMinStok)
                put(STOK_COLUMN_BIRIM_FIYAT, yeniBirimFiyat)
                put(STOK_COLUMN_ALIS_FIYATI, yeniAlisFiyati)
                put(STOK_COLUMN_KDV_ORANI, yeniKdvOrani)
                put(STOK_COLUMN_TEDARIKCI, yeniTedarikci.trim())
                put(STOK_COLUMN_DEPO_KONUM, yeniDepoKonum.trim().ifEmpty { UrunModel.DEFAULT_WAREHOUSE })
                put(STOK_COLUMN_ACIKLAMA, yeniUrunAciklama.trim())
                put(STOK_COLUMN_GUNCELLEME_TARIHI, now)
            }
            val rows = db.update(
                STOK_TABLE_NAME,
                values,
                "$STOK_COLUMN_ID = ?",
                arrayOf(secilenUrun.id.toString())
            )
            if (rows > 0) {
                val delta = yeniUrunAdet - secilenUrun.adet
                insertMovement(
                    db = db,
                    urunId = secilenUrun.id,
                    urunAdi = yeniUrunAd.trim(),
                    urunKodu = yeniUrunKod.trim(),
                    tip = HareketTipi.GUNCELLEME,
                    miktarDegisimi = delta,
                    yeniAdet = yeniUrunAdet,
                    not = hareketNotu,
                    yapanKullanici = yapanKullanici,
                    tarih = now
                )
                db.setTransactionSuccessful()
            }
            rows
        } finally {
            db.endTransaction()
        }
    }

    fun stokMiktarGuncelle(
        urun: UrunModel,
        delta: Int,
        not: String,
        yapanKullanici: String
    ): Boolean {
        val yeniAdet = SecuritySanitizer.safeAddQuantity(urun.adet, delta) ?: return false

        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(STOK_COLUMN_ADET, yeniAdet)
                put(STOK_COLUMN_GUNCELLEME_TARIHI, now)
            }
            val updated = db.update(
                STOK_TABLE_NAME,
                values,
                "$STOK_COLUMN_ID = ?",
                arrayOf(urun.id.toString())
            )
            if (updated > 0) {
                insertMovement(
                    db = db,
                    urunId = urun.id,
                    urunAdi = urun.ad,
                    urunKodu = urun.kod,
                    tip = if (delta >= 0) HareketTipi.GIRIS else HareketTipi.CIKIS,
                    miktarDegisimi = delta,
                    yeniAdet = yeniAdet,
                    not = not,
                    yapanKullanici = yapanKullanici,
                    tarih = now
                )
                db.setTransactionSuccessful()
                true
            } else {
                false
            }
        } finally {
            db.endTransaction()
        }
    }

    fun deleteUrun(
        secilenUrun: UrunModel,
        yapanKullanici: String,
        hareketNotu: String
    ): Int {
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val rows = db.delete(
                STOK_TABLE_NAME,
                "$STOK_COLUMN_ID = ?",
                arrayOf(secilenUrun.id.toString())
            )
            if (rows > 0) {
                insertMovement(
                    db = db,
                    urunId = secilenUrun.id,
                    urunAdi = secilenUrun.ad,
                    urunKodu = secilenUrun.kod,
                    tip = HareketTipi.SILME,
                    miktarDegisimi = -secilenUrun.adet,
                    yeniAdet = 0,
                    not = hareketNotu,
                    yapanKullanici = yapanKullanici,
                    tarih = now
                )
                db.setTransactionSuccessful()
            }
            rows
        } finally {
            db.endTransaction()
        }
    }

    // --- Movements ---

    fun hareketleriGetir(limit: Int = 300): List<StokHareketModel> {
        val list = mutableListOf<StokHareketModel>()
        readableDatabase.query(
            MOVEMENT_TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$MOV_TARIH DESC, $MOV_ID DESC",
            limit.toString()
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MOV_ID)
            val urunIdIdx = cursor.getColumnIndexOrThrow(MOV_URUN_ID)
            val urunAdIdx = cursor.getColumnIndexOrThrow(MOV_URUN_AD)
            val urunKodIdx = cursor.getColumnIndexOrThrow(MOV_URUN_KOD)
            val tipIdx = cursor.getColumnIndexOrThrow(MOV_TIPI)
            val miktarIdx = cursor.getColumnIndexOrThrow(MOV_MIKTAR)
            val yeniAdetIdx = cursor.getColumnIndexOrThrow(MOV_YENI_ADET)
            val notIdx = cursor.getColumnIndexOrThrow(MOV_NOT)
            val kulIdx = cursor.getColumnIndexOrThrow(MOV_KULLANICI)
            val tarihIdx = cursor.getColumnIndexOrThrow(MOV_TARIH)

            while (cursor.moveToNext()) {
                list.add(
                    StokHareketModel(
                        id = cursor.getLong(idIdx),
                        urunId = cursor.getLong(urunIdIdx),
                        urunAdi = cursor.getString(urunAdIdx) ?: "",
                        urunKodu = cursor.getString(urunKodIdx) ?: "",
                        hareketTipi = HareketTipi.fromString(cursor.getString(tipIdx)),
                        miktarDegisimi = cursor.getInt(miktarIdx),
                        yeniAdet = cursor.getInt(yeniAdetIdx),
                        not = cursor.getString(notIdx) ?: "",
                        yapanKullanici = cursor.getString(kulIdx) ?: "",
                        tarih = cursor.getLong(tarihIdx)
                    )
                )
            }
        }
        return list
    }

    private fun insertMovement(
        db: SQLiteDatabase,
        urunId: Long,
        urunAdi: String,
        urunKodu: String,
        tip: HareketTipi,
        miktarDegisimi: Int,
        yeniAdet: Int,
        not: String,
        yapanKullanici: String,
        tarih: Long
    ) {
        val values = ContentValues().apply {
            put(MOV_URUN_ID, urunId)
            put(MOV_URUN_AD, urunAdi)
            put(MOV_URUN_KOD, urunKodu)
            put(MOV_TIPI, tip.name)
            put(MOV_MIKTAR, miktarDegisimi)
            put(MOV_YENI_ADET, yeniAdet)
            put(MOV_NOT, not)
            put(MOV_KULLANICI, yapanKullanici)
            put(MOV_TARIH, tarih)
        }
        db.insert(MOVEMENT_TABLE_NAME, null, values)
    }

    // --- Suppliers ---

    fun tedarikciEkle(
        firmaAdi: String,
        yetkiliKisi: String,
        telefon: String,
        eposta: String,
        vergiNo: String,
        notlar: String
    ): Long {
        val values = ContentValues().apply {
            put(SUP_FIRMA, firmaAdi.trim())
            put(SUP_YETKILI, yetkiliKisi.trim())
            put(SUP_TELEFON, telefon.trim())
            put(SUP_EPOSTA, eposta.trim())
            put(SUP_VERGI_NO, vergiNo.trim())
            put(SUP_NOTLAR, notlar.trim())
        }
        return writableDatabase.insertWithOnConflict(
            SUPPLIER_TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun tedarikcileriGetir(): List<TedarikciModel> {
        val list = mutableListOf<TedarikciModel>()
        readableDatabase.query(
            SUPPLIER_TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$SUP_FIRMA COLLATE NOCASE ASC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(SUP_ID)
            val firmaIdx = cursor.getColumnIndexOrThrow(SUP_FIRMA)
            val yetkiliIdx = cursor.getColumnIndexOrThrow(SUP_YETKILI)
            val telIdx = cursor.getColumnIndexOrThrow(SUP_TELEFON)
            val mailIdx = cursor.getColumnIndexOrThrow(SUP_EPOSTA)
            val vergiIdx = cursor.getColumnIndexOrThrow(SUP_VERGI_NO)
            val notIdx = cursor.getColumnIndexOrThrow(SUP_NOTLAR)

            while (cursor.moveToNext()) {
                list.add(
                    TedarikciModel(
                        id = cursor.getLong(idIdx),
                        firmaAdi = cursor.getString(firmaIdx) ?: "",
                        yetkiliKisi = cursor.getString(yetkiliIdx) ?: "",
                        telefon = cursor.getString(telIdx) ?: "",
                        eposta = cursor.getString(mailIdx) ?: "",
                        vergiNo = cursor.getString(vergiIdx) ?: "",
                        notlar = cursor.getString(notIdx) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun tedarikciSil(id: Long): Int {
        return writableDatabase.delete(
            SUPPLIER_TABLE_NAME,
            "$SUP_ID = ?",
            arrayOf(id.toString())
        )
    }

    // --- Users & Security Audit Log ---

    fun userExists(username: String): Boolean {
        readableDatabase.query(
            USER_TABLE_NAME,
            arrayOf(USER_USERNAME),
            "LOWER($USER_USERNAME) = LOWER(?)",
            arrayOf(username.trim()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun addUser(
        username: String,
        password: String,
        email: String,
        business: String,
        role: UserRole = UserRole.USER,
        mustChangePassword: Boolean = false
    ): Long {
        val values = ContentValues().apply {
            put(USER_USERNAME, username.trim())
            put(USER_PASSWORD, PasswordHasher.hash(password))
            put(USER_EMAIL, email.trim())
            put(USER_BUSINESS, business.trim())
            put(USER_ROLE, role.name)
            put(USER_IS_ACTIVE, 1)
            put(USER_MUST_CHANGE_PWD, if (mustChangePassword) 1 else 0)
            put(USER_CREATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(USER_TABLE_NAME, null, values)
    }

    fun kullanicilariGetir(): List<UserModel> {
        val db = writableDatabase
        seedDefaultAdminIfNeeded(db)
        val list = mutableListOf<UserModel>()
        db.query(
            USER_TABLE_NAME,
            arrayOf(USER_USERNAME, USER_EMAIL, USER_BUSINESS, USER_ROLE, USER_IS_ACTIVE, USER_MUST_CHANGE_PWD, USER_CREATED_AT),
            null,
            null,
            null,
            null,
            "$USER_ROLE ASC, $USER_USERNAME COLLATE NOCASE ASC"
        ).use { cursor ->
            val uIdx = cursor.getColumnIndexOrThrow(USER_USERNAME)
            val eIdx = cursor.getColumnIndexOrThrow(USER_EMAIL)
            val bIdx = cursor.getColumnIndexOrThrow(USER_BUSINESS)
            val rIdx = cursor.getColumnIndexOrThrow(USER_ROLE)
            val aIdx = cursor.getColumnIndexOrThrow(USER_IS_ACTIVE)
            val mIdx = cursor.getColumnIndexOrThrow(USER_MUST_CHANGE_PWD)
            val cIdx = cursor.getColumnIndexOrThrow(USER_CREATED_AT)
            while (cursor.moveToNext()) {
                list.add(
                    UserModel(
                        username = cursor.getString(uIdx) ?: "",
                        email = cursor.getString(eIdx) ?: "",
                        business = cursor.getString(bIdx) ?: "",
                        role = UserRole.fromString(cursor.getString(rIdx)),
                        isActive = cursor.getInt(aIdx) != 0,
                        mustChangePassword = cursor.getInt(mIdx) != 0,
                        createdAt = cursor.getLong(cIdx)
                    )
                )
            }
        }
        return list
    }

    fun kullaniciDurumGuncelle(username: String, isActive: Boolean): Boolean {
        val values = ContentValues().apply {
            put(USER_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return writableDatabase.update(
            USER_TABLE_NAME,
            values,
            "$USER_USERNAME = ?",
            arrayOf(username.trim())
        ) > 0
    }

    fun kullaniciRolGuncelle(username: String, newRole: UserRole): Boolean {
        val values = ContentValues().apply {
            put(USER_ROLE, newRole.name)
        }
        return writableDatabase.update(
            USER_TABLE_NAME,
            values,
            "$USER_USERNAME = ?",
            arrayOf(username.trim())
        ) > 0
    }

    fun authenticateUser(
        username: String,
        password: String,
        allowedRoles: Set<UserRole>? = null
    ): UserModel? {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty() || password.isEmpty()) return null

        val db = writableDatabase
        seedDefaultAdminIfNeeded(db)

        db.query(
            USER_TABLE_NAME,
            arrayOf(
                USER_USERNAME,
                USER_PASSWORD,
                USER_EMAIL,
                USER_BUSINESS,
                USER_ROLE,
                USER_IS_ACTIVE,
                USER_MUST_CHANGE_PWD,
                USER_CREATED_AT
            ),
            "$USER_USERNAME = ?",
            arrayOf(trimmedUser),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            val isActive = cursor.getInt(cursor.getColumnIndexOrThrow(USER_IS_ACTIVE)) != 0
            if (!isActive) return null

            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)) ?: ""
            if (!PasswordHasher.verify(password, storedPassword)) return null

            val role = UserRole.fromString(cursor.getString(cursor.getColumnIndexOrThrow(USER_ROLE)))
            if (allowedRoles != null && role !in allowedRoles) return null

            // Upgrade legacy plaintext or lower-iteration hashes transparently
            if (PasswordHasher.needsRehash(storedPassword)) {
                val upgradeValues = ContentValues().apply {
                    put(USER_PASSWORD, PasswordHasher.hash(password))
                }
                db.update(
                    USER_TABLE_NAME,
                    upgradeValues,
                    "$USER_USERNAME = ?",
                    arrayOf(trimmedUser)
                )
            }

            return UserModel(
                username = cursor.getString(cursor.getColumnIndexOrThrow(USER_USERNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(USER_EMAIL)) ?: "",
                business = cursor.getString(cursor.getColumnIndexOrThrow(USER_BUSINESS)) ?: "",
                role = role,
                isActive = true,
                mustChangePassword = cursor.getInt(cursor.getColumnIndexOrThrow(USER_MUST_CHANGE_PWD)) != 0,
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(USER_CREATED_AT))
            )
        }
    }

    fun changePassword(username: String, currentPassword: String, newPassword: String): Boolean {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty() || currentPassword.isEmpty() || newPassword.isEmpty()) return false

        val db = writableDatabase
        db.query(
            USER_TABLE_NAME,
            arrayOf(USER_PASSWORD),
            "$USER_USERNAME = ?",
            arrayOf(trimmedUser),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)) ?: ""
            if (!PasswordHasher.verify(currentPassword, storedPassword)) return false
        }

        val values = ContentValues().apply {
            put(USER_PASSWORD, PasswordHasher.hash(newPassword))
            put(USER_MUST_CHANGE_PWD, 0)
        }
        return db.update(
            USER_TABLE_NAME,
            values,
            "$USER_USERNAME = ?",
            arrayOf(trimmedUser)
        ) > 0
    }

    fun guvenlikKaydiEkle(
        olayTipi: String,
        kullaniciAdi: String,
        detay: String,
        basarili: Boolean
    ) {
        val values = ContentValues().apply {
            put(AUD_OLAY, SecuritySanitizer.sanitizeText(olayTipi, 60))
            put(AUD_KULLANICI, SecuritySanitizer.sanitizeText(kullaniciAdi, 60).ifEmpty { "-" })
            put(AUD_DETAY, SecuritySanitizer.sanitizeText(detay, 250))
            put(AUD_BASARILI, if (basarili) 1 else 0)
            put(AUD_TARIH, System.currentTimeMillis())
        }
        writableDatabase.insert(AUDIT_TABLE_NAME, null, values)
    }

    fun guvenlikKayitlariniGetir(limit: Int = 150): List<SecurityAuditLogModel> {
        val list = mutableListOf<SecurityAuditLogModel>()
        readableDatabase.query(
            AUDIT_TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$AUD_TARIH DESC, $AUD_ID DESC",
            limit.toString()
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(AUD_ID)
            val olayIdx = cursor.getColumnIndexOrThrow(AUD_OLAY)
            val kulIdx = cursor.getColumnIndexOrThrow(AUD_KULLANICI)
            val detIdx = cursor.getColumnIndexOrThrow(AUD_DETAY)
            val basIdx = cursor.getColumnIndexOrThrow(AUD_BASARILI)
            val tarIdx = cursor.getColumnIndexOrThrow(AUD_TARIH)

            while (cursor.moveToNext()) {
                list.add(
                    SecurityAuditLogModel(
                        id = cursor.getLong(idIdx),
                        olayTipi = cursor.getString(olayIdx) ?: "",
                        kullaniciAdi = cursor.getString(kulIdx) ?: "",
                        detay = cursor.getString(detIdx) ?: "",
                        basarili = cursor.getInt(basIdx) != 0,
                        tarih = cursor.getLong(tarIdx)
                    )
                )
            }
        }
        return list
    }

    fun yedektenGeriYukle(payload: BackupPayload, yapanKullanici: String): Int {
        var importedCount = 0
        for (s in payload.suppliers) {
            tedarikciEkle(s.firmaAdi, s.yetkiliKisi, s.telefon, s.eposta, s.vergiNo, s.notlar)
        }
        for (p in payload.products) {
            if (!urunKoduMevcutMu(p.kod)) {
                val id = urunKaydet(
                    ad = p.ad,
                    kod = p.kod,
                    kategori = p.kategori,
                    adet = p.adet,
                    minStok = p.minStok,
                    birimFiyat = p.birimFiyat,
                    aciklama = p.aciklama,
                    yapanKullanici = yapanKullanici,
                    hareketNotu = "JSON yedekten içe aktarıldı",
                    alisFiyati = p.alisFiyati,
                    kdvOrani = p.kdvOrani,
                    tedarikci = p.tedarikci,
                    depoKonum = p.depoKonum
                )
                if (id > 0) importedCount++
            }
        }
        return importedCount
    }
}