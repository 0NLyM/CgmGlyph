package it.mattia.glucoseglyph.model

/** Seconds between the Unix epoch and the Tandem pump's own epoch (2008-01-01T00:00:00Z). */
const val PUMP_EPOCH_OFFSET_SECONDS = 1_199_145_600L

enum class Trend {
    DOUBLE_UP, SINGLE_UP, FORTY_FIVE_UP,
    FLAT,
    FORTY_FIVE_DOWN, SINGLE_DOWN, DOUBLE_DOWN,
    UNKNOWN;

    companion object {
        /**
         * The pump's own trend arrow, from HomeScreenMirrorResponse.cgmTrendIconId -- the exact
         * same icon ControlX2 shows on its Dashboard/home screen. Preferred over [fromTrendRate].
         * Mirrors pumpX2's HomeScreenMirrorResponse.CGMTrendIcon id values.
         */
        fun fromCgmTrendIconId(id: Int): Trend = when (id) {
            1 -> DOUBLE_UP
            2 -> SINGLE_UP
            3 -> FORTY_FIVE_UP
            4 -> FLAT
            5 -> FORTY_FIVE_DOWN
            6 -> SINGLE_DOWN
            7 -> DOUBLE_DOWN
            else -> UNKNOWN // 0 = NO_ARROW (no pump icon), or an id we don't recognise
        }

        /**
         * Fallback used only when HomeScreenMirrorResponse isn't available: derives a direction
         * from CurrentEGVGuiDataResponse.trendRate using the same thresholds ControlX2 itself uses
         * for its xDrip broadcast (XdripSgvPayload.directionFromTrendRate). Less accurate than the
         * pump's own icon -- e.g. it won't ever report "no data" -- so [fromCgmTrendIconId] wins
         * whenever both are available.
         */
        fun fromTrendRate(trendRate: Int): Trend = when {
            trendRate <= -3 -> DOUBLE_DOWN
            trendRate <= -2 -> SINGLE_DOWN
            trendRate <= -1 -> FORTY_FIVE_DOWN
            trendRate < 1 -> FLAT
            trendRate < 2 -> FORTY_FIVE_UP
            trendRate < 3 -> SINGLE_UP
            else -> DOUBLE_UP
        }
    }
}

data class GlucoseReading(
    val mgdl: Int,
    val trend: Trend,
    /** Real-world Unix epoch millis of the CGM reading itself (pump timestamp, converted). */
    val readingEpochMillis: Long,
    /** Local device time when this app received/parsed the reading. */
    val receivedEpochMillis: Long,
    val valid: Boolean
) {
    fun mmol(): Double = mgdl / 18.0182
}
