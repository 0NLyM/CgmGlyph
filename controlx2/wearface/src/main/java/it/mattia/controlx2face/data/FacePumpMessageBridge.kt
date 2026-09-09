package it.mattia.controlx2face.data

import android.util.Log
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CGMStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBatteryAbstractResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentEGVGuiDataResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HomeScreenMirrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import it.mattia.pixelfont.PUMP_EPOCH_OFFSET_SECONDS
import it.mattia.pixelfont.Trend

/** Default sensor lifetime, in the absence of a configurable duration (see Fase 7). */
private const val DEFAULT_SENSOR_DURATION_DAYS = 10L

class FacePumpMessageBridge(private val prefs: FacePrefs) {
    fun processPumpMessage(message: Message) {
        val current = prefs.getSnapshot()
        val updated = when (message) {
            is CurrentEGVGuiDataResponse -> {
                Log.d(TAG, "CurrentEGVGuiDataResponse: cgmReading=${message.cgmReading} trendRate=${message.trendRate}")
                current.copy(
                    glucoseMgdl = message.cgmReading,
                    trend = Trend.fromTrendRate(message.trendRate),
                    lastUpdateEpochSeconds = message.bgReadingTimestampSeconds + PUMP_EPOCH_OFFSET_SECONDS,
                )
            }
            is HomeScreenMirrorResponse -> {
                Log.d(TAG, "HomeScreenMirrorResponse: cgmTrendIcon=${message.cgmTrendIcon}")
                val trendEnum = message.cgmTrendIcon?.id()?.let { Trend.fromCgmTrendIconId(it) }
                if (trendEnum != null) current.copy(trend = trendEnum) else current
            }
            is CurrentBatteryAbstractResponse -> {
                Log.d(TAG, "CurrentBatteryAbstractResponse: batteryPercent=${message.batteryPercent}")
                current.copy(batteryPercent = message.batteryPercent)
            }
            is InsulinStatusResponse -> {
                Log.d(TAG, "InsulinStatusResponse: currentInsulinAmount=${message.currentInsulinAmount}")
                current.copy(iobUnits = message.currentInsulinAmount.toFloat())
            }
            is CGMStatusResponse -> {
                val startedEpochMillis = message.sensorStartedTimestamp.takeIf { it > 0 }
                    ?.let { message.sensorStartedTimestampInstant.toEpochMilli() }
                Log.d(TAG, "CGMStatusResponse: sensorStartedEpochMillis=$startedEpochMillis")
                current.copy(sensorDaysRemaining = estimateSensorDaysRemaining(startedEpochMillis))
            }
            else -> {
                Log.d(TAG, "Unhandled message type: ${message.javaClass.simpleName}")
                return
            }
        }
        prefs.setSnapshot(updated)
    }

    private fun estimateSensorDaysRemaining(startedEpochMillis: Long?): Int? {
        if (startedEpochMillis == null) return null
        val elapsedDays = (System.currentTimeMillis() - startedEpochMillis) / (1000L * 60 * 60 * 24)
        return (DEFAULT_SENSOR_DURATION_DAYS - elapsedDays).toInt().coerceAtLeast(0)
    }

    companion object {
        private const val TAG = "FacePumpMessageBridge"
    }
}
