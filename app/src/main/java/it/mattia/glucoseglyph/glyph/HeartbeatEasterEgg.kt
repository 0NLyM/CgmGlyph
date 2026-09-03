package it.mattia.glucoseglyph.glyph

import android.os.Handler
import com.nothing.ketchum.GlyphMatrixManager

/**
 * A small "thank you for using the app" surprise: a pixel heart that fades in, gives a couple of
 * heartbeat pulses (fitting, for a glucose app), then fades back out. Triggered by a double shake
 * (see GlucoseToyService) -- deliberately just brightness steps on one static shape, the same
 * "brightness is the only expressive dimension" approach MatrixRenderer itself uses, so it needs
 * no new drawing primitives. Kept to a modest number of frames, spaced well apart, rather than a
 * smoother-but-denser sequence -- setMatrixFrame is an IPC call to the Glyph service, and firing
 * it in a tight burst was suspected of destabilizing that service on real hardware.
 */
object HeartbeatEasterEgg {
    private const val SIZE = MatrixRenderer.SIZE
    private const val FULL_BRIGHTNESS = 4095

    private val heart = listOf(
        "0110110",
        "1111111",
        "1111111",
        "1111111",
        "0111110",
        "0011100",
        "0001000"
    )

    // (brightness, hold-ms-before-next-frame) pairs, played in order.
    private val sequence: List<Pair<Int, Long>> = buildList {
        // Fade in.
        for (b in listOf(0, 1200, 2600, FULL_BRIGHTNESS)) add(b to 150L)
        // Heartbeat: lub-dub.
        add(1600 to 150L)
        add(FULL_BRIGHTNESS to 150L)
        add(FULL_BRIGHTNESS to 350L)
        // Fade out.
        for (b in listOf(2600, 1200, 0)) add(b to 150L)
    }

    private fun frame(brightness: Int): IntArray {
        val grid = IntArray(SIZE * SIZE)
        val x0 = (SIZE - heart[0].length) / 2
        val y0 = (SIZE - heart.size) / 2
        for (row in heart.indices) {
            for (col in heart[row].indices) {
                if (heart[row][col] != '1') continue
                grid[(y0 + row) * SIZE + (x0 + col)] = brightness
            }
        }
        return grid
    }

    /** Plays the animation on [manager] via [handler], then calls [onFinished] (the caller's cue
     * to redraw its normal content) once the last frame has held its full duration. */
    fun play(manager: GlyphMatrixManager, handler: Handler, onFinished: () -> Unit) {
        fun step(index: Int) {
            if (index >= sequence.size) {
                onFinished()
                return
            }
            val (brightness, holdMs) = sequence[index]
            try {
                manager.setMatrixFrame(frame(brightness))
            } catch (_: Exception) {
                onFinished()
                return
            }
            handler.postDelayed({ step(index + 1) }, holdMs)
        }
        step(0)
    }
}
