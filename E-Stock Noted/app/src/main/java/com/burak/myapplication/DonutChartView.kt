package com.burak.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Native Material 3 Donut Chart displaying category inventory distribution with legend.
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var slices: List<KategoriAnaliz> = emptyList()
    private var centerPrimaryText: String = "0"
    private var centerSecondaryText: String = ""

    private val palette = intArrayOf(
        Color.parseColor("#053262"),
        Color.parseColor("#2AB89C"),
        Color.parseColor("#0284C7"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#10B981"),
        Color.parseColor("#64748B")
    )

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val emptyTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E2E8F0")
    }

    private val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val centerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textAlign = Paint.Align.CENTER
    }

    private val legendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val legendLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
    }

    private val legendValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val ovalRect = RectF()

    fun setData(data: List<KategoriAnaliz>, centerPrimary: String, centerSecondary: String) {
        slices = data
        centerPrimaryText = centerPrimary
        centerSecondaryText = centerSecondary
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val legendRows = slices.size.coerceAtLeast(1)
        val desiredHeight = ((190f + legendRows * 24f) * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val w = width.toFloat()
        val donutSize = min(w * 0.56f, 168f * density)
        val strokeWidth = 24f * density
        arcPaint.strokeWidth = strokeWidth
        emptyTrackPaint.strokeWidth = strokeWidth

        val cx = w / 2f
        val cy = (donutSize / 2f) + (12f * density)
        val radius = (donutSize - strokeWidth) / 2f
        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        if (slices.isEmpty()) {
            canvas.drawArc(ovalRect, 0f, 360f, false, emptyTrackPaint)
        } else {
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = ((slice.oranYuzde / 100.0) * 360.0).toFloat().coerceAtLeast(1f)
                arcPaint.color = palette[index % palette.size]
                canvas.drawArc(ovalRect, startAngle, sweep, false, arcPaint)
                startAngle += sweep
            }
        }

        centerTitlePaint.textSize = 15f * density
        centerSubPaint.textSize = 11f * density
        canvas.drawText(centerPrimaryText, cx, cy + (2f * density), centerTitlePaint)
        canvas.drawText(centerSecondaryText, cx, cy + (18f * density), centerSubPaint)

        // Legend below donut
        var ly = cy + (donutSize / 2f) + (24f * density)
        val leftPad = 12f * density
        val rightPad = w - (12f * density)
        legendLabelPaint.textSize = 12f * density
        legendValuePaint.textSize = 12f * density
        legendValuePaint.textAlign = Paint.Align.RIGHT

        slices.forEachIndexed { index, slice ->
            legendDotPaint.color = palette[index % palette.size]
            canvas.drawCircle(leftPad + (5f * density), ly - (4f * density), 5f * density, legendDotPaint)
            canvas.drawText(
                "${slice.kategori} (${slice.urunSayisi})",
                leftPad + (16f * density),
                ly,
                legendLabelPaint
            )
            val rightText = "${StockAnalytics.formatPercent(slice.oranYuzde)} • ${StockAnalytics.formatCurrency(slice.toplamDeger)}"
            canvas.drawText(rightText, rightPad, ly, legendValuePaint)
            ly += 22f * density
        }
    }
}
