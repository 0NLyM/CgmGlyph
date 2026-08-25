package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.Trend
import java.time.Instant
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Renders the current glucose reading as a raw 25x25 brightness grid (row-major) matching the
 * format expected by GlyphMatrixManager.setMatrixFrame(IntArray).
 *
 * Style notes (deliberately minimal, "Nothing"-esque): the matrix is monochrome hardware, so the
 * only expressive dimension left is brightness. A fresh, valid reading is drawn at full
 * brightness; a stale one (no update in a while) dims automatically instead of adding an icon.
 * No reading yet, or ControlX2 reporting no CGM connected to the pump (its own "n/a"), both show
 * in full brightness -- those are states worth noticing, not quietly fading into the background.
 * On the physical Glyph Matrix only, a small clock sits above the value, equally spaced from the
 * top and bottom edges of the matrix as the value is, and the phone's battery level is drawn as a
 * ring that traces the matrix's circular edge (clockwise from the top, like a charge indicator)
 * instead of a percentage number.
 */
object MatrixRenderer {
    const val SIZE = 25

    // 2047 (an 11-bit ceiling, going by Nothing's own official GlyphButtonDemoService.kt using
    // Random.nextInt(2047) on this same raw array) measurably brightened the matrix over the
    // previously-assumed 255 cap, but still reads slightly dimmer than other Glyph Toys -- trying
    // the next natural ceiling up, 4095 (12-bit), on the same reasoning. Still unverified/a guess;
    // report back whether this closes the gap, over-shoots, or renders oddly.
    private const val FULL_BRIGHTNESS = 4095
    private const val DIM_BRIGHTNESS = 1120
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
            if (batteryPercent != null) drawBatteryRing(grid, batteryPercent, brightness)
            drawClock(grid, nowMillis, brightness)
        }

        when {
            reading == null -> drawPlaceholder(grid, "---", brightness)
            !reading.valid -> drawPlaceholder(grid, "n/a", brightness)
            useMmol -> drawMmol(grid, reading.mmol(), reading.trend, brightness)
            else -> drawMgdl(grid, reading.mgdl, reading.trend, brightness)
        }
        return grid
    }

    // Clock (5 rows) then a 1px gap then value+arrow (7 rows) form a 13-row block, centered
    // top-to-bottom with equal 6-row margins above and below -- so the clock and the glucose
    // value sit equidistant from the matrix's top and bottom edges. This is a deliberate step
    // away from centering the value+arrow on the matrix's true vertical center (where the
    // circular LED layout has the most physical width per row); best-effort/unverified whether a
    // 3-digit value with the arrow still fits cleanly this far from center.
    private const val CLOCK_Y = 6
    private const val VALUE_Y = 12
    private const val ARROW_Y = 12

    // The ring traces the matrix's circular silhouette (radius ~12.5 from center, same "bounding
    // circle" model used throughout this renderer) rather than any real per-LED map, since that
    // map isn't available -- best-effort/unverified.
    private const val RING_OUTER_RADIUS = 12.5
    private const val RING_INNER_RADIUS = 11.5
    private const val RING_TRACK_BRIGHTNESS = 200

    /** Draws the phone's battery level as a ring around the matrix's circular edge, filling
     * clockwise from the top like a charge indicator; the unfilled remainder of the ring stays
     * lit at a low brightness so the full circle is always visible, not just the filled arc. */
    private fun drawBatteryRing(grid: IntArray, percent: Int, brightness: Int) {
        val center = (SIZE - 1) / 2.0
        val fillDegrees = percent.coerceIn(0, 100) / 100.0 * 360.0
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val dx = x - center
                val dy = y - center
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < RING_INNER_RADIUS || dist > RING_OUTER_RADIUS) continue
                var angleDeg = Math.toDegrees(atan2(dx, -dy))
                if (angleDeg < 0) angleDeg += 360.0
                grid[y * SIZE + x] = if (angleDeg <= fillDegrees) brightness else RING_TRACK_BRIGHTNESS
            }
        }
    }

    private fun drawClock(grid: IntArray, nowMillis: Long, brightness: Int) {
        val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        // No ":" separator -- the 1px inter-character gap below keeps hour and minute from
        // visually running together instead.
        val text = "%02d%02d".format(time.hour, time.minute)
        val totalWidth = text.length * PixelFont.STATUS_DIGIT_WIDTH + (text.length - 1)
        var x = centeredStart(totalWidth, SIZE)
        for (c in text) {
            drawGlyph(grid, PixelFont.statusDigits.getValue(c), x, CLOCK_Y, brightness)
            x += PixelFont.STATUS_DIGIT_WIDTH + 1
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
