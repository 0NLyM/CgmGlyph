package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * Pixel fonts hand-drawn by the app's designer and transcribed pixel-for-pixel from their
 * reference sheets: a 3x7 glucose-value digit font, a 3x4 clock digit font, and a set of 5x5
 * trend-arrow glyphs (plus an "X" for an expired sensor). '1' = lit pixel.
 */
internal object PixelFont {

    // 4 wide x 7 tall (widened from 3, per the designer's redrawn reference sheet), transcribed
    // from its three rows of digits (1,2,7 / 3,4,5,6,0 / 8,9).
    val digits: Map<Char, List<String>> = mapOf(
        '0' to listOf("0110", "1001", "1001", "1001", "1001", "1001", "0110"),
        '1' to listOf("0010", "0110", "0010", "0010", "0010", "0010", "0111"),
        '2' to listOf("1110", "0001", "0001", "0110", "1000", "1000", "1111"),
        '3' to listOf("1110", "0001", "0001", "0110", "0001", "0001", "1110"),
        '4' to listOf("0010", "0110", "0110", "1010", "1111", "0010", "0010"),
        '5' to listOf("1111", "1000", "1000", "1110", "0001", "0001", "1110"),
        '6' to listOf("0110", "1000", "1000", "1110", "1001", "1001", "0110"),
        '7' to listOf("1111", "0001", "0010", "0010", "0100", "0100", "0100"),
        '8' to listOf("0110", "1001", "1001", "0110", "1001", "1001", "0110"),
        '9' to listOf("0110", "1001", "1001", "0111", "0001", "0001", "0110"),
        '-' to listOf("0000", "0000", "0000", "1111", "0000", "0000", "0000"),
        // Lowercase, used only to spell "n/a" when ControlX2 reports no CGM connected to the pump.
        'n' to listOf("0000", "0000", "1100", "1010", "1010", "1010", "1010"),
        'a' to listOf("0000", "0000", "0110", "1010", "0110", "1010", "0110"),
        '/' to listOf("0001", "0001", "0010", "0010", "0100", "0100", "1000")
    )
    const val DIGIT_WIDTH = 4
    const val DOT_WIDTH = 1
    const val GLYPH_HEIGHT = 7

    /** A single lit pixel on the baseline, used as a decimal point. */
    val dot: List<String> = listOf("0", "0", "0", "0", "0", "0", "1")

    // 5 wide x 5 tall, transcribed pixel-for-pixel from the reference sheet. The designer
    // confirmed each shape's meaning against a render of these exact patterns (labeled A-E) after
    // an earlier guess at the up/down/flat/diagonal assignment turned out wrong on real hardware.
    private val flat = listOf(
        "00100", "10110", "01111", "10110", "00100"
    )
    private val up = listOf(
        "00100", "01110", "11111", "00100", "01010"
    )
    private val down = listOf(
        "01010", "00100", "11111", "01110", "00100"
    )
    private val fortyFiveUp = listOf(
        "11111", "01111", "00111", "11011", "01001"
    )
    private val fortyFiveDown = listOf(
        "01001", "11011", "00111", "01111", "11111"
    )
    // Not part of this round's designer-confirmed relabeling -- kept as-is.
    val expiredSensor = listOf(
        "10011", "01010", "00100", "01010", "11001"
    )
    val arrows: Map<Trend, List<String>> = mapOf(
        Trend.DOUBLE_UP to up,
        Trend.SINGLE_UP to up,
        Trend.FORTY_FIVE_UP to fortyFiveUp,
        Trend.FLAT to flat,
        Trend.FORTY_FIVE_DOWN to fortyFiveDown,
        Trend.SINGLE_DOWN to down,
        Trend.DOUBLE_DOWN to down,
        Trend.UNKNOWN to flat
    )
    const val ARROW_WIDTH = 5

    // 4 wide x 4 tall -- widened to match the glucose-value font's block style (each row here is
    // that font's row 0/2/4/6, the same 4 rows that carry its top bar, upper and lower body, and
    // bottom bar), instead of the older, separately-drawn thinner 3-wide clock font. A dedicated
    // 1-wide colon glyph sits between HH and MM.
    val statusDigits: Map<Char, List<String>> = mapOf(
        '0' to listOf("0110", "1001", "1001", "0110"),
        '1' to listOf("0010", "0010", "0010", "0111"),
        '2' to listOf("1110", "0001", "1000", "1111"),
        '3' to listOf("1110", "0001", "0001", "1110"),
        '4' to listOf("0010", "0110", "1111", "0010"),
        '5' to listOf("1111", "1000", "0001", "1110"),
        '6' to listOf("0110", "1000", "1001", "0110"),
        '7' to listOf("1111", "0010", "0100", "0100"),
        '8' to listOf("0110", "1001", "1001", "0110"),
        '9' to listOf("0110", "1001", "0001", "0110")
    )
    const val STATUS_DIGIT_WIDTH = 4

    val statusColon: List<String> = listOf("0", "1", "0", "1")
    const val STATUS_COLON_WIDTH = 1
}
