package it.mattia.controlx2face.minimalgrid

/**
 * Geometry for the Minimal Grid face, as fractions of the display's width/height so one set of
 * numbers serves every watch size (a 384px round and a 454px round differ only in scale).
 *
 * Deliberately free of any Android import: these are the numbers most likely to be quietly wrong,
 * and keeping them as plain data means they can be asserted in a JVM unit test -- specifically
 * that every complication corner actually falls inside the round display, which the obvious
 * full-width layout does not.
 */
object MinimalGridLayout {

    /** A fractional rectangle in [0,1] display space. */
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val corners: List<Pair<Float, Float>>
            get() = listOf(left to top, right to top, left to bottom, right to bottom)
    }

    // --- Complication row -------------------------------------------------------------------
    //
    // Tucked in from the edges on purpose. Spanning 0.14..0.86 across y 0.70..0.88 -- the layout
    // that looks right on a square canvas -- puts the outer bottom corners at radius 0.523, i.e.
    // off the side of a round watch. These bounds keep the worst corner at ~0.439.

    private const val ROW_LEFT = 0.18f
    private const val ROW_RIGHT = 0.82f
    const val ROW_TOP = 0.66f
    const val ROW_BOTTOM = 0.80f

    private const val CELL_WIDTH = (ROW_RIGHT - ROW_LEFT) / 3f

    /** Left, centre and right data cells, in reading order. */
    val cells: List<Rect> = (0..2).map { i ->
        Rect(
            left = ROW_LEFT + i * CELL_WIDTH,
            top = ROW_TOP,
            right = ROW_LEFT + (i + 1) * CELL_WIDTH,
            bottom = ROW_BOTTOM,
        )
    }

    /** Trend slot, sitting above the time. */
    val trendSlot = Rect(0.40f, 0.13f, 0.60f, 0.25f)

    /** Every slot the face owns, in slot-id order. */
    val allSlots: List<Rect> = cells + trendSlot

    // --- Rules and text ---------------------------------------------------------------------

    /** x positions of the two vertical hairlines separating the three cells. */
    val cellDividers: List<Float> = listOf(ROW_LEFT + CELL_WIDTH, ROW_LEFT + 2 * CELL_WIDTH)

    /** Vertical extent of those hairlines: shorter than the cells, so they read as separators. */
    const val DIVIDER_TOP = 0.68f
    const val DIVIDER_BOTTOM = 0.78f

    /** Horizontal rule above the data row. */
    const val RULE_Y = 0.645f
    const val RULE_LEFT = ROW_LEFT
    const val RULE_RIGHT = ROW_RIGHT

    /** Baseline of the date line, above the time. */
    const val DATE_BASELINE_Y = 0.262f

    /** Baseline of the time. */
    const val TIME_BASELINE_Y = 0.515f

    /** Time size as a fraction of the shorter side. */
    const val TIME_SIZE_RATIO = 0.284f

    /** Date size as a fraction of the shorter side. */
    const val DATE_SIZE_RATIO = 0.033f

    /** True when every corner of [rect] lies within the inscribed circle of a round display. */
    fun isInsideRoundDisplay(rect: Rect): Boolean = rect.corners.all { (x, y) ->
        val dx = x - 0.5f
        val dy = y - 0.5f
        dx * dx + dy * dy <= 0.25f
    }
}
