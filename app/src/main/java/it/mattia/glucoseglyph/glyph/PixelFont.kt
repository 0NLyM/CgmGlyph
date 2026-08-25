package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * A 5x7 dot-matrix font (digits, dash, decimal point) and a set of 5x5 trend-arrow glyphs,
 * hand-drawn to read clearly on the Phone (3) Glyph Matrix's 25x25 LEDs.
 */
internal object PixelFont {

    // Each glyph is 7 rows of a bit-string; '1' = lit pixel. Full original size (this app's very
    // first digit font, before it was shrunk in stages to make room for the clock/battery rows).
    val digits: Map<Char, List<String>> = mapOf(
        '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to listOf("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
        '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
        // Lowercase, used only to spell "n/a" when ControlX2 reports no CGM connected to the pump.
        'n' to listOf("00000", "00000", "10110", "11001", "10001", "10001", "10001"),
        'a' to listOf("00000", "00000", "01110", "00001", "01111", "10001", "01111"),
        '/' to listOf("00001", "00001", "00010", "00100", "01000", "10000", "10000")
    )
    const val DIGIT_WIDTH = 5
    const val DOT_WIDTH = 1
    const val GLYPH_HEIGHT = 7

    /** A single lit pixel on the baseline, used as a decimal point. */
    val dot: List<String> = listOf("0", "0", "0", "0", "0", "0", "1")

    // Chevron-head-plus-shaft arrows, back at a compact 5x5 (down from 7x7) -- a small pointed
    // head with a short trailing line, rather than the solid filled triangles tried in between.
    // Single and double magnitude still share one shape per direction (the earlier chevron design
    // stacked two chevrons for DOUBLE_UP/DOUBLE_DOWN, which read as two triangles glued together).
    private val chevronUp = listOf(
        "00100", "01110", "00100", "00100", "00100"
    )
    private val chevronDown = listOf(
        "00100", "00100", "00100", "01110", "00100"
    )
    // Diagonal chevrons hand-drawn directly (not derived by transforming chevronUp/Right) after an
    // earlier bug: rotating a shape by transposing its grid mirrors it across the diagonal instead
    // of rotating it, which silently flips the direction it points.
    private val chevronUpRight = listOf(
        "00111", "00011", "00100", "01000", "10000"
    )
    private val chevronDownRight = listOf(
        "10000", "01000", "00100", "00011", "00111"
    )
    private val chevronRight = listOf(
        "00000", "00010", "11111", "00010", "00000"
    )
    val arrows: Map<Trend, List<String>> = mapOf(
        Trend.DOUBLE_UP to chevronUp,
        Trend.SINGLE_UP to chevronUp,
        Trend.FORTY_FIVE_UP to chevronUpRight,
        Trend.FLAT to chevronRight,
        Trend.FORTY_FIVE_DOWN to chevronDownRight,
        Trend.SINGLE_DOWN to chevronDown,
        Trend.DOUBLE_DOWN to chevronDown,
        Trend.UNKNOWN to listOf(
            "00000", "00000", "01110", "00000", "00000"
        )
    )
    const val ARROW_WIDTH = 5

    // Clock uses a dedicated 4x5 font -- a step up from the original 3px-wide version (which read
    // as noticeably thinner/smaller than the glucose value) but narrower than the full 5px digit
    // font above (which clipped badly at these outer rows: the matrix's circular LED layout
    // leaves far less physical width there than in the middle). Same rounded-rectangle design
    // language as `digits` above, just one column narrower; drawn with a 1px gap between every
    // character so adjacent glyphs' strokes don't touch and bleed into each other.
    val statusDigits: Map<Char, List<String>> = mapOf(
        '0' to listOf("0110", "1001", "1001", "1001", "0110"),
        '1' to listOf("0010", "0110", "0010", "0010", "0111"),
        '2' to listOf("1110", "0001", "0110", "1000", "1111"),
        '3' to listOf("1110", "0001", "0110", "0001", "1110"),
        '4' to listOf("1001", "1001", "1111", "0001", "0001"),
        '5' to listOf("1111", "1000", "1110", "0001", "1110"),
        '6' to listOf("0110", "1000", "1110", "1001", "0110"),
        '7' to listOf("1111", "0001", "0010", "0100", "0100"),
        '8' to listOf("0110", "1001", "0110", "1001", "0110"),
        '9' to listOf("0110", "1001", "0111", "0001", "0110")
    )
    const val STATUS_DIGIT_WIDTH = 4
}
