package com.burak.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.burak.myapplication.databinding.ActivityRaporlarBinding
import java.io.File

class RaporlarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRaporlarBinding
    private lateinit var database: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    private var currentRange: RaporZamanAraligi = RaporZamanAraligi.SON_7_GUN
    private var products: List<UrunModel> = emptyList()
    private var movements: List<StokHareketModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityWindowHelper.applySecureFlag(this)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn() || sessionManager.isSessionExpired()) {
            sessionManager.clearSession()
            finish()
            return
        }
        sessionManager.touchActivity()

        binding = ActivityRaporlarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseHelper(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_report_pdf -> {
                    sharePdfReport()
                    true
                }
                R.id.action_report_csv -> {
                    shareCsvReport()
                    true
                }
                else -> false
            }
        }

        binding.chipGroupRange.setOnCheckedStateChangeListener { _, checkedIds ->
            currentRange = when (checkedIds.firstOrNull()) {
                R.id.chipRange30d -> RaporZamanAraligi.SON_30_GUN
                R.id.chipRangeAll -> RaporZamanAraligi.TUMU
                else -> RaporZamanAraligi.SON_7_GUN
            }
            renderCharts()
        }

        loadAndRender()
    }

    private fun loadAndRender() {
        products = database.urunGetir()
        movements = database.hareketleriGetir(limit = 500)
        renderCharts()
    }

    private fun renderCharts() {
        val summary = StockAnalytics.computeSummary(products)

        // Financial KPIs
        binding.tvRepCost.text = StockAnalytics.formatCurrency(summary.toplamMaliyet)
        binding.tvRepRevenue.text = StockAnalytics.formatCurrency(summary.toplamDeger)
        binding.tvRepProfit.text = StockAnalytics.formatCurrency(summary.tahminiToplamKar)
        binding.tvRepMargin.text = StockAnalytics.formatPercent(summary.ortalamaKarMarji)

        // Stock health stacked bar weights
        val okWeight = if (summary.toplamUrunCesidi == 0) 1f else summary.yeterliStokSayisi.toFloat()
        val lowWeight = summary.kritikStokSayisi.toFloat()
        val outWeight = summary.tukenenStokSayisi.toFloat()
        (binding.barHealthOk.layoutParams as LinearLayout.LayoutParams).weight = okWeight
        (binding.barHealthLow.layoutParams as LinearLayout.LayoutParams).weight = lowWeight
        (binding.barHealthOut.layoutParams as LinearLayout.LayoutParams).weight = outWeight
        binding.barHealthOk.requestLayout()
        binding.tvHealthSummary.text = getString(
            R.string.label_health_summary,
            summary.yeterliStokSayisi,
            summary.kritikStokSayisi,
            summary.tukenenStokSayisi
        )

        // Donut chart
        val catDist = StockAnalytics.computeCategoryDistribution(products)
        binding.donutChart.setData(
            data = catDist,
            centerPrimary = "${summary.toplamUrunCesidi} Ürün",
            centerSecondary = "${summary.toplamAdet} adet"
        )

        // Daily movement bar trend
        val trend = StockAnalytics.computeDailyMovementTrend(movements, currentRange)
        binding.barTrendChart.setData(trend)

        // Top 5 valued products (ABC analysis)
        val top5 = StockAnalytics.computeTopValuedProducts(products, 5)
        binding.rankingChart.setData(top5)

        // Auto-reorder list
        val reorderItems = StockAnalytics.computeReorderList(products)
        if (reorderItems.isEmpty()) {
            binding.tvReorderList.setText(R.string.empty_reorder_msg)
        } else {
            val lines = reorderItems.joinToString("\n") { item ->
                val supplierLabel = item.tedarikci.ifBlank { "-" }
                getString(
                    R.string.reorder_row_format,
                    item.ad,
                    item.kod,
                    item.adet,
                    item.onerilenSiparisMiktari,
                    supplierLabel
                )
            }
            binding.tvReorderList.text = lines
        }
    }

    private fun sharePdfReport() {
        if (products.isEmpty()) {
            Toast.makeText(this, R.string.msg_export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val pdfFile = File(exportDir, "e_stock_noted_rapor.pdf")
            PdfReportGenerator.generateInventoryReport(
                outputFile = pdfFile,
                items = products,
                generatedBy = sessionManager.getUsername()
            )
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pdf_subject))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_pdf_chooser)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.msg_pdf_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCsvReport() {
        if (products.isEmpty()) {
            Toast.makeText(this, R.string.msg_export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(exportDir, "e_stock_noted_rapor.csv")
            csvFile.writeText(StokCsvExporter.toCsv(products), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                csvFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_csv_subject))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_csv_chooser)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
