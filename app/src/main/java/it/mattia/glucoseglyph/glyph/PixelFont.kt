package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * A tiny 5x7 dot-matrix font (digits + dash + decimal point) and a handful of 6x7 trend-arrow
 * glyphs, hand-drawn to read clearly on the Phone (3) Glyph Matrix's 25x25 LEDs.
 */
internal object PixelFont {

    // Each glyph is 7 rows of a bit-string; '1' = lit pixel.
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

    /** A tiny 3x5 digit set used for the clock and battery readouts (secondary info, kept small
     * and dim so the glucose value stays the one thing that grabs your eye). */
    val tinyDigits: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "001", "001", "001"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111")
    )
    const val TINY_DIGIT_WIDTH = 3
    const val TINY_GLYPH_HEIGHT = 5

    /** Two stacked dots, 1px wide. */
    val tinyColon: List<String> = listOf("0", "1", "0", "1", "0")
    const val TINY_COLON_WIDTH = 1

    /** A small diagonal stand-in for "%", 3px wide -- a literal percent sign doesn't read at
     * this size, but a lone slash next to a number is an unambiguous "percent" convention. */
    val tinyPercent: List<String> = listOf("100", "001", "010", "100", "001")
    const val TINY_PERCENT_WIDTH = 3
}
