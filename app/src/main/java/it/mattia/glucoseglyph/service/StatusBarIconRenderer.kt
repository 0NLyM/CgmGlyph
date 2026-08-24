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
 * Layout: the number sits in the left ~82% of the icon, the trend indicator (a small filled
 * triangle, rotated to point in the reading's direction) sits beside it, vertically centered, in
 * the remaining strip on the right -- side by side, not stacked. The number uses a condensed,
 * non-bold system typeface, sized aggressively (see below), so it reads clearly at the tiny size
 * a status bar icon actually renders at.
 */
object StatusBarIconRenderer {
    private const val CANVAS_SIZE = 96
    private val CONDENSED = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    fun render(reading: GlucoseReading?, useMmol: Boolean): IconCompat {
        val text = valueText(reading, useMmol)
        val degrees = arrowRotationDegrees(reading)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val triangleZone = if (degrees != null) CANVAS_SIZE * 0.18f else 0f
        val numberZoneWidth = CANVAS_SIZE - triangleZone

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = CONDENSED
            textAlign = Paint.Align.CENTER
        }

        // Start well above the canvas size: a given textSize's actual glyph ink is noticeably
        // shorter than that (normal font metrics reserve headroom above/below), so sizing to
        // *fill the width* here -- letting tall digits touch or slightly crop the very top/bottom
        // -- reads as bigger than sizing conservatively to guarantee nothing is ever clipped.
        var textSize = CANVAS_SIZE * 1.35f
        paint.textSize = textSize
        val maxTextWidth = numberZoneWidth * 0.96f
        while (paint.measureText(text) > maxTextWidth && textSize > 16f) {
            textSize -= 2f
            paint.textSize = textSize
        }

        val numberCenterX = numberZoneWidth / 2f
        val y = CANVAS_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, numberCenterX, y, paint)

        if (degrees != null) drawTrendTriangle(canvas, numberZoneWidth, triangleZone, degrees)

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

    /** A small filled triangle centered in the right-hand strip, rotated to point in the trend
     * direction; vertically centered on the icon so it sits beside the number, not above it. */
    private fun drawTrendTriangle(canvas: Canvas, zoneStartX: Float, zoneWidth: Float, degrees: Float) {
        val size = minOf(zoneWidth, CANVAS_SIZE.toFloat()) * 0.8f
        val cx = zoneStartX + zoneWidth / 2f
        val cy = CANVAS_SIZE / 2f

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
