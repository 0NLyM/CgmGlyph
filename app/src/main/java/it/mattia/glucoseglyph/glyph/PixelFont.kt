package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * A 5x6 dot-matrix font (digits, dash, decimal point, percent) and a set of 7x7 trend-arrow
 * glyphs, hand-drawn to read clearly on the Phone (3) Glyph Matrix's 25x25 LEDs.
 */
internal object PixelFont {

    // Each glyph is 6 rows of a bit-string; '1' = lit pixel. Halfway back to the original 5x7
    // size (later shrunk to 5x5 to make room for the clock/battery rows) -- one row taller than
    // that 5x5 version, kept minimal/thin rather than bulked up, so it reads bigger without
    // eating into the gap above/below it.
    val digits: Map<Char, List<String>> = mapOf(
        '0' to listOf("01110", "10001", "10001", "10001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "01110"),
        '2' to listOf("11110", "00001", "00001", "01110", "10000", "11111"),
        '3' to listOf("11110", "00001", "00001", "00110", "00001", "11110"),
        '4' to listOf("10010", "10010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "10000", "11110", "00001", "11110"),
        '6' to listOf("01110", "10000", "10000", "11110", "10001", "01110"),
        '7' to listOf("11111", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "01110"),
        '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000"),
        // Lowercase, used only to spell "n/a" when ControlX2 reports no CGM connected to the pump.
        'n' to listOf("00000", "11110", "10001", "10001", "10001", "10001"),
        'a' to listOf("00000", "01110", "00001", "00001", "01111", "10001"),
        '/' to listOf("00001", "00001", "00010", "00100", "01000", "10000")
    )
    const val DIGIT_WIDTH = 5
    const val DOT_WIDTH = 1
    const val GLYPH_HEIGHT = 6

    /** A single lit pixel on the baseline, used as a decimal point. */
    val dot: List<String> = listOf("0", "0", "0", "0", "0", "1")

    // 7x7 chevron-tip-plus-shaft arrows, each direction hand-checked for a continuous,
    // unambiguous shape (the earlier 6px diagonals had disconnected pixels that read as a
    // stray right triangle on the real hardware instead of an arrow).
    val arrows: Map<Trend, List<String>> = mapOf(
        Trend.DOUBLE_UP to listOf(
            "0001000", "0011100", "0101010", "0000000", "0001000", "0011100", "0101010"
        ),
        Trend.SINGLE_UP to listOf(
            "0001000", "0011100", "0101010", "0001000", "0001000", "0001000", "0001000"
        ),
        Trend.FORTY_FIVE_UP to listOf(
            "0000111", "0000011", "0000100", "0001000", "0010000", "0100000", "1000000"
        ),
        Trend.FLAT to listOf(
            "0000000", "0000100", "0000010", "1111111", "0000010", "0000100", "0000000"
        ),
        Trend.FORTY_FIVE_DOWN to listOf(
            "1000000", "0100000", "0010000", "0001000", "0000100", "0000011", "0000111"
        ),
        Trend.SINGLE_DOWN to listOf(
            "0001000", "0001000", "0001000", "0001000", "0101010", "0011100", "0001000"
        ),
        Trend.DOUBLE_DOWN to listOf(
            "0101010", "0011100", "0001000", "0000000", "0101010", "0011100", "0001000"
        ),
        Trend.UNKNOWN to listOf(
            "0000000", "0000000", "0000000", "0111110", "0000000", "0000000", "0000000"
        )
    )
    const val ARROW_WIDTH = 7

    // Clock/battery use a dedicated 4x5 font -- a step up from the original 3px-wide version
    // (which read as noticeably thinner/smaller than the glucose value) but narrower than the
    // full 5px digit font above (which clipped badly at these outer rows: the matrix's circular
    // LED layout leaves far less physical width there than in the middle). Same rounded-rectangle
    // design language as `digits` above, just one column narrower; drawStatusText now puts a 1px
    // gap between every character (this font was previously packed with zero gap, which let
    // adjacent glyphs' strokes touch and bleed into each other -- e.g. two digits' vertical
    // sides sitting in adjacent columns read as one blob rather than two distinct "0"s).
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

    /** "%" at status-digit scale: a 1px dot at each corner (each exactly one column wide, unlike
     * the earlier two-pixel-wide blobs) with a single stray pixel between them standing in for
     * the diagonal stroke. */
    val statusPercent: List<String> = listOf("1000", "1000", "0010", "0001", "0001")
    const val STATUS_PERCENT_WIDTH = 4
}
