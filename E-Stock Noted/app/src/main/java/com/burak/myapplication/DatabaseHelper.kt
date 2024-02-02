package com.burak.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Stoklar"


        private const val STOK_TABLE_NAME = "stoklar"
        private const val STOK_COLUMN_ID = "id"
        private const val STOK_COLUMN_AD = "ad"
        private const val STOK_COLUMN_KOD = "kod"
        private const val STOK_COLUMN_ADET = "adet"
        private const val STOK_COLUMN_ACIKLAMA = "aciklama"

        private const val USER_TABLE_NAME = "kullanici"
        private const val USER_USERNAME = "username"
        private const val USER_PASSWORD = "password"
        private const val USER_EMAIL = "email"
        private const val USER_BUSINESS = "business"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_STOKLAR_TABLE = ("CREATE TABLE IF NOT EXISTS $STOK_TABLE_NAME ($STOK_COLUMN_ID INTEGER PRIMARY KEY, " +
                "$STOK_COLUMN_AD TEXT, $STOK_COLUMN_KOD TEXT, $STOK_COLUMN_ADET INTEGER, $STOK_COLUMN_ACIKLAMA TEXT)")
        db.execSQL(CREATE_STOKLAR_TABLE)

        val CREATE_USER_TABLE = ("CREATE TABLE IF NOT EXISTS $USER_TABLE_NAME (" +
                "$USER_USERNAME TEXT PRIMARY KEY, " +
                "$USER_PASSWORD TEXT, " +
                "$USER_BUSINESS TEXT, " +
                "$USER_EMAIL TEXT)")

        db.execSQL(CREATE_USER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $STOK_TABLE_NAME")
        onCreate(db)
    }

    fun urunKaydet(ad: String, kod: String, adet: Int, aciklama: String) {
        val database = this.writableDatabase
        val values = ContentValues()
        values.put("ad", ad)
        values.put("kod", kod)
        values.put("adet", adet)
        values.put("aciklama", aciklama)
        database.insert("stoklar", null, values)
    }

    fun urunKontrol(urunAdi: String, urunKodu: String): Boolean {
        val database = this.readableDatabase
        val cursor = database.rawQuery("SELECT * FROM stoklar", null)
        var durum: Boolean = false
        val ad = cursor.getColumnIndex("ad")
        val kod = cursor.getColumnIndex("kod")

        while (cursor.moveToNext()) {
            if (cursor.getString(ad) == urunAdi && cursor.getString(kod) == urunKodu) {
                durum = true
            }
        }
        cursor.close()
        return durum
    }

    fun urunGetir(): ArrayList<UrunModel>{
        val database = this.readableDatabase
        val cursor = database.rawQuery("SELECT * FROM ${STOK_TABLE_NAME}", null)
        val geriDondurulenListe = ArrayList<UrunModel>()
        while (cursor.moveToNext()) {
            val idI = cursor.getColumnIndex(STOK_COLUMN_ID)
            val adI = cursor.getColumnIndex(STOK_COLUMN_AD)
            val kodI = cursor.getColumnIndex(STOK_COLUMN_KOD)
            val adetI = cursor.getColumnIndex(STOK_COLUMN_ADET)
            val aciklamaI = cursor.getColumnIndex(STOK_COLUMN_ACIKLAMA)

            val id = cursor.getInt(idI)
            val ad = cursor.getString(adI)
            val kod = cursor.getInt(kodI)
            val adet = cursor.getInt(adetI)
            val aciklama = cursor.getString(aciklamaI)


            geriDondurulenListe.add(UrunModel(id,ad,kod,adet,aciklama))
        }
        cursor.close()

        return geriDondurulenListe
    }


    // Kullanıcı eklemek için
    fun addUser(username: String, password: String, email: String, business: String): Long {
        val database = this.writableDatabase

        val values = ContentValues()
        values.put(USER_USERNAME, username)
        values.put(USER_PASSWORD, password)
        values.put(USER_EMAIL, email)
        values.put(USER_BUSINESS, business)

        val id = database.insert(USER_TABLE_NAME, null, values)
        database.close()

        return id
    }

    fun updateUrun(secilenUrun: UrunModel, yeniUrunKod: Int, yeniUrunAd: String, yeniUrunAdet: Int, yeniUrunAciklama: String): Int {
        val database = this.writableDatabase
        val values = ContentValues()

        values.put(STOK_COLUMN_KOD, yeniUrunKod) // Kod sütununu String olarak güncelle

        values.put(STOK_COLUMN_AD, yeniUrunAd)
        values.put(STOK_COLUMN_ADET, yeniUrunAdet)
        values.put(STOK_COLUMN_ACIKLAMA, yeniUrunAciklama)

        // Güncelleme işlemini gerçekleştir
        val result = database.update(STOK_TABLE_NAME, values, "$STOK_COLUMN_ID = ?", arrayOf(secilenUrun.id.toString()))

        database.close()
        return result
    }

    fun deleteUrun(secilenUrun: UrunModel): Int {
        val database = this.writableDatabase

        // Silme işlemini gerçekleştir
        val result = database.delete(STOK_TABLE_NAME, "$STOK_COLUMN_ID = ?", arrayOf(secilenUrun.id.toString()))

        database.close()
        return result
    }

    fun login(username: String, password: String){
        val database = this.readableDatabase
        val cursor = database.rawQuery("SELECT * FROM ${USER_TABLE_NAME} WHERER ${USER_USERNAME} = ? AND ${USER_PASSWORD} = ?", null)
        val s = arrayOf(username,password)
    }

    // Kullanıcı kontrolü için
    fun checkUser(username: String, password: String): Boolean {
        val columns = arrayOf(USER_USERNAME)
        val db = this.readableDatabase
        val selection = "$USER_USERNAME = ? AND $USER_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)

        val cursor = db.query(
            USER_TABLE_NAME, columns, selection, selectionArgs,
            null, null, null
        )

        val cursorCount = cursor.count
        cursor.close()
        db.close()

        return cursorCount > 0
    }
}