package it.mattia.controlx2face.minimalgrid

import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import it.mattia.controlx2face.R
import it.mattia.controlx2face.common.ComplicationDefaults

/** Slot ids. Stable across releases: they key the user's own complication choices. */
private const val SLOT_CELL_LEFT = 100
private const val SLOT_CELL_CENTRE = 101
private const val SLOT_CELL_RIGHT = 102
private const val SLOT_TREND = 103

/**
 * "Minimal Grid" -- the flat, functional CMF-styled face: a large light time, a hairline rule, and
 * a row of three data cells beneath it.
 *
 * Every cell is an ordinary editable complication slot rather than hardcoded pump data. That is
 * what makes the face useful on day one (ControlX2's existing CGM/IOB/battery providers are just
 * the defaults) and still useful to someone who wants steps or the weather in one of them.
 */
class MinimalGridWatchFaceService : WatchFaceService() {

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): ComplicationSlotsManager {
        val factory = CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(applicationContext, R.drawable.wf_complication_style)!!,
                watchState,
                listener,
            )
        }

        fun slot(id: Int, rect: MinimalGridLayout.Rect, className: String, types: List<ComplicationType>) =
            ComplicationSlot.createRoundRectComplicationSlotBuilder(
                id = id,
                canvasComplicationFactory = factory,
                supportedTypes = types,
                defaultDataSourcePolicy = ComplicationDefaults.policyFor(applicationContext, className),
                bounds = ComplicationSlotBounds(RectF(rect.left, rect.top, rect.right, rect.bottom)),
            ).build()

        val (left, centre, right) = MinimalGridLayout.cells
        val textAndRange = listOf(ComplicationType.SHORT_TEXT, ComplicationType.RANGED_VALUE)

        return ComplicationSlotsManager(
            listOf(
                slot(SLOT_CELL_LEFT, left, ComplicationDefaults.CGM_READING, textAndRange),
                slot(SLOT_CELL_CENTRE, centre, ComplicationDefaults.PUMP_IOB, textAndRange),
                slot(SLOT_CELL_RIGHT, right, ComplicationDefaults.PUMP_BATTERY, textAndRange),
                slot(
                    SLOT_TREND,
                    MinimalGridLayout.trendSlot,
                    ComplicationDefaults.CGM_READING,
                    listOf(ComplicationType.SHORT_TEXT, ComplicationType.MONOCHROMATIC_IMAGE),
                ),
            ),
            currentUserStyleRepository,
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): WatchFace {
        val renderer = MinimalGridRenderer(
            surfaceHolder = surfaceHolder,
            currentUserStyleRepository = currentUserStyleRepository,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            canvasType = CanvasType.HARDWARE,
            context = this,
        )
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}
