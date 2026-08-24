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
 * brightness; a stale one (no update in a while) dims automatically instead of adding an icon.
 * No reading yet, or ControlX2 reporting no CGM connected to the pump (its own "n/a"), both show
 * in full brightness -- those are states worth noticing, not quietly fading into the background.
 * On the physical Glyph Matrix only, a small clock sits above the value and the phone's battery
 * percentage below it, at the same brightness as the value (so nothing looks like a washed-out
 * decoration) but in a narrower dedicated font -- the value's own 5px-wide digits were tried for
 * these two rows and clipped badly, since the matrix's circular LED layout leaves far less
 * physical width near the edges than in the middle.
 */
object MatrixRenderer {
    const val SIZE = 25

    private const val FULL_BRIGHTNESS = 255
    private const val DIM_BRIGHTNESS = 70
    private const val STALE_AFTER_MS = 15 * 60 * 1000L

    fun render(
        reading: GlucoseReading?,
        useMmol: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        batteryPercent: Int? = null,
        includeStatusRows: Boolean = false
    ): IntArray {
        val grid = IntArray(SIZE * SIZE)

        val stale = reading != null && (nowMillis - reading.receivedEpochMillis) > STALE_AFTER_MS
        val brightness = if (reading != null && reading.valid && stale) DIM_BRIGHTNESS else FULL_BRIGHTNESS

        if (includeStatusRows) {
            drawClock(grid, nowMillis, brightness)
            if (batteryPercent != null) drawBattery(grid, batteryPercent, brightness)
        }

        when {
            reading == null -> drawPlaceholder(grid, "---", brightness)
            !reading.valid -> drawPlaceholder(grid, "n/a", brightness)
            useMmol -> drawMmol(grid, reading.mmol(), reading.trend, brightness)
            else -> drawMgdl(grid, reading.mgdl, reading.trend, brightness)
        }
        return grid
    }

    // Rows kept close to the matrix's vertical center (row 12) so the narrowest, outermost part
    // of the circular LED layout isn't in play -- this is the configuration that measured clean
    // (no clipping) with the narrower status font.
    private const val CLOCK_Y = 4
    private const val VALUE_Y = 10
    private const val ARROW_Y = 9
    private const val BATTERY_Y = 16

    private fun drawClock(grid: IntArray, nowMillis: Long, brightness: Int) {
        val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        val text = "%02d:%02d".format(time.hour, time.minute)
        drawStatusText(grid, text, y = CLOCK_Y, brightness)
    }

    private fun drawBattery(grid: IntArray, percent: Int, brightness: Int) {
        drawStatusText(grid, "${percent.coerceIn(0, 100)}%", y = BATTERY_Y, brightness)
    }

    /** Centers a string of digits/':'/'%' in the narrow status font on the given row. No gap is
     * left around the colon (unlike the 1px gap elsewhere) to keep "HH:MM" as narrow as possible. */
    private fun drawStatusText(grid: IntArray, text: String, y: Int, brightness: Int) {
        fun widthOf(c: Char) = when (c) {
            ':' -> PixelFont.STATUS_COLON_WIDTH
            '%' -> PixelFont.STATUS_PERCENT_WIDTH
            else -> PixelFont.STATUS_DIGIT_WIDTH
        }

        var totalWidth = 0
        for (c in text) totalWidth += widthOf(c)
        for (i in 1 until text.length) if (text[i] != ':' && text[i - 1] != ':') totalWidth++

        var x = centeredStart(totalWidth, SIZE)
        for ((i, c) in text.withIndex()) {
            val pattern = when (c) {
                ':' -> PixelFont.statusColon
                '%' -> PixelFont.statusPercent
                else -> PixelFont.statusDigits.getValue(c)
            }
            drawGlyph(grid, pattern, x, y, brightness)
            x += widthOf(c)
            val nextIsColon = i + 1 < text.length && text[i + 1] == ':'
            if (c != ':' && !nextIsColon) x += 1
        }
    }

    private fun drawPlaceholder(grid: IntArray, text: String, brightness: Int) {
        val totalWidth = text.length * PixelFont.DIGIT_WIDTH + (text.length - 1)
        var x = centeredStart(totalWidth, SIZE)
        for (c in text) {
            drawGlyph(grid, PixelFont.digits.getValue(c), x, VALUE_Y, brightness)
            x += PixelFont.DIGIT_WIDTH + 1
        }
    }

    private fun drawMgdl(grid: IntArray, mgdl: Int, trend: Trend, brightness: Int) {
        drawValueAndArrow(grid, mgdl.coerceIn(0, 999).toString(), trend, brightness)
    }

    private fun drawMmol(grid: IntArray, mmol: Double, trend: Trend, brightness: Int) {
        // Below 10 mmol/L there's room for a decimal place next to the arrow; at/above it,
        // drop the decimal so "value + arrow" still fits on the same row as mg/dL does.
        val text = if (mmol < 10.0) {
            "%.1f".format((mmol * 10).roundToInt() / 10.0)
        } else {
            mmol.roundToInt().coerceAtMost(99).toString()
        }
        drawValueAndArrow(grid, text, trend, brightness)
    }

    /** Draws "value + arrow" as a single block centered as a unit on the matrix, so the pair
     * stays visually centered regardless of how many digits the value has. */
    private fun drawValueAndArrow(grid: IntArray, text: String, trend: Trend, brightness: Int) {
        var valueWidth = 0
        for (c in text) valueWidth += if (c == '.') PixelFont.DOT_WIDTH else PixelFont.DIGIT_WIDTH
        valueWidth += text.length - 1 // 1px gap between glyphs

        // 1px gap between the value and the arrow: baked in below by always advancing x by
        // "glyph width + 1" (including after the very last character).
        val totalWidth = valueWidth + 1 + PixelFont.ARROW_WIDTH
        var x = centeredStart(totalWidth, SIZE)

        for (c in text) {
            if (c == '.') {
                drawGlyph(grid, PixelFont.dot, x, VALUE_Y, brightness)
                x += PixelFont.DOT_WIDTH + 1
            } else {
                drawGlyph(grid, PixelFont.digits.getValue(c), x, VALUE_Y, brightness)
                x += PixelFont.DIGIT_WIDTH + 1
            }
        }

        drawGlyph(grid, PixelFont.arrows.getValue(trend), x, ARROW_Y, brightness)
    }

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
