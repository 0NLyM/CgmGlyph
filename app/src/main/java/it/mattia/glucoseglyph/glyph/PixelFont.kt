package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * Pixel fonts hand-drawn by the app's designer and transcribed pixel-for-pixel from their
 * reference sheets: a 3x7 glucose-value digit font, a 3x4 clock digit font, and a set of 5x5
 * trend-arrow glyphs (plus an "X" for an expired sensor). '1' = lit pixel.
 */
internal object PixelFont {

    // 3 wide x 7 tall. Transcribed from the reference sheet's two rows of digits (1-4, then 5-9);
    // '0' wasn't drawn there, so it's designed here to match the others' block style.
    val digits: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "101", "101", "101", "111"),
        '1' to listOf("010", "000", "010", "010", "010", "010", "011"),
        '2' to listOf("110", "001", "001", "010", "100", "100", "111"),
        '3' to listOf("110", "001", "001", "110", "001", "001", "111"),
        '4' to listOf("001", "010", "100", "101", "110", "001", "001"),
        '5' to listOf("111", "100", "100", "010", "001", "001", "110"),
        '6' to listOf("011", "100", "100", "110", "101", "101", "011"),
        '7' to listOf("110", "001", "001", "010", "001", "001", "001"),
        '8' to listOf("110", "101", "101", "111", "101", "101", "011"),
        '9' to listOf("110", "101", "101", "011", "001", "001", "110"),
        '-' to listOf("000", "000", "000", "111", "000", "000", "000"),
        // Lowercase, used only to spell "n/a" when ControlX2 reports no CGM connected to the pump.
        'n' to listOf("000", "000", "110", "101", "101", "101", "101"),
        'a' to listOf("000", "000", "011", "101", "011", "101", "011"),
        '/' to listOf("001", "001", "010", "010", "100", "100", "100")
    )
    const val DIGIT_WIDTH = 3
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

    // 3 wide x 4 tall, transcribed from the reference sheet's two rows of digits (1-4, then
    // 5-9); '0' wasn't drawn there, so it's designed here to match the others' block style. A
    // dedicated 1-wide colon glyph sits between HH and MM.
    val statusDigits: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "111"),
        '1' to listOf("010", "100", "010", "111"),
        '2' to listOf("110", "001", "100", "111"),
        '3' to listOf("111", "010", "001", "110"),
        '4' to listOf("001", "010", "111", "001"),
        '5' to listOf("111", "100", "001", "110"),
        '6' to listOf("011", "100", "111", "011"),
        '7' to listOf("110", "001", "011", "001"),
        '8' to listOf("111", "000", "111", "111"),
        '9' to listOf("110", "111", "001", "110")
    )
    const val STATUS_DIGIT_WIDTH = 3

    val statusColon: List<String> = listOf("0", "1", "0", "1")
    const val STATUS_COLON_WIDTH = 1
}
