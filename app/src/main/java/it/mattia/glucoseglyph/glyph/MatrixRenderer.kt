package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.Trend
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Renders the current glucose reading as a raw 25x25 brightness grid (row-major, values 0-255)
 * matching the format expected by GlyphMatrixManager.setMatrixFrame(IntArray).
 *
 * Style notes (deliberately minimal, "Nothing"-esque): the matrix is monochrome hardware, so the
 * only expressive dimension left is brightness. A fresh, valid reading is drawn at full
 * brightness; a stale one (no update in a while) dims automatically instead of adding an icon;
 * no reading at all shows a dim dashed placeholder. A small clock sits above the value and the
 * phone's battery percentage below it, both dim so the glucose reading stays the focal point.
 */
object MatrixRenderer {
    const val SIZE = 25

    private const val FULL_BRIGHTNESS = 255
    private const val DIM_BRIGHTNESS = 70
    private const val SECONDARY_BRIGHTNESS = 90
    private const val STALE_AFTER_MS = 15 * 60 * 1000L

    fun render(
        reading: GlucoseReading?,
        useMmol: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        batteryPercent: Int? = null
    ): IntArray {
        val grid = IntArray(SIZE * SIZE)

        drawClock(grid, nowMillis)
        if (batteryPercent != null) drawBattery(grid, batteryPercent)

        if (reading == null) {
            drawPlaceholder(grid)
            return grid
        }

        val stale = (nowMillis - reading.receivedEpochMillis) > STALE_AFTER_MS
        val brightness = if (!reading.valid) DIM_BRIGHTNESS else if (stale) DIM_BRIGHTNESS else FULL_BRIGHTNESS

        if (!reading.valid) {
            drawPlaceholder(grid, brightness)
            return grid
        }

        if (useMmol) {
            drawMmol(grid, reading.mmol(), reading.trend, brightness)
        } else {
            drawMgdl(grid, reading.mgdl, reading.trend, brightness)
        }
        return grid
    }

    private fun drawClock(grid: IntArray, nowMillis: Long) {
        val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        val text = "%02d:%02d".format(time.hour, time.minute)
        drawTinyText(grid, text, y = 2)
    }

    private fun drawBattery(grid: IntArray, percent: Int) {
        drawTinyText(grid, percent.coerceIn(0, 100).toString(), y = 18)
    }

    /** Centers a string of digits/':' set in the tiny 3x5 font at the given row. */
    private fun drawTinyText(grid: IntArray, text: String, y: Int) {
        var totalWidth = 0
        for (c in text) totalWidth += if (c == ':') PixelFont.TINY_COLON_WIDTH else PixelFont.TINY_DIGIT_WIDTH
        totalWidth += text.length - 1

        var x = centeredStart(totalWidth, SIZE)
        for (c in text) {
            if (c == ':') {
                drawGlyph(grid, PixelFont.tinyColon, x, y, SECONDARY_BRIGHTNESS)
                x += PixelFont.TINY_COLON_WIDTH + 1
            } else {
                drawGlyph(grid, PixelFont.tinyDigits.getValue(c), x, y, SECONDARY_BRIGHTNESS)
                x += PixelFont.TINY_DIGIT_WIDTH + 1
            }
        }
    }

    private fun drawPlaceholder(grid: IntArray, brightness: Int = DIM_BRIGHTNESS) {
        val chars = "---".toList()
        val totalWidth = chars.size * PixelFont.DIGIT_WIDTH + (chars.size - 1)
        var x = centeredStart(totalWidth, 17)
        val y = (SIZE - PixelFont.GLYPH_HEIGHT) / 2
        for (c in chars) {
            drawGlyph(grid, PixelFont.digits.getValue(c), x, y, brightness)
            x += PixelFont.DIGIT_WIDTH + 1
        }
    }

    private fun drawMgdl(grid: IntArray, mgdl: Int, trend: Trend, brightness: Int) {
        val digitsStr = mgdl.coerceIn(0, 999).toString()
        val totalWidth = digitsStr.length * PixelFont.DIGIT_WIDTH + (digitsStr.length - 1)
        var x = centeredStart(totalWidth, 17)
        val y = (SIZE - PixelFont.GLYPH_HEIGHT) / 2
        for (c in digitsStr) {
            drawGlyph(grid, PixelFont.digits.getValue(c), x, y, brightness)
            x += PixelFont.DIGIT_WIDTH + 1
        }

        // 3 digits (worst case) take columns 0-16; a 7px arrow at column 18 lands on 18-24,
        // using the full 25px width with no clipping.
        val arrowX = 18
        drawGlyph(grid, PixelFont.arrows.getValue(trend), arrowX, y, arrowBrightness(brightness))
    }

    private fun drawMmol(grid: IntArray, mmol: Double, trend: Trend, brightness: Int) {
        // Below 10 mmol/L there's room for a decimal place next to the arrow; at/above it,
        // drop the decimal so "value + arrow" still fits on the same row as mg/dL does.
        val text = if (mmol < 10.0) {
            "%.1f".format((mmol * 10).roundToInt() / 10.0)
        } else {
            mmol.roundToInt().coerceAtMost(99).toString()
        }

        var totalWidth = 0
        for (c in text) totalWidth += if (c == '.') PixelFont.DOT_WIDTH else PixelFont.DIGIT_WIDTH
        totalWidth += text.length - 1 // 1px gap between glyphs

        val y = (SIZE - PixelFont.GLYPH_HEIGHT) / 2
        var x = centeredStart(totalWidth, 17)
        for (c in text) {
            if (c == '.') {
                drawGlyph(grid, PixelFont.dot, x, y, brightness)
                x += PixelFont.DOT_WIDTH + 1
            } else {
                drawGlyph(grid, PixelFont.digits.getValue(c), x, y, brightness)
                x += PixelFont.DIGIT_WIDTH + 1
            }
        }

        val arrowX = 18
        drawGlyph(grid, PixelFont.arrows.getValue(trend), arrowX, y, arrowBrightness(brightness))
    }

    private fun arrowBrightness(base: Int) = (base * 0.75).roundToInt().coerceIn(0, 255)

    private fun centeredStart(contentWidth: Int, availableWidth: Int): Int =
        ((availableWidth - contentWidth) / 2).coerceAtLeast(0)

    private fun drawGlyph(grid: IntArray, pattern: List<String>, x0: Int, y0: Int, brightness: Int) {
        for (row in pattern.indices) {
            val y = y0 + row
            if (y < 0 || y >= SIZE) continue
            val line = pattern[row]
            for (col in line.indices) {
                if (line[col] != '1') continue
                val x = x0 + col
                if (x < 0 || x >= SIZE) continue
                grid[y * SIZE + x] = brightness
            }
        }
    }
}
