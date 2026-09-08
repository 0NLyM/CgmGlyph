package it.mattia.controlx2face.minimalgrid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import it.mattia.controlx2face.data.FaceStateHolder
import it.mattia.controlx2face.data.Staleness
import it.mattia.controlx2face.render.FacePalette
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val FRAME_PERIOD_MS = 60_000L

/** Stroke width of the outlined ambient time, as a fraction of the shorter side. */
private const val AMBIENT_STROKE_RATIO = 0.0033f

class MinimalGridRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    canvasType: Int,
    private val context: Context,
) : Renderer.CanvasRenderer2<MinimalGridRenderer.Assets>(
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

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.getDefault())

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        color = FacePalette.LIT
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        color = FacePalette.SECONDARY
        letterSpacing = 0.18f
    }

    private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FacePalette.RULE }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets) {
        val ambient = renderParameters.drawMode == DrawMode.AMBIENT
        val shorterSide = minOf(bounds.width(), bounds.height()).toFloat()

        canvas.drawColor(FacePalette.BACKGROUND)

        val lowBit = ambient && watchState.hasLowBitAmbient
        timePaint.isAntiAlias = !lowBit
        datePaint.isAntiAlias = !lowBit

        if (ambient) {
            timePaint.style = Paint.Style.STROKE
            timePaint.strokeWidth = shorterSide * AMBIENT_STROKE_RATIO
        } else {
            timePaint.style = Paint.Style.FILL
        }

        timePaint.textSize = shorterSide * MinimalGridLayout.TIME_SIZE_RATIO
        val timeText = "%02d:%02d".format(zonedDateTime.hour, zonedDateTime.minute)
        canvas.drawText(
            timeText,
            bounds.exactCenterX(),
            bounds.top + bounds.height() * MinimalGridLayout.TIME_BASELINE_Y,
            timePaint,
        )

        if (!ambient) {
            val snapshot = FaceStateHolder.getInstance(context).getSnapshot()

            datePaint.textSize = shorterSide * MinimalGridLayout.DATE_SIZE_RATIO
            canvas.drawText(
                dateFormatter.format(zonedDateTime).uppercase(Locale.getDefault()),
                bounds.exactCenterX(),
                bounds.top + bounds.height() * MinimalGridLayout.DATE_BASELINE_Y,
                datePaint,
            )
            drawRules(canvas, bounds)
        }

        complicationSlotsManager.complicationSlots.values.forEach { slot ->
            if (slot.enabled) slot.render(canvas, zonedDateTime, renderParameters)
        }
    }

    private fun drawRules(canvas: Canvas, bounds: Rect) {
        val hairline = maxOf(1f, bounds.width() * 0.0022f)
        val ruleY = bounds.top + bounds.height() * MinimalGridLayout.RULE_Y
        canvas.drawRect(
            bounds.left + bounds.width() * MinimalGridLayout.RULE_LEFT,
            ruleY,
            bounds.left + bounds.width() * MinimalGridLayout.RULE_RIGHT,
            ruleY + hairline,
            rulePaint,
        )

        val top = bounds.top + bounds.height() * MinimalGridLayout.DIVIDER_TOP
        val bottom = bounds.top + bounds.height() * MinimalGridLayout.DIVIDER_BOTTOM
        MinimalGridLayout.cellDividers.forEach { x ->
            val px = bounds.left + bounds.width() * x
            canvas.drawRect(px, top, px + hairline, bottom, rulePaint)
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets) {
        canvas.drawColor(Color.TRANSPARENT)
        complicationSlotsManager.complicationSlots.values.forEach { slot ->
            if (slot.enabled) slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
        }
    }
}
