package com.burak.myapplication

import java.security.MessageDigest
import java.util.Locale

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val products: List<UrunModel>,
    val suppliers: List<TedarikciModel>
)

/**
 * Pure Kotlin JSON backup codec with SHA-256 tamper-detection checksum and strict schema bounds.
 */
object BackupJsonCodec {
    const val CURRENT_SCHEMA_VERSION = 3
    const val MAX_BACKUP_BYTES = 2 * 1024 * 1024 // 2 MB safety ceiling

    fun encode(
        products: List<UrunModel>,
        suppliers: List<TedarikciModel>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val dataJson = encodeBody(products, suppliers, exportedAt)
        val digestHex = sha256Hex(dataJson)
        return "{\n  \"integritySha256\": \"$digestHex\",\n$dataJson}\n"
    }

    private fun encodeBody(
        products: List<UrunModel>,
        suppliers: List<TedarikciModel>,
        exportedAt: Long
    ): String {
        val sb = StringBuilder()
        sb.append("  \"schemaVersion\": $CURRENT_SCHEMA_VERSION,\n")
        sb.append("  \"exportedAt\": $exportedAt,\n")
        sb.append("  \"products\": [\n")
        products.forEachIndexed { idx, p ->
            sb.append("    {")
            sb.append("\"ad\": ${quote(p.ad)}, ")
            sb.append("\"kod\": ${quote(p.kod)}, ")
            sb.append("\"kategori\": ${quote(p.kategori)}, ")
            sb.append("\"adet\": ${p.adet}, ")
            sb.append("\"minStok\": ${p.minStok}, ")
            sb.append("\"birimFiyat\": ${String.format(Locale.US, "%.2f", p.birimFiyat)}, ")
            sb.append("\"alisFiyati\": ${String.format(Locale.US, "%.2f", p.alisFiyati)}, ")
            sb.append("\"kdvOrani\": ${p.kdvOrani}, ")
            sb.append("\"tedarikci\": ${quote(p.tedarikci)}, ")
            sb.append("\"depoKonum\": ${quote(p.depoKonum)}, ")
            sb.append("\"aciklama\": ${quote(p.aciklama)}")
            sb.append("}")
            if (idx < products.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"suppliers\": [\n")
        suppliers.forEachIndexed { idx, s ->
            sb.append("    {")
            sb.append("\"firmaAdi\": ${quote(s.firmaAdi)}, ")
            sb.append("\"yetkiliKisi\": ${quote(s.yetkiliKisi)}, ")
            sb.append("\"telefon\": ${quote(s.telefon)}, ")
            sb.append("\"eposta\": ${quote(s.eposta)}, ")
            sb.append("\"vergiNo\": ${quote(s.vergiNo)}, ")
            sb.append("\"notlar\": ${quote(s.notlar)}")
            sb.append("}")
            if (idx < suppliers.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        return sb.toString()
    }

    fun decode(json: String): BackupPayload? {
        if (json.isBlank() || json.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            return null
        }
        return try {
            val root = MiniJsonParser(json).parseValue() as? Map<*, *> ?: return null
            val schemaVersion = (root["schemaVersion"] as? Number)?.toInt() ?: return null
            if (schemaVersion !in 1..CURRENT_SCHEMA_VERSION) return null
            val exportedAt = (root["exportedAt"] as? Number)?.toLong() ?: 0L

            val rawProducts = root["products"] as? List<*> ?: return null
            val products = mutableListOf<UrunModel>()
            for (entry in rawProducts) {
                val obj = entry as? Map<*, *> ?: return null
                val ad = SecuritySanitizer.sanitizeText(obj["ad"] as? String ?: return null)
                val kod = SecuritySanitizer.sanitizeText(obj["kod"] as? String ?: return null, 60)
                if (ad.isEmpty() || kod.isEmpty()) return null

                val adet = (obj["adet"] as? Number)?.toInt() ?: return null
                val minStok = (obj["minStok"] as? Number)?.toInt() ?: UrunModel.DEFAULT_MIN_STOCK
                val birimFiyat = (obj["birimFiyat"] as? Number)?.toDouble() ?: 0.0
                val alisFiyati = (obj["alisFiyati"] as? Number)?.toDouble() ?: 0.0
                val kdvOrani = (obj["kdvOrani"] as? Number)?.toInt() ?: UrunModel.DEFAULT_VAT_RATE
                if (adet < 0 || minStok < 0 || birimFiyat < 0.0 || alisFiyati < 0.0 || kdvOrani !in 0..100) {
                    return null
                }

                products.add(
                    UrunModel(
                        id = 0L,
                        ad = ad,
                        kod = kod,
                        kategori = SecuritySanitizer.sanitizeText(
                            obj["kategori"] as? String ?: UrunModel.DEFAULT_CATEGORY,
                            60
                        ).ifEmpty { UrunModel.DEFAULT_CATEGORY },
                        adet = adet,
                        minStok = minStok,
                        birimFiyat = birimFiyat,
                        alisFiyati = alisFiyati,
                        kdvOrani = kdvOrani,
                        tedarikci = SecuritySanitizer.sanitizeText(obj["tedarikci"] as? String ?: "", 80),
                        depoKonum = SecuritySanitizer.sanitizeText(
                            obj["depoKonum"] as? String ?: UrunModel.DEFAULT_WAREHOUSE,
                            60
                        ).ifEmpty { UrunModel.DEFAULT_WAREHOUSE },
                        aciklama = SecuritySanitizer.sanitizeText(
                            obj["aciklama"] as? String ?: "",
                            SecuritySanitizer.MAX_LONG_TEXT
                        )
                    )
                )
            }

            val rawSuppliers = root["suppliers"] as? List<*> ?: emptyList<Any>()
            val suppliers = mutableListOf<TedarikciModel>()
            for (entry in rawSuppliers) {
                val obj = entry as? Map<*, *> ?: return null
                val firma = SecuritySanitizer.sanitizeText(obj["firmaAdi"] as? String ?: return null, 100)
                if (firma.isEmpty()) return null
                suppliers.add(
                    TedarikciModel(
                        id = 0L,
                        firmaAdi = firma,
                        yetkiliKisi = SecuritySanitizer.sanitizeText(obj["yetkiliKisi"] as? String ?: "", 80),
                        telefon = SecuritySanitizer.sanitizeText(obj["telefon"] as? String ?: "", 30),
                        eposta = SecuritySanitizer.sanitizeText(obj["eposta"] as? String ?: "", 120),
                        vergiNo = SecuritySanitizer.sanitizeText(obj["vergiNo"] as? String ?: "", 30),
                        notlar = SecuritySanitizer.sanitizeText(obj["notlar"] as? String ?: "", 250)
                    )
                )
            }

            // Verify SHA-256 tamper checksum when present (required for schemaVersion >= 3)
            val storedDigest = root["integritySha256"] as? String
            if (schemaVersion >= 3) {
                if (storedDigest.isNullOrBlank()) return null
                val recomputed = sha256Hex(encodeBody(products, suppliers, exportedAt))
                val matches = MessageDigest.isEqual(
                    storedDigest.toByteArray(Charsets.UTF_8),
                    recomputed.toByteArray(Charsets.UTF_8)
                )
                if (!matches) return null
            }

            BackupPayload(schemaVersion, exportedAt, products, suppliers)
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256Hex(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun quote(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class MiniJsonParser(private val text: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= text.length) throw IllegalArgumentException("Unexpected EOF")
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < text.length) {
                val ch = text[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= text.length) throw IllegalArgumentException("Unterminated escape")
                        when (val esc = text[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = text.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Bad escape \\$esc")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < text.length && text[pos] in '0'..'9') pos++
            var isDouble = false
            if (pos < text.length && text[pos] == '.') {
                isDouble = true
                pos++
                while (pos < text.length && text[pos] in '0'..'9') pos++
            }
            val token = text.substring(start, pos)
            return if (isDouble) token.toDouble() else token.toLong()
        }

        private fun parseBoolean(): Boolean {
            return if (text.startsWith("true", pos)) {
                pos += 4
                true
            } else if (text.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at $pos")
            }
        }

        private fun parseNull(): Any? {
            if (text.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at $pos")
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < text.length) text[pos] else '\u0000'

        private fun expect(ch: Char) {
            if (pos >= text.length || text[pos] != ch) {
                throw IllegalArgumentException("Expected '$ch' at $pos")
            }
            pos++
        }
    }
}
