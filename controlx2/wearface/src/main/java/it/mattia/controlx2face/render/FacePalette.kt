package it.mattia.controlx2face.render

import android.graphics.Color

/**
 * The one place colour is decided, shared by both faces.
 *
 * Monochrome by intent: on a Nothing/CMF-styled face colour is not decoration, it is a signal, so
 * exactly one hue exists and it means one thing -- glucose is out of range right now. Everything
 * else (time, labels, complications, and the whole of ambient) stays white/grey on black.
 */
object FacePalette {
    const val BACKGROUND = Color.BLACK

    /** A lit pixel / primary text. */
    const val LIT = 0xFFFFFFFF.toInt()

    /** A lit pixel whose value is stale: still readable, visibly not current. */
    const val LIT_DIM = 0xFF4A4A4A.toInt()

    /**
     * An unlit pixel. Drawn (interactive only) so the glyphs read as a real LED panel with dark
     * cells rather than as floating dots -- the same look the phone's Glyph Matrix has.
     */
    const val UNLIT = 0xFF141414.toInt()

    /** Secondary readouts: IOB, battery, date. */
    const val SECONDARY = 0xFF8A8A8A.toInt()

    /** Hairline rules and dividers. */
    const val RULE = 0xFF262626.toInt()

    /** Nothing red. The only hue on either face, and only ever for out-of-range glucose. */
    const val ACCENT = 0xFFD71921.toInt()
}
