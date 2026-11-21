package com.burak.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Native Material 3 Grouped Bar Chart comparing daily stock inflow vs outflow.
 */
class BarTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bars: List<GunlukHareketOzet> = emptyList()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 1.5f
    }

    private val inBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2AB89C")
        style = Paint.Style.FILL
    }

    private val outBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textAlign = Paint.Align.CENTER
    }

    private val barRect = RectF()

    fun setData(data: List<GunlukHareketOzet>) {
        bars = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()

        val topPad = 24f * density
        val bottomPad = 30f * density
        val sidePad = 12f * density
        val chartHeight = (h - topPad - bottomPad).coerceAtLeast(10f)
        val baseline = topPad + chartHeight

        // Horizontal grid lines
        for (step in 0..3) {
            val y = topPad + (chartHeight * step / 3f)
            canvas.drawLine(sidePad, y, w - sidePad, y, gridPaint)
        }

        if (bars.isEmpty()) {
            labelPaint.textSize = 12f * density
            canvas.drawText("Bu aralıkta stok hareketi yok", w / 2f, topPad + chartHeight / 2f, labelPaint)
            return
        }

        val maxVal = bars.maxOf { maxOf(it.girisMiktari, it.cikisMiktari) }.coerceAtLeast(1)
        val slotWidth = (w - 2 * sidePad) / bars.size
        val singleBarWidth = (slotWidth * 0.28f).coerceAtMost(18f * density)
        val gap = 3f * density
        val corner = 4f * density

        labelPaint.textSize = 10f * density
        valuePaint.textSize = 9f * density

        bars.forEachIndexed { idx, item ->
            val centerX = sidePad + slotWidth * idx + slotWidth / 2f

            // Inflow bar (left)
            val inH = (item.girisMiktari.toFloat() / maxVal) * chartHeight
            if (inH > 0f) {
                barRect.set(
                    centerX - singleBarWidth - gap / 2f,
                    baseline - inH.coerceAtLeast(3f * density),
                    centerX - gap / 2f,
                    baseline
                )
                canvas.drawRoundRect(barRect, corner, corner, inBarPaint)
                canvas.drawText(
                    "+${item.girisMiktari}",
                    barRect.centerX(),
                    barRect.top - (4f * density),
                    valuePaint
                )
            }

            // Outflow bar (right)
            val outH = (item.cikisMiktari.toFloat() / maxVal) * chartHeight
            if (outH > 0f) {
                barRect.set(
                    centerX + gap / 2f,
                    baseline - outH.coerceAtLeast(3f * density),
                    centerX + singleBarWidth + gap / 2f,
                    baseline
                )
                canvas.drawRoundRect(barRect, corner, corner, outBarPaint)
                canvas.drawText(
                    "-${item.cikisMiktari}",
                    barRect.centerX(),
                    barRect.top - (4f * density),
                    valuePaint
                )
            }

            canvas.drawText(item.gunEtiketi, centerX, baseline + (16f * density), labelPaint)
        }
    }
}
