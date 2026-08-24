package it.mattia.glucoseglyph.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.Trend

/**
 * Renders the current glucose reading plus its trend arrow as a small bitmap, used as the
 * foreground service notification's small icon so the value itself is what shows up in the
 * status bar -- not a generic app icon that tells you nothing at a glance.
 */
object StatusBarIconRenderer {
    private const val CANVAS_SIZE = 96

    fun render(reading: GlucoseReading?, useMmol: Boolean): IconCompat {
        val text = valueText(reading, useMmol) + arrowGlyph(reading)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        var textSize = CANVAS_SIZE * 0.85f
        paint.textSize = textSize
        val maxWidth = CANVAS_SIZE * 0.92f
        while (paint.measureText(text) > maxWidth && textSize > 12f) {
            textSize -= 2f
            paint.textSize = textSize
        }

        val y = CANVAS_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, CANVAS_SIZE / 2f, y, paint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun valueText(reading: GlucoseReading?, useMmol: Boolean): String = when {
        reading == null -> "…" // ellipsis: waiting for the first reading
        !reading.valid -> "n/a"
        useMmol -> "%.1f".format(reading.mmol())
        else -> reading.mgdl.toString()
    }

    private fun arrowGlyph(reading: GlucoseReading?): String {
        if (reading == null || !reading.valid) return ""
        return when (reading.trend) {
            Trend.DOUBLE_UP -> "⇑"
            Trend.SINGLE_UP -> "↑"
            Trend.FORTY_FIVE_UP -> "↗"
            Trend.FLAT -> "→"
            Trend.FORTY_FIVE_DOWN -> "↘"
            Trend.SINGLE_DOWN -> "↓"
            Trend.DOUBLE_DOWN -> "⇓"
            Trend.UNKNOWN -> ""
        }
    }
}
