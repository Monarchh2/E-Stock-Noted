package com.burak.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "Stoklar"

        private const val STOK_TABLE_NAME = "stoklar"
        private const val STOK_COLUMN_ID = "id"
        private const val STOK_COLUMN_AD = "ad"
        private const val STOK_COLUMN_KOD = "kod"
        private const val STOK_COLUMN_KATEGORI = "kategori"
        private const val STOK_COLUMN_ADET = "adet"
        private const val STOK_COLUMN_MIN_STOK = "min_stok"
        private const val STOK_COLUMN_BIRIM_FIYAT = "birim_fiyat"
        private const val STOK_COLUMN_ACIKLAMA = "aciklama"
        private const val STOK_COLUMN_GUNCELLEME_TARIHI = "guncelleme_tarihi"

        private const val USER_TABLE_NAME = "kullanici"
        private const val USER_USERNAME = "username"
        private const val USER_PASSWORD = "password"
        private const val USER_EMAIL = "email"
        private const val USER_BUSINESS = "business"
        private const val USER_ROLE = "role"
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
                $USER_CREATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createUserTable)

        createMovementsTable(db)
        seedDefaultAdminIfNeeded(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            addColumnIfMissing(
                db, STOK_TABLE_NAME, STOK_COLUMN_KATEGORI,
                "TEXT NOT NULL DEFAULT '${UrunModel.DEFAULT_CATEGORY}'"
            )
            addColumnIfMissing(
                db, STOK_TABLE_NAME, STOK_COLUMN_MIN_STOK,
                "INTEGER NOT NULL DEFAULT ${UrunModel.DEFAULT_MIN_STOCK}"
            )
            addColumnIfMissing(
                db, STOK_TABLE_NAME, STOK_COLUMN_BIRIM_FIYAT,
                "REAL NOT NULL DEFAULT 0.0"
            )
            addColumnIfMissing(
                db, STOK_TABLE_NAME, STOK_COLUMN_GUNCELLEME_TARIHI,
                "INTEGER NOT NULL DEFAULT 0"
            )
            addColumnIfMissing(
                db, USER_TABLE_NAME, USER_ROLE,
                "TEXT NOT NULL DEFAULT 'USER'"
            )
            addColumnIfMissing(
                db, USER_TABLE_NAME, USER_CREATED_AT,
                "INTEGER NOT NULL DEFAULT 0"
            )
            createMovementsTable(db)
            seedDefaultAdminIfNeeded(db)
        }
    }

    private fun createMovementsTable(db: SQLiteDatabase) {
        val sql = """
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
        db.execSQL(sql)
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
            put(USER_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(USER_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

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
        hareketNotu: String
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
        hareketNotu: String
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
        val yeniAdet = urun.adet + delta
        if (yeniAdet < 0) return false

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

    fun hareketleriGetir(limit: Int = 200): List<StokHareketModel> {
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
        role: UserRole = UserRole.USER
    ): Long {
        val values = ContentValues().apply {
            put(USER_USERNAME, username.trim())
            put(USER_PASSWORD, PasswordHasher.hash(password))
            put(USER_EMAIL, email.trim())
            put(USER_BUSINESS, business.trim())
            put(USER_ROLE, role.name)
            put(USER_CREATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(USER_TABLE_NAME, null, values)
    }

    fun authenticateUser(
        username: String,
        password: String,
        requiredRole: UserRole? = null
    ): UserModel? {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty() || password.isEmpty()) return null

        val db = writableDatabase
        seedDefaultAdminIfNeeded(db)

        db.query(
            USER_TABLE_NAME,
            arrayOf(USER_USERNAME, USER_PASSWORD, USER_EMAIL, USER_BUSINESS, USER_ROLE, USER_CREATED_AT),
            "$USER_USERNAME = ?",
            arrayOf(trimmedUser),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)) ?: ""
            if (!PasswordHasher.verify(password, storedPassword)) return null

            val role = UserRole.fromString(cursor.getString(cursor.getColumnIndexOrThrow(USER_ROLE)))
            if (requiredRole != null && role != requiredRole) return null

            // Upgrade legacy plaintext password to PBKDF2 hash transparently on login
            if (!PasswordHasher.isHashed(storedPassword)) {
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
        }
        return db.update(
            USER_TABLE_NAME,
            values,
            "$USER_USERNAME = ?",
            arrayOf(trimmedUser)
        ) > 0
    }
}