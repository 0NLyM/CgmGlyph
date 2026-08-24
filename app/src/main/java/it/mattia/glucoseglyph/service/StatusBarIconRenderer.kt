package it.mattia.glucoseglyph.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.Trend

/**
 * Renders the current glucose reading plus its trend arrow as a small bitmap, used as the
 * foreground service notification's small icon so the value itself is what shows up in the
 * status bar -- not a generic app icon that tells you nothing at a glance.
 *
 * The number gets almost the whole icon (a status bar icon is tiny to begin with, so cramming a
 * unicode arrow character next to the digits left the digits too small to read); the trend is a
 * small filled triangle in the corner instead, rotated to point in the reading's direction.
 */
object StatusBarIconRenderer {
    private const val CANVAS_SIZE = 96

    fun render(reading: GlucoseReading?, useMmol: Boolean): IconCompat {
        val text = valueText(reading, useMmol)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        var textSize = CANVAS_SIZE * 0.98f
        paint.textSize = textSize
        val maxWidth = CANVAS_SIZE * 0.98f
        while (paint.measureText(text) > maxWidth && textSize > 16f) {
            textSize -= 2f
            paint.textSize = textSize
        }

        val y = CANVAS_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, CANVAS_SIZE / 2f, y, paint)

        arrowRotationDegrees(reading)?.let { degrees -> drawTrendTriangle(canvas, degrees) }

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun valueText(reading: GlucoseReading?, useMmol: Boolean): String = when {
        reading == null -> "…" // ellipsis: waiting for the first reading
        !reading.valid -> "n/a"
        useMmol -> "%.1f".format(reading.mmol())
        else -> reading.mgdl.toString()
    }

    /** Degrees clockwise from "pointing up", or null to draw no trend indicator at all. */
    private fun arrowRotationDegrees(reading: GlucoseReading?): Float? {
        if (reading == null || !reading.valid) return null
        return when (reading.trend) {
            Trend.DOUBLE_UP, Trend.SINGLE_UP -> 0f
            Trend.FORTY_FIVE_UP -> 45f
            Trend.FLAT -> 90f
            Trend.FORTY_FIVE_DOWN -> 135f
            Trend.SINGLE_DOWN, Trend.DOUBLE_DOWN -> 180f
            Trend.UNKNOWN -> null
        }
    }

    /** A small filled triangle in the top-right corner, rotated to point in the trend direction. */
    private fun drawTrendTriangle(canvas: Canvas, degrees: Float) {
        val size = CANVAS_SIZE * 0.26f
        val cx = CANVAS_SIZE - size * 0.7f
        val cy = size * 0.7f

        val path = Path().apply {
            moveTo(cx, cy - size / 2f)
            lineTo(cx + size / 2f, cy + size / 2f)
            lineTo(cx - size / 2f, cy + size / 2f)
            close()
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.rotate(degrees, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}
