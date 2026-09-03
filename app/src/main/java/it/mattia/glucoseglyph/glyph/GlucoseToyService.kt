package it.mattia.glucoseglyph.glyph

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import it.mattia.glucoseglyph.model.AppSettings
import it.mattia.glucoseglyph.model.GlucoseState
import it.mattia.glucoseglyph.model.currentBatteryPercent

/**
 * The Glyph Toy shown in the Phone (3)'s Glyph Toys carousel / AOD. Draws whatever
 * [GlucoseState] currently holds -- plus a small clock and the phone's battery level -- and
 * redraws immediately whenever [GlucosePollingService] publishes a new reading, once a minute
 * while on the always-on display (EVENT_AOD), and every 30s while actively shown so the clock
 * doesn't go stale between glucose polls.
 *
 * A plain press of the Glyph button isn't ours to use -- on real hardware the system consumes it
 * to switch to the next Glyph Toy in the carousel before our own press/release handling ever gets
 * a meaningful look at it -- so onTouchPointPressed/onTouchPointReleased are deliberately left
 * unused for that case (no timer is ever scheduled off a plain press; that was what destabilized
 * an earlier version of this). Only two gestures are used:
 *  - shaking the phone cycles [ToyDisplayMode] -- glucose (default) -> pump battery -> reservoir
 *    units -> sensor days remaining and back -- and a display mode other than glucose reverts to
 *    glucose on its own after [REVERT_TO_GLUCOSE_MS] of no further shake;
 *  - the Glyph button's long press tells two holds apart purely from event timestamps (the SDK
 *    only offers "long press was reached" and "released" as discrete signals, not a live
 *    duration while still held, so no continuous "charging up" readout is possible): released
 *    soon after the long-press threshold toggles mg/dL<->mmol/L, same as before; released only
 *    after holding substantially longer plays the heartbeat easter egg instead.
 */
class GlucoseToyService : GlyphMatrixServiceBase("Glucose-Toy") {

    private val settings by lazy { AppSettings(this) }
    private val onReadingChanged: () -> Unit = { redraw() }
    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            redraw()
            tickHandler.postDelayed(this, CLOCK_TICK_MS)
        }
    }
    private val shakeDetector by lazy { ShakeDetector(this) { onShake() } }
    private var displayMode = ToyDisplayMode.GLUCOSE
    private var easterEggPlaying = false

    // Set only when onTouchPointLongPress actually fires -- 0 means "this release wasn't preceded
    // by a long press" (e.g. a plain press the system already handled itself), in which case
    // onTouchPointReleased does nothing at all.
    private var longPressStartMillis = 0L

    private val revertToGlucoseRunnable = Runnable {
        displayMode = ToyDisplayMode.GLUCOSE
        redraw()
    }

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        displayMode = ToyDisplayMode.GLUCOSE
        redraw()
        GlucoseState.addListener(onReadingChanged)
        tickHandler.postDelayed(tickRunnable, CLOCK_TICK_MS)
        shakeDetector.start()
    }

    override fun performOnServiceDisconnected(context: Context) {
        GlucoseState.removeListener(onReadingChanged)
        tickHandler.removeCallbacksAndMessages(null)
        shakeDetector.stop()
    }

    override fun onAodTick() {
        redraw()
    }

    override fun onTouchPointLongPress() {
        longPressStartMillis = System.currentTimeMillis()
    }

    override fun onTouchPointReleased() {
        val startedAt = longPressStartMillis
        longPressStartMillis = 0L
        if (startedAt == 0L) return
        if (System.currentTimeMillis() - startedAt >= EASTER_EGG_EXTRA_HOLD_MS) {
            playEasterEgg()
        } else {
            settings.useMmol = !settings.useMmol
            redraw()
        }
    }

    private fun onShake() {
        if (easterEggPlaying) return
        cycleDisplayMode()
    }

    private fun cycleDisplayMode() {
        displayMode = displayMode.next()
        tickHandler.removeCallbacks(revertToGlucoseRunnable)
        if (displayMode != ToyDisplayMode.GLUCOSE) {
            tickHandler.postDelayed(revertToGlucoseRunnable, REVERT_TO_GLUCOSE_MS)
        }
        redraw()
    }

    private fun playEasterEgg() {
        val manager = glyphMatrixManager ?: return
        tickHandler.removeCallbacks(revertToGlucoseRunnable)
        easterEggPlaying = true
        HeartbeatEasterEgg.play(manager, tickHandler) {
            easterEggPlaying = false
            if (displayMode != ToyDisplayMode.GLUCOSE) {
                tickHandler.postDelayed(revertToGlucoseRunnable, REVERT_TO_GLUCOSE_MS)
            }
            redraw()
        }
    }

    private fun redraw() {
        val manager = glyphMatrixManager ?: return
        if (easterEggPlaying) return
        val frame = MatrixRenderer.render(
            reading = GlucoseState.current,
            useMmol = settings.useMmol,
            batteryPercent = currentBatteryPercent(applicationContext),
            includeStatusRows = true,
            valueDigitStyle = settings.valueDigitStyle,
            clockDigitStyle = settings.clockDigitStyle,
            arrowStyle = settings.arrowStyle,
            displayMode = displayMode,
            sensorDurationDays = settings.sensorDurationDays
        )
        try {
            manager.setMatrixFrame(frame)
        } catch (e: Exception) {
            Log.w(TAG, "setMatrixFrame failed", e)
        }
    }

    private companion object {
        const val TAG = "GlucoseToyService"
        const val CLOCK_TICK_MS = 30_000L
        const val REVERT_TO_GLUCOSE_MS = 15_000L
        // How much longer than the SDK's own long-press threshold you need to keep holding,
        // measured from onTouchPointLongPress to onTouchPointReleased, for it to count as "held
        // long enough" for the easter egg instead of the unit toggle.
        const val EASTER_EGG_EXTRA_HOLD_MS = 1200L
    }
}
