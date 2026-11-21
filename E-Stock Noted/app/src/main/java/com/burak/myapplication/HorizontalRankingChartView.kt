package com.burak.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal ranking bar chart for Top-N highest-value products (ABC value analysis).
 */
class HorizontalRankingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rankedItems: List<UrunModel> = emptyList()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#053262")
        style = Paint.Style.FILL
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#059669")
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textAlign = Paint.Align.CENTER
    }

    private val rect = RectF()

    fun setData(items: List<UrunModel>) {
        rankedItems = items
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val rows = rankedItems.size.coerceAtLeast(1)
        val desiredHeight = ((rows * 44f + 12f) * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val w = width.toFloat()

        if (rankedItems.isEmpty()) {
            emptyPaint.textSize = 12f * density
            canvas.drawText("Görüntülenecek ürün bulunmuyor", w / 2f, 28f * density, emptyPaint)
            return
        }

        val maxValue = rankedItems.maxOf { it.toplamDeger }.coerceAtLeast(1.0)
        val pad = 4f * density
        val barHeight = 10f * density
        val radius = 5f * density
        namePaint.textSize = 12f * density
        valuePaint.textSize = 11.5f * density

        var y = 16f * density
        rankedItems.forEachIndexed { idx, item ->
            val label = "${idx + 1}. ${item.ad} (${item.kod})"
            val trimmedLabel = if (label.length > 28) label.substring(0, 27) + "…" else label
            canvas.drawText(trimmedLabel, pad, y, namePaint)
            canvas.drawText(
                StockAnalytics.formatCurrency(item.toplamDeger),
                w - pad,
                y,
                valuePaint
            )

            val barTop = y + (6f * density)
            val fullWidth = w - 2 * pad
            rect.set(pad, barTop, pad + fullWidth, barTop + barHeight)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            val ratio = (item.toplamDeger / maxValue).toFloat().coerceIn(0f, 1f)
            if (ratio > 0f) {
                fillPaint.color = if (idx == 0) Color.parseColor("#2AB89C") else Color.parseColor("#053262")
                rect.set(pad, barTop, pad + (fullWidth * ratio).coerceAtLeast(barHeight), barTop + barHeight)
                canvas.drawRoundRect(rect, radius, radius, fillPaint)
            }
            y += 42f * density
        }
    }
}
