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
 * Long-pressing the toy toggles between mg/dL and mmol/L, matching the long-press "cycle"
 * convention used by other Glyph Toys. A plain press of the Glyph button, or shaking the phone,
 * instead cycles [ToyDisplayMode] -- glucose (default) -> pump battery -> reservoir units and
 * back -- always starting back at glucose whenever the toy is (re)bound.
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
    private val shakeDetector by lazy { ShakeDetector(this) { cycleDisplayMode() } }
    private var displayMode = ToyDisplayMode.GLUCOSE
    // EVENT_ACTION_DOWN always fires first, even for what turns into a long press, so a plain
    // "cycle on press" would also fire on every long-press-to-toggle-units gesture. Only cycling
    // on release, and only when no long-press was reported in between, tells a short tap apart
    // from a long hold using the SDK's own event order instead of guessing at a timing threshold.
    private var longPressHandled = false

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        displayMode = ToyDisplayMode.GLUCOSE
        redraw()
        GlucoseState.addListener(onReadingChanged)
        tickHandler.postDelayed(tickRunnable, CLOCK_TICK_MS)
        shakeDetector.start()
    }

    override fun performOnServiceDisconnected(context: Context) {
        GlucoseState.removeListener(onReadingChanged)
        tickHandler.removeCallbacks(tickRunnable)
        shakeDetector.stop()
    }

    override fun onAodTick() {
        redraw()
    }

    override fun onTouchPointPressed() {
        longPressHandled = false
    }

    override fun onTouchPointLongPress() {
        longPressHandled = true
        settings.useMmol = !settings.useMmol
        redraw()
    }

    override fun onTouchPointReleased() {
        if (!longPressHandled) cycleDisplayMode()
    }

    private fun cycleDisplayMode() {
        displayMode = displayMode.next()
        redraw()
    }

    private fun redraw() {
        val manager = glyphMatrixManager ?: return
        val frame = MatrixRenderer.render(
            reading = GlucoseState.current,
            useMmol = settings.useMmol,
            batteryPercent = currentBatteryPercent(applicationContext),
            includeStatusRows = true,
            valueDigitStyle = settings.valueDigitStyle,
            clockDigitStyle = settings.clockDigitStyle,
            arrowStyle = settings.arrowStyle,
            displayMode = displayMode
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
    }
}
