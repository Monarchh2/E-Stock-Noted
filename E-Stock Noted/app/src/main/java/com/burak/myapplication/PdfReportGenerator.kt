package com.burak.myapplication

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a multi-page A4 commercial PDF inventory & profitability report using Android PdfDocument.
 */
object PdfReportGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateInventoryReport(
        outputFile: File,
        items: List<UrunModel>,
        generatedBy: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val document = PdfDocument()
        val summary = StockAnalytics.computeSummary(items)

        val headerBgPaint = Paint().apply { color = Color.parseColor("#053262") }
        val cardBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 10f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#053262")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 9f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        // Header banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 78f, headerBgPaint)
        canvas.drawText("E-STOCK NOTED — TİCARİ ENVANTER RAPORU", MARGIN, 36f, titlePaint)
        canvas.drawText(
            "Oluşturan: $generatedBy  •  Tarih: ${StockAnalytics.formatDateTime(nowMillis)}",
            MARGIN,
            58f,
            subtitlePaint
        )

        var y = 100f

        // Financial KPI Summary Box
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 84f, 8f, 8f, cardBgPaint)
        canvas.drawText("FİNANSAL & STOK ÖZETİ", MARGIN + 12f, y + 20f, sectionPaint)
        canvas.drawText(
            "Ürün Çeşidi: ${summary.toplamUrunCesidi}   |   Toplam Adet: ${summary.toplamAdet}   |   Kritik/Tükenen: ${summary.toplamUyariSayisi}",
            MARGIN + 12f,
            y + 42f,
            boldPaint
        )
        canvas.drawText(
            "Toplam Maliyet: ${StockAnalytics.formatCurrency(summary.toplamMaliyet)}   |   Potansiyel Ciro: ${StockAnalytics.formatCurrency(summary.toplamDeger)}",
            MARGIN + 12f,
            y + 60f,
            bodyPaint
        )
        canvas.drawText(
            "Tahmini Brüt Kâr: ${StockAnalytics.formatCurrency(summary.tahminiToplamKar)}   (Ort. Marj: ${StockAnalytics.formatPercent(summary.ortalamaKarMarji)})",
            MARGIN + 12f,
            y + 76f,
            accentPaint
        )

        y += 108f

        fun drawTableHeader() {
            canvas.drawText("SKU", MARGIN, y, boldPaint)
            canvas.drawText("Ürün Adı", MARGIN + 70f, y, boldPaint)
            canvas.drawText("Kategori", MARGIN + 200f, y, boldPaint)
            canvas.drawText("Adet", MARGIN + 275f, y, boldPaint)
            canvas.drawText("Alış", MARGIN + 320f, y, boldPaint)
            canvas.drawText("Satış", MARGIN + 385f, y, boldPaint)
            canvas.drawText("Toplam Değer", MARGIN + 450f, y, boldPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += 14f
        }

        canvas.drawText("ÜRÜN LİSTESİ (${items.size} Kayıt)", MARGIN, y, sectionPaint)
        y += 18f
        drawTableHeader()

        for (item in items) {
            if (y > PAGE_HEIGHT - 60f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = 48f
                drawTableHeader()
            }

            canvas.drawText(truncate(item.kod, 11), MARGIN, y, bodyPaint)
            canvas.drawText(truncate(item.ad, 22), MARGIN + 70f, y, bodyPaint)
            canvas.drawText(truncate(item.kategori, 12), MARGIN + 200f, y, bodyPaint)
            canvas.drawText(item.adet.toString(), MARGIN + 275f, y, boldPaint)
            canvas.drawText(StockAnalytics.formatCurrency(item.alisFiyati), MARGIN + 320f, y, bodyPaint)
            canvas.drawText(StockAnalytics.formatCurrency(item.birimFiyat), MARGIN + 385f, y, bodyPaint)
            canvas.drawText(StockAnalytics.formatCurrency(item.toplamDeger), MARGIN + 450f, y, boldPaint)
            y += 16f
        }

        document.finishPage(page)
        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun truncate(text: String, maxChars: Int): String {
        return if (text.length <= maxChars) text else text.substring(0, maxChars - 1) + "…"
    }
}
