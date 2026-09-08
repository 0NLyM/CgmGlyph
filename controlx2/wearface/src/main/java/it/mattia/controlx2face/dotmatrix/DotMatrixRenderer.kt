package it.mattia.controlx2face.dotmatrix

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import it.mattia.controlx2face.render.DotGrid
import it.mattia.controlx2face.render.FacePalette
import it.mattia.pixelfont.PixelFont
import java.time.ZonedDateTime

/**
 * Nothing there is sub-minute on this face, so it only needs redrawing once a minute; live data
 * changes drive extra redraws through [Renderer.invalidate] rather than through polling.
 */
private const val FRAME_PERIOD_MS = 60_000L

/** Cell pitch for the clock, as a fraction of the display's shorter side. */
private const val CLOCK_PITCH_RATIO = 0.0356f

/** Clock baseline, as a fraction of display height (top edge of the glyph block). */
private const val CLOCK_TOP_RATIO = 0.213f

class DotMatrixRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    canvasType: Int,
) : Renderer.CanvasRenderer2<DotMatrixRenderer.Assets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false,
) {

    class Assets : SharedAssets {
        override fun onDestroy() = Unit
    }

    override suspend fun createSharedAssets() = Assets()

    private val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FacePalette.LIT }
    private val unlitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FacePalette.UNLIT }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets) {
        val ambient = renderParameters.drawMode == DrawMode.AMBIENT
        canvas.drawColor(FacePalette.BACKGROUND)

        // Low-bit ambient quantises colours hard and antialiasing smears the result, so drop it
        // there. Everything else keeps smooth dots.
        val lowBit = ambient && watchState.hasLowBitAmbient
        litPaint.isAntiAlias = !lowBit

        val glyphs = PixelFont.clockDigitSets.getValue(PixelFont.DigitStyle.CURRENT)
        val pitch = minOf(bounds.width(), bounds.height()) * CLOCK_PITCH_RATIO
        val text = "%02d:%02d".format(zonedDateTime.hour, zonedDateTime.minute)

        val x = bounds.exactCenterX() - DotGrid.measureText(text, glyphs, pitch) / 2f
        val y = bounds.top + bounds.height() * CLOCK_TOP_RATIO

        DotGrid.drawText(
            canvas = canvas,
            text = text,
            glyphs = glyphs,
            x = x,
            y = y,
            pitch = pitch,
            litPaint = litPaint,
            // Ambient draws lit dots only: a permanently-lit dark grid is a burn-in pattern and
            // costs power for something nobody reads with the wrist down.
            unlitPaint = if (ambient) null else unlitPaint,
        )
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets) {
        // No complication slots on this face yet, so there is nothing to highlight in the editor.
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
    }
}
