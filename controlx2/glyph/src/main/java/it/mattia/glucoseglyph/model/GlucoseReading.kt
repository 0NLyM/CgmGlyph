package it.mattia.glucoseglyph.model

// Trend and PUMP_EPOCH_OFFSET_SECONDS moved to the dependency-free :pixelfont module, because
// PixelFont.arrowSets is keyed by Trend and the watch faces need both without pulling in this
// module (and the Nothing SDK behind it). Re-exported here as typealias/const so every existing
// `it.mattia.glucoseglyph.model.Trend` reference keeps resolving unchanged.
typealias Trend = it.mattia.pixelfont.Trend

const val PUMP_EPOCH_OFFSET_SECONDS = it.mattia.pixelfont.PUMP_EPOCH_OFFSET_SECONDS

data class GlucoseReading(
    val mgdl: Int,
    val trend: Trend,
    /** Real-world Unix epoch millis of the CGM reading itself (pump timestamp, converted). */
    val readingEpochMillis: Long,
    /** Local device time when this app received/parsed the reading. */
    val receivedEpochMillis: Long,
    /** False when the pump reports mgdl == 0, ControlX2's own convention for "no CGM connected"
     * (shown there as "n/a"). */
    val valid: Boolean,
    /** True when HomeScreenMirrorResponse.cgmAlertIconId is REPLACE_SENSOR (11) -- the pump's own
     * "sensor expired, insert a new one" alert, the same one it shows on its Dashboard. */
    val sensorExpired: Boolean = false,
    /** The pump's own battery level (CurrentBatteryV1Response.currentBatteryIbc), 0-100, or null
     * if that message wasn't in the batch response. */
    val pumpBatteryPercent: Int? = null,
    /** Insulin units remaining in the pump's reservoir/cartridge
     * (InsulinStatusResponse.currentInsulinAmount), or null if unavailable. */
    val reservoirUnits: Int? = null,
    /** Real-world Unix epoch millis of CGMStatusResponse.sensorStartedTimestamp -- when the
     * current CGM sensor session began, or null if unavailable. The pump doesn't report the
     * sensor's total lifespan itself, so "days remaining" is computed from this plus the
     * user-configured AppSettings.sensorDurationDays. */
    val sensorStartedEpochMillis: Long? = null
) {
    fun mmol(): Double = mgdl / 18.0182
}
