package it.mattia.controlx2face.data

import it.mattia.pixelfont.Trend
import kotlin.time.Duration

data class FaceSnapshot(
    val glucoseMgdl: Int?,
    val glucoseUnit: GlucoseUnit,
    val trend: Trend?,
    val iobUnits: Float?,
    val batteryPercent: Int?,
    val sensorDaysRemaining: Int?,
    val lastUpdateEpochSeconds: Long,
) {
    val staleness: Staleness
        get() {
            val ageSeconds = System.currentTimeMillis() / 1000 - lastUpdateEpochSeconds
            return when {
                ageSeconds < 600 -> Staleness.FRESH
                ageSeconds < 1800 -> Staleness.STALE
                else -> Staleness.DEAD
            }
        }

    val range: GlucoseRange
        get() {
            val mg = glucoseMgdl ?: return GlucoseRange.UNKNOWN
            return when {
                mg < 70 -> GlucoseRange.LOW
                mg > 180 -> GlucoseRange.HIGH
                else -> GlucoseRange.IN_RANGE
            }
        }
}

enum class Staleness {
    FRESH,
    STALE,
    DEAD,
}

enum class GlucoseRange {
    IN_RANGE,
    LOW,
    HIGH,
    UNKNOWN,
}

enum class GlucoseUnit {
    MG_DL,
    MMOL_L,
}
