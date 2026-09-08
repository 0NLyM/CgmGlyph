package it.mattia.controlx2face.dotmatrix

import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository

/**
 * "Dot Matrix" -- the watch-side counterpart to the phone's Glyph Matrix toy, drawing the time and
 * (once the data layer lands) the glucose reading from the very same [PixelFont] glyph tables, so
 * the two surfaces read as one product rather than two lookalikes.
 *
 * Skeleton at this stage: clock only. The data layer, trend arrow, ambient treatment and
 * out-of-range accent arrive in later commits.
 */
class DotMatrixWatchFaceService : WatchFaceService() {

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): WatchFace {
        val renderer = DotMatrixRenderer(
            surfaceHolder = surfaceHolder,
            currentUserStyleRepository = currentUserStyleRepository,
            watchState = watchState,
            canvasType = CanvasType.HARDWARE,
        )
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}
