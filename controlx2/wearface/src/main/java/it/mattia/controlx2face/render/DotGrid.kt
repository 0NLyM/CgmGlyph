package it.mattia.controlx2face.render

import android.graphics.Canvas
import android.graphics.Paint
import it.mattia.pixelfont.PixelFont

/**
 * Draws [PixelFont] glyphs -- lists of "0110" row strings, '1' = lit -- as a grid of dots.
 *
 * Widths are measured rather than assumed: glyph sets differ in cell width between styles (3, 4
 * and 5 wide all exist), and a mmol/L reading like "12.3" is a different width again from a
 * 3-digit mg/dL one, so callers that centre content have to ask rather than hardcode.
 */
object DotGrid {

    /** Gap between adjacent glyphs, in grid cells. */
    const val GLYPH_GAP_CELLS = 1

    /** Dot diameter as a fraction of cell pitch; leaves the dark gap that reads as an LED panel. */
    private const val DOT_RADIUS_RATIO = 0.34f

    /** Rendered width in px of [text] drawn with [glyphs], including inter-glyph gaps. */
    fun measureText(text: String, glyphs: PixelFont.GlyphSet, pitch: Float): Float {
        if (text.isEmpty()) return 0f
        var cells = 0
        for (c in text) {
            cells += cellWidthOf(c, glyphs) + GLYPH_GAP_CELLS
        }
        return (cells - GLYPH_GAP_CELLS) * pitch
    }

    /** Rendered height in px of any glyph in [glyphs] (all rows in a set share a height). */
    fun measureHeight(glyphs: PixelFont.GlyphSet, pitch: Float): Float {
        val rows = glyphs.glyphs.values.firstOrNull()?.size ?: 0
        return rows * pitch
    }

    private fun cellWidthOf(c: Char, glyphs: PixelFont.GlyphSet): Int = when (c) {
        '.' -> PixelFont.DOT_WIDTH
        ':' -> PixelFont.STATUS_COLON_WIDTH
        else -> glyphs.width
    }

    private fun patternOf(c: Char, glyphs: PixelFont.GlyphSet): List<String>? = when (c) {
        '.' -> PixelFont.dot
        ':' -> PixelFont.statusColon
        else -> glyphs.glyphs[c]
    }

    /**
     * Draws [text] with its left edge at [x] and top edge at [y]. Unknown characters are skipped
     * rather than throwing -- a renderer must never crash the watch face over an unexpected glyph.
     */
    fun drawText(
        canvas: Canvas,
        text: String,
        glyphs: PixelFont.GlyphSet,
        x: Float,
        y: Float,
        pitch: Float,
        litPaint: Paint,
        unlitPaint: Paint? = null,
    ) {
        var cursor = x
        for (c in text) {
            val pattern = patternOf(c, glyphs)
            if (pattern != null) {
                drawGlyph(canvas, pattern, cursor, y, pitch, litPaint, unlitPaint)
            }
            cursor += (cellWidthOf(c, glyphs) + GLYPH_GAP_CELLS) * pitch
        }
    }

    /**
     * Draws one glyph pattern with its top-left cell at ([x], [y]).
     *
     * [unlitPaint] is optional and omitted in ambient: an always-on dark grid is both a burn-in
     * pattern and wasted power, so ambient draws only the dots that are actually lit.
     */
    fun drawGlyph(
        canvas: Canvas,
        pattern: List<String>,
        x: Float,
        y: Float,
        pitch: Float,
        litPaint: Paint,
        unlitPaint: Paint? = null,
    ) {
        val radius = pitch * DOT_RADIUS_RATIO
        for (row in pattern.indices) {
            val line = pattern[row]
            val cy = y + row * pitch + pitch / 2f
            for (col in line.indices) {
                val paint = if (line[col] == '1') litPaint else unlitPaint ?: continue
                canvas.drawCircle(x + col * pitch + pitch / 2f, cy, radius, paint)
            }
        }
    }
}
