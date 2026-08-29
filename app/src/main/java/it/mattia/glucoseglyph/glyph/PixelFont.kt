package it.mattia.glucoseglyph.glyph

import it.mattia.glucoseglyph.model.Trend

/**
 * Pixel fonts for the Glyph Matrix, each offered in a few selectable [DigitStyle]/[ArrowStyle]
 * variants (see [AppSettings][it.mattia.glucoseglyph.model.AppSettings] for the stored choice):
 * a glucose-value digit font, a clock digit font, and a set of 5x5 trend-arrow glyphs (plus a
 * fixed "X" for an expired sensor, which isn't part of the style choice). '1' = lit pixel.
 */
internal object PixelFont {

    /** CURRENT is always the designer's latest-confirmed shapes and the default selection.
     * LEGACY/ORIGINAL are kept around purely as alternate looks the designer can switch back to. */
    enum class DigitStyle(val label: String) {
        CURRENT("Attuale"),
        LEGACY("Precedente"),
        ORIGINAL("Originale")
    }

    enum class ArrowStyle(val label: String) {
        CURRENT("Attuale"),
        ALTERNATE("Nuovo stile")
    }

    /** A digit font plus the column width its glyphs are drawn at (styles vary in both shape and
     * width, e.g. the original glucose-value font is 5 wide, today's is 4). */
    class GlyphSet(val glyphs: Map<Char, List<String>>, val width: Int)

    // --- Glucose-value digit font: CURRENT is 4x7 (widened from the designer's first 3x7 pass to
    // read more clearly); LEGACY is that first 3x7 designer pass; ORIGINAL is this app's very
    // first (pre-designer) 5x7 font. Each includes '-' and the lowercase "n/a" letters used when
    // ControlX2 reports no CGM connected to the pump. ---
    val valueDigitSets: Map<DigitStyle, GlyphSet> = mapOf(
        DigitStyle.CURRENT to GlyphSet(
            mapOf(
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
                'n' to listOf("0000", "0000", "1100", "1010", "1010", "1010", "1010"),
                'a' to listOf("0000", "0000", "0110", "1010", "0110", "1010", "0110"),
                '/' to listOf("0001", "0001", "0010", "0010", "0100", "0100", "1000")
            ),
            width = 4
        ),
        DigitStyle.LEGACY to GlyphSet(
            mapOf(
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
                'n' to listOf("000", "000", "110", "101", "101", "101", "101"),
                'a' to listOf("000", "000", "011", "101", "011", "101", "011"),
                '/' to listOf("001", "001", "010", "010", "100", "100", "100")
            ),
            width = 3
        ),
        DigitStyle.ORIGINAL to GlyphSet(
            mapOf(
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
                'n' to listOf("00000", "00000", "10110", "11001", "10001", "10001", "10001"),
                'a' to listOf("00000", "00000", "01110", "00001", "01111", "10001", "01111"),
                '/' to listOf("00001", "00001", "00010", "00100", "01000", "10000", "10000")
            ),
            width = 5
        )
    )
    const val DOT_WIDTH = 1

    /** A single lit pixel on the baseline, used as a decimal point (same for every style). */
    val dot: List<String> = listOf("0", "0", "0", "0", "0", "0", "1")

    // --- Trend-arrow glyphs, 5x5 for every style. CURRENT is the designer-confirmed shape set
    // (labeled A-E and confirmed against a render after an earlier guess turned out wrong on real
    // hardware). ALTERNATE is a second hand-drawn style the designer provided afterward. ---
    private val currentFlat = listOf(
        "00100", "10110", "01111", "10110", "00100"
    )
    private val currentUp = listOf(
        "00100", "01110", "11111", "00100", "01010"
    )
    private val currentDown = listOf(
        "01010", "00100", "11111", "01110", "00100"
    )
    private val currentFortyFiveUp = listOf(
        "11111", "01111", "00111", "11011", "01001"
    )
    private val currentFortyFiveDown = listOf(
        "01001", "11011", "00111", "01111", "11111"
    )

    val arrowSets: Map<ArrowStyle, Map<Trend, List<String>>> = mapOf(
        ArrowStyle.CURRENT to mapOf(
            Trend.DOUBLE_UP to currentUp,
            Trend.SINGLE_UP to currentUp,
            Trend.FORTY_FIVE_UP to currentFortyFiveUp,
            Trend.FLAT to currentFlat,
            Trend.FORTY_FIVE_DOWN to currentFortyFiveDown,
            Trend.SINGLE_DOWN to currentDown,
            Trend.DOUBLE_DOWN to currentDown,
            Trend.UNKNOWN to currentFlat
        )
    )
    const val ARROW_WIDTH = 5

    // Not part of the style choice -- kept as-is regardless of arrow style.
    val expiredSensor = listOf(
        "10011", "01010", "00100", "01010", "11001"
    )

    // --- Clock digit font: CURRENT is today's pixel-perfect re-transcription (photo-verified,
    // 5-9-0 corrected via automated per-cell analysis); LEGACY is the transcription attempt right
    // before that fix; ORIGINAL is this app's very first (pre-designer) clock font. A dedicated
    // 1-wide colon glyph (same for every style) sits between HH and MM. ---
    val clockDigitSets: Map<DigitStyle, GlyphSet> = mapOf(
        DigitStyle.CURRENT to GlyphSet(
            mapOf(
                '0' to listOf("111", "101", "101", "111"),
                '1' to listOf("010", "110", "010", "111"),
                '2' to listOf("111", "001", "110", "111"),
                '3' to listOf("111", "011", "001", "111"),
                '4' to listOf("101", "101", "111", "001"),
                '5' to listOf("111", "110", "001", "111"),
                '6' to listOf("111", "100", "111", "111"),
                '7' to listOf("111", "001", "001", "001"),
                '8' to listOf("111", "101", "111", "111"),
                '9' to listOf("111", "111", "001", "111")
            ),
            width = 3
        ),
        DigitStyle.LEGACY to GlyphSet(
            mapOf(
                '0' to listOf("111", "101", "101", "111"),
                '1' to listOf("010", "110", "010", "111"),
                '2' to listOf("111", "001", "110", "111"),
                '3' to listOf("111", "011", "001", "111"),
                '4' to listOf("101", "101", "111", "001"),
                '5' to listOf("111", "100", "001", "110"),
                '6' to listOf("111", "110", "001", "111"),
                '7' to listOf("111", "100", "111", "111"),
                '8' to listOf("111", "101", "001", "111"),
                '9' to listOf("111", "001", "110", "111")
            ),
            width = 3
        ),
        DigitStyle.ORIGINAL to GlyphSet(
            mapOf(
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
            ),
            width = 4
        )
    )

    val statusColon: List<String> = listOf("0", "1", "0", "1")
    const val STATUS_COLON_WIDTH = 1
}
