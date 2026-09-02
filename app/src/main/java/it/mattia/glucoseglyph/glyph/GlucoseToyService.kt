package it.mattia.glucoseglyph.glyph

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import it.mattia.glucoseglyph.model.AppSettings
import it.mattia.glucoseglyph.model.GlucoseState
import it.mattia.glucoseglyph.model.currentBatteryPercent
import it.mattia.glucoseglyph.net.ControlX2Client
import kotlin.concurrent.thread

/**
 * The Glyph Toy shown in the Phone (3)'s Glyph Toys carousel / AOD. Draws whatever
 * [GlucoseState] currently holds -- plus a small clock and the phone's battery level -- and
 * redraws immediately whenever [GlucosePollingService] publishes a new reading, once a minute
 * while on the always-on display (EVENT_AOD), and every 30s while actively shown so the clock
 * doesn't go stale between glucose polls.
 *
 * The Glyph button on the back of the phone (or shaking the phone) drives several gestures:
 *  - a short press cycles [ToyDisplayMode] -- glucose (default) -> pump battery -> reservoir
 *    units -> sensor days remaining and back -- always starting at glucose whenever the toy is
 *    (re)bound;
 *  - a long press (the SDK's own long-press threshold) toggles mg/dL<->mmol/L, matching the
 *    long-press "cycle" convention used by other Glyph Toys;
 *  - holding it well past that plays a small heartbeat easter egg, then returns to whatever was
 *    showing before;
 *  - a double tap forces an immediate re-poll of ControlX2 (instead of waiting for the next
 *    scheduled poll), redrawing in place on whichever display mode was already showing.
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
    // A short tap's own action (cycling the display mode) is held back briefly in case a second
    // tap arrives within the double-tap window, in which case it's cancelled in favour of a
    // refresh -- so a double tap never also cycles the mode as a side effect of its first tap.
    private val singleTapRunnable = Runnable { cycleDisplayMode() }
    private var lastShortReleaseMillis = 0L
    private val easterEggRunnable = Runnable { playEasterEgg() }
    private var easterEggPlaying = false

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
        tickHandler.removeCallbacks(singleTapRunnable)
        tickHandler.removeCallbacks(easterEggRunnable)
        shakeDetector.stop()
    }

    override fun onAodTick() {
        redraw()
    }

    override fun onTouchPointPressed() {
        longPressHandled = false
        tickHandler.postDelayed(easterEggRunnable, EASTER_EGG_HOLD_MS)
    }

    override fun onTouchPointLongPress() {
        longPressHandled = true
        settings.useMmol = !settings.useMmol
        redraw()
    }

    override fun onTouchPointReleased() {
        tickHandler.removeCallbacks(easterEggRunnable)
        if (longPressHandled || easterEggPlaying) return

        val now = System.currentTimeMillis()
        if (now - lastShortReleaseMillis < DOUBLE_TAP_WINDOW_MS) {
            // Second tap of a double tap: the pending single-tap cycle never happened, refresh instead.
            tickHandler.removeCallbacks(singleTapRunnable)
            lastShortReleaseMillis = 0L
            refreshNow()
        } else {
            lastShortReleaseMillis = now
            tickHandler.postDelayed(singleTapRunnable, DOUBLE_TAP_WINDOW_MS)
        }
    }

    private fun cycleDisplayMode() {
        displayMode = displayMode.next()
        redraw()
    }

    /** Re-polls ControlX2 immediately instead of waiting for GlucosePollingService's own
     * schedule, redrawing on whichever display mode was already showing. Mirrors
     * GlucosePollingService.pollOnce's success/failure handling; GlucoseState.update already
     * notifies this service's own listener, which is what actually triggers the redraw. */
    private fun refreshNow() {
        thread(name = "toy-refresh") {
            val client = ControlX2Client(
                host = settings.host,
                port = settings.port,
                username = settings.username,
                password = settings.password
            )
            when (val result = client.fetchLatestReading()) {
                is ControlX2Client.FetchResult.Success -> {
                    GlucoseState.update(result.reading)
                    settings.lastMgdl = result.reading.mgdl
                    settings.lastReadingEpochMillis = result.reading.readingEpochMillis
                    settings.lastFetchOk = true
                    settings.lastError = null
                }
                is ControlX2Client.FetchResult.Failure -> {
                    GlucoseState.updateError(result.message)
                    settings.lastFetchOk = false
                    settings.lastError = result.message
                }
            }
        }
    }

    private fun playEasterEgg() {
        val manager = glyphMatrixManager ?: return
        easterEggPlaying = true
        HeartbeatEasterEgg.play(manager, tickHandler) {
            easterEggPlaying = false
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
        const val DOUBLE_TAP_WINDOW_MS = 300L
        // Comfortably past the SDK's own long-press threshold (which already fires
        // onTouchPointLongPress well before this) so a plain unit-toggle long-press never also
        // triggers the easter egg.
        const val EASTER_EGG_HOLD_MS = 2500L
    }
}
