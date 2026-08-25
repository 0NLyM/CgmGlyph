package it.mattia.glucoseglyph.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import it.mattia.glucoseglyph.model.GlucoseReading

/**
 * Renders the current glucose reading as a small bitmap, used as the foreground service
 * notification's small icon so the value itself is what shows up in the status bar -- not a
 * generic app icon that tells you nothing at a glance.
 *
 * The number fills the entire icon (no reserved trend-arrow strip -- that made the digits
 * noticeably smaller and the triangle read poorly at this size anyway). Condensed, non-bold
 * system typeface, additionally squeezed narrower via textScaleX, so it reads clearly at the
 * tiny size a status bar icon actually renders at.
 */
object StatusBarIconRenderer {
    private const val CANVAS_SIZE = 96
    private val CONDENSED = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    fun render(reading: GlucoseReading?, useMmol: Boolean): IconCompat {
        val text = valueText(reading, useMmol)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = CONDENSED
            textAlign = Paint.Align.CENTER
            // The width budget below is fixed (CANVAS_SIZE), and textSize is picked purely by
            // shrinking until the text measures within it -- so the only way to make the digits
            // actually render bigger is to make each glyph narrower per unit of textSize, which
            // lets a taller size still fit the same width. sans-serif-condensed alone wasn't
            // narrow enough; scale it down further on top.
            textScaleX = 0.78f
        }

        var textSize = CANVAS_SIZE * 1.35f
        paint.textSize = textSize
        val maxTextWidth = CANVAS_SIZE * 0.99f
        while (paint.measureText(text) > maxTextWidth && textSize > 16f) {
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
}
