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
        includeStatusRows: Boolean = false,
        valueDigitStyle: PixelFont.DigitStyle = PixelFont.DigitStyle.CURRENT,
        clockDigitStyle: PixelFont.DigitStyle = PixelFont.DigitStyle.CURRENT,
        arrowStyle: PixelFont.ArrowStyle = PixelFont.ArrowStyle.CURRENT,
        displayMode: ToyDisplayMode = ToyDisplayMode.GLUCOSE
    ): IntArray {
        val grid = IntArray(SIZE * SIZE)
        // A stored style with no glyph data behind it (e.g. persisted before that style was
        // actually drawn in) must degrade to CURRENT, never crash: this renderer runs on app
        // launch, so throwing here bricks the app until its data is cleared.
        val valueDigits = PixelFont.valueDigitSets[valueDigitStyle]
            ?: PixelFont.valueDigitSets.getValue(PixelFont.DigitStyle.CURRENT)
        val arrows = PixelFont.arrowSets[arrowStyle]
            ?: PixelFont.arrowSets.getValue(PixelFont.ArrowStyle.CURRENT)

        val stale = reading != null && (nowMillis - reading.receivedEpochMillis) > STALE_AFTER_MS
        val brightness = if (reading != null && reading.valid && stale) DIM_BRIGHTNESS else FULL_BRIGHTNESS

        // Same reasoning as valueDigits above: a clock style with no clock glyph data (e.g. Stile 4,
        // which only exists for the glucose-value digits) must degrade to CURRENT, never crash.
        val clockDigits = PixelFont.clockDigitSets[clockDigitStyle]
            ?: PixelFont.clockDigitSets.getValue(PixelFont.DigitStyle.CURRENT)

        if (includeStatusRows) {
            if (batteryPercent != null) drawBatteryRing(grid, batteryPercent, brightness)
            drawClock(grid, nowMillis, brightness, clockDigits)
        }

        when (displayMode) {
            ToyDisplayMode.PUMP_BATTERY ->
                drawValueAndIcon(grid, reading?.pumpBatteryPercent?.toString() ?: "--", PixelFont.batteryIcon, brightness, valueDigits)
            ToyDisplayMode.RESERVOIR ->
                drawValueAndIcon(grid, reading?.reservoirUnits?.toString() ?: "--", PixelFont.reservoirIcon, brightness, valueDigits)
            ToyDisplayMode.GLUCOSE -> when {
                reading == null -> drawPlaceholder(grid, "---", brightness, valueDigits)
                !reading.valid -> drawPlaceholder(grid, "n/a", brightness, valueDigits)
                useMmol -> drawMmol(grid, reading.mmol(), reading.trend, reading.sensorExpired, brightness, valueDigits, arrows)
                else -> drawMgdl(grid, reading.mgdl, reading.trend, reading.sensorExpired, brightness, valueDigits, arrows)
            }
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
    // The arrow shrank from 7 to 5 rows tall; centering it within the 7-row value band instead of
    // pinning it to the same top row keeps it looking aligned with the digits next to it.
    private const val ARROW_Y = VALUE_Y + 1

    // Back to the original radius -- shrinking it last round didn't fix the clipping (the ring
    // still cut off left/right) and made it noticeably smaller, so the clipping wasn't a radius
    // problem. It was excluding the clock/value's whole row band: that made each cap end abruptly
    // right where it was widest (row 5/19, next to the excluded band), which reads as a hard cut
    // on both sides rather than a smooth curve. Dropping that exclusion and letting the ring draw
    // as a full, uninterrupted circle again -- it's drawn before the clock/value, so they still
    // naturally sit on top of it wherever the two would overlap, which is the normal/expected look
    // for a ring behind content, not a bug to route around.
    private const val RING_OUTER_RADIUS = 12.5
    private const val RING_INNER_RADIUS = 11.5

    /** Draws the phone's battery level as a single-line ring around the matrix's circular edge,
     * filling clockwise from the top like a charge indicator; the unfilled remainder is left off
     * rather than dimly lit. */
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
                if (angleDeg > fillDegrees) continue
                grid[y * SIZE + x] = brightness
            }
        }
    }

    private fun drawClock(grid: IntArray, nowMillis: Long, brightness: Int, clockDigits: PixelFont.GlyphSet) {
        val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        // "HH:MM" with a dedicated 1px-wide colon glyph between hour and minute; every character
        // (digit or colon) is separated from its neighbour by a uniform 1px gap.
        val text = "%02d:%02d".format(time.hour, time.minute)
        fun widthOf(c: Char) = if (c == ':') PixelFont.STATUS_COLON_WIDTH else clockDigits.width
        val totalWidth = text.sumOf { widthOf(it) } + (text.length - 1)
        var x = centeredStart(totalWidth, SIZE)
        for (c in text) {
            val glyph = if (c == ':') PixelFont.statusColon else clockDigits.glyphs.getValue(c)
            drawGlyph(grid, glyph, x, CLOCK_Y, brightness)
            x += widthOf(c) + 1
        }
    }

    private fun drawPlaceholder(grid: IntArray, text: String, brightness: Int, valueDigits: PixelFont.GlyphSet) {
        val totalWidth = text.length * valueDigits.width + (text.length - 1)
        var x = centeredStart(totalWidth, SIZE)
        for (c in text) {
            drawGlyph(grid, valueDigits.glyphs.getValue(c), x, VALUE_Y, brightness)
            x += valueDigits.width + 1
        }
    }

    private fun drawMgdl(
        grid: IntArray, mgdl: Int, trend: Trend, sensorExpired: Boolean, brightness: Int,
        valueDigits: PixelFont.GlyphSet, arrows: Map<Trend, List<String>>
    ) {
        drawValueAndArrow(grid, mgdl.coerceIn(0, 999).toString(), trend, sensorExpired, brightness, valueDigits, arrows)
    }

    private fun drawMmol(
        grid: IntArray, mmol: Double, trend: Trend, sensorExpired: Boolean, brightness: Int,
        valueDigits: PixelFont.GlyphSet, arrows: Map<Trend, List<String>>
    ) {
        // Below 10 mmol/L there's room for a decimal place next to the arrow; at/above it,
        // drop the decimal so "value + arrow" still fits on the same row as mg/dL does.
        val text = if (mmol < 10.0) {
            "%.1f".format((mmol * 10).roundToInt() / 10.0)
        } else {
            mmol.roundToInt().coerceAtMost(99).toString()
        }
        drawValueAndArrow(grid, text, trend, sensorExpired, brightness, valueDigits, arrows)
    }

    /** Draws "value + arrow" as a single block centered as a unit on the matrix, so the pair
     * stays visually centered regardless of how many digits the value has. When the pump reports
     * the CGM sensor as expired, an "X" takes the trend arrow's place instead. */
    private fun drawValueAndArrow(
        grid: IntArray, text: String, trend: Trend, sensorExpired: Boolean, brightness: Int,
        valueDigits: PixelFont.GlyphSet, arrows: Map<Trend, List<String>>
    ) {
        val arrowGlyph = if (sensorExpired) PixelFont.expiredSensor else arrows.getValue(trend)
        drawValueAndIcon(grid, text, arrowGlyph, brightness, valueDigits)
    }

    /** Draws "value + a fixed 5-wide icon" as a single centered block, in the exact same font and
     * position as the glucose value+arrow -- used by the Glyph Toy's pump-battery/reservoir
     * display modes, where the icon (not a trend arrow) says what the number means. */
    private fun drawValueAndIcon(
        grid: IntArray, text: String, icon: List<String>, brightness: Int, valueDigits: PixelFont.GlyphSet
    ) {
        var totalWidth = 0
        for (c in text) {
            totalWidth += (if (c == '.') PixelFont.DOT_WIDTH else valueDigits.width) + 1
        }
        totalWidth += PixelFont.ARROW_WIDTH
        var x = centeredStart(totalWidth, SIZE)

        for (c in text) {
            if (c == '.') {
                drawGlyph(grid, PixelFont.dot, x, VALUE_Y, brightness)
                x += PixelFont.DOT_WIDTH + 1
            } else {
                drawGlyph(grid, valueDigits.glyphs.getValue(c), x, VALUE_Y, brightness)
                x += valueDigits.width + 1
            }
        }

        drawGlyph(grid, icon, x, ARROW_Y, brightness)
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
