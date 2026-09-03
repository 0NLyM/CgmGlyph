package it.mattia.glucoseglyph.bridge

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CGMStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBatteryAbstractResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentEGVGuiDataResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HomeScreenMirrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.GlucoseState
import it.mattia.glucoseglyph.model.PUMP_EPOCH_OFFSET_SECONDS
import it.mattia.glucoseglyph.model.Trend

/**
 * Feeds [GlucoseState] straight from the same in-process pump message stream CommService already
 * receives over its live BLE connection -- no HTTP, no polling, no separate process. Wired in by a
 * single line in CommService's onPumpMessageReceived override (see that file), so this is the only
 * place ControlX2's own message flow needed to be touched.
 *
 * Each message type updates one part of the picture and re-publishes immediately, rather than
 * waiting for a full batch like the old HTTP client did -- e.g. a battery update between CGM
 * readings still refreshes the Glyph Toy's battery display right away.
 */
object PumpMessageBridge {

    @Volatile private var cgmReading: Int? = null
    @Volatile private var readingEpochMillis: Long = 0
    @Volatile private var trendRateFallback: Int = 0
    @Volatile private var trendIconId: Int? = null
    @Volatile private var alertIconId: Int? = null
    @Volatile private var pumpBatteryPercent: Int? = null
    @Volatile private var reservoirUnits: Int? = null
    @Volatile private var sensorStartedEpochMillis: Long? = null

    fun onPumpMessageReceived(message: Message) {
        when (message) {
            is CurrentEGVGuiDataResponse -> {
                cgmReading = message.cgmReading
                readingEpochMillis = (message.bgReadingTimestampSeconds + PUMP_EPOCH_OFFSET_SECONDS) * 1000L
                trendRateFallback = message.trendRate
            }
            is HomeScreenMirrorResponse -> {
                trendIconId = message.cgmTrendIcon?.id()
                alertIconId = message.cgmAlertIcon?.id()
            }
            is CurrentBatteryAbstractResponse -> pumpBatteryPercent = message.batteryPercent
            is InsulinStatusResponse -> reservoirUnits = message.currentInsulinAmount
            is CGMStatusResponse -> sensorStartedEpochMillis =
                message.sensorStartedTimestamp.takeIf { it > 0 }?.let { message.sensorStartedTimestampInstant.toEpochMilli() }
            else -> return
        }
        publish()
    }

    private fun publish() {
        val mgdl = cgmReading ?: return
        // Same preference as the old HTTP client: the pump's own trend icon (matches ControlX2's
        // UI exactly) over the trendRate heuristic, used only until a HomeScreenMirrorResponse
        // has arrived at least once.
        val trend = trendIconId?.let { Trend.fromCgmTrendIconId(it) } ?: Trend.fromTrendRate(trendRateFallback)
        GlucoseState.update(
            GlucoseReading(
                mgdl = mgdl,
                trend = trend,
                readingEpochMillis = readingEpochMillis,
                receivedEpochMillis = System.currentTimeMillis(),
                valid = mgdl > 0,
                // HomeScreenMirrorResponse.CGMAlertIcon.REPLACE_SENSOR == 11.
                sensorExpired = alertIconId == 11,
                pumpBatteryPercent = pumpBatteryPercent,
                reservoirUnits = reservoirUnits,
                sensorStartedEpochMillis = sensorStartedEpochMillis
            )
        )
    }
}
