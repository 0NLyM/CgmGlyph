package it.mattia.controlx2face.data

import android.util.Log
import com.jwoglom.controlx2.shared.CGMReadingResponse
import com.jwoglom.controlx2.shared.CGMStatusResponse
import com.jwoglom.controlx2.shared.HomeScreenMirrorResponse
import com.jwoglom.controlx2.shared.InsulinStatusResponse
import com.jwoglom.controlx2.shared.Parcelable
import com.jwoglom.controlx2.shared.BatteryStatusResponse
import it.mattia.pixelfont.Trend

class FacePumpMessageBridge(private val prefs: FacePrefs) {
    fun processPumpMessage(message: Parcelable) {
        val current = prefs.getSnapshot()
        val updated = when (message) {
            is CGMStatusResponse -> {
                Log.d(TAG, "CGMStatusResponse: age=${message.sensorAge} expires=${message.sensorExpires}")
                val days = estimateSensorDays(message.sensorAge, message.sensorExpires)
                current.copy(sensorDaysRemaining = days)
            }
            is CGMReadingResponse -> {
                Log.d(TAG, "CGMReadingResponse: sgv=${message.sgv} trend=${message.trend}")
                val trendEnum = trendFromCode(message.trend)
                current.copy(
                    glucoseMgdl = message.sgv,
                    trend = trendEnum,
                    lastUpdateEpochSeconds = System.currentTimeMillis() / 1000,
                )
            }
            is InsulinStatusResponse -> {
                Log.d(TAG, "InsulinStatusResponse: reservoir=${message.reservoirAmount}")
                current.copy(iobUnits = message.reservoirAmount.toFloat())
            }
            is BatteryStatusResponse -> {
                Log.d(TAG, "BatteryStatusResponse: percent=${message.percent}")
                current.copy(batteryPercent = message.percent)
            }
            is HomeScreenMirrorResponse -> {
                Log.d(TAG, "HomeScreenMirrorResponse: cgmAlertIcon=${message.cgmAlertIcon}")
                val trendEnum = trendFromHomeScreenAlert(message.cgmAlertIcon)
                current.copy(trend = trendEnum)
            }
            else -> {
                Log.d(TAG, "Unhandled message type: ${message.javaClass.simpleName}")
                return
            }
        }
        prefs.setSnapshot(updated)
    }

    private fun estimateSensorDays(sensorAge: Int?, sensorExpires: Int?): Int? {
        if (sensorAge == null || sensorExpires == null) return null
        val minutesRemaining = sensorExpires - sensorAge
        val daysRemaining = minutesRemaining / 60 / 24
        return if (daysRemaining > 0) daysRemaining else 0
    }

    private fun trendFromCode(code: Int?): Trend? {
        return when (code) {
            1 -> Trend.ARROW_UP_UP
            2 -> Trend.ARROW_UP
            3 -> Trend.ARROW_UP_45
            4 -> Trend.FLAT
            5 -> Trend.ARROW_DOWN_45
            6 -> Trend.ARROW_DOWN
            7 -> Trend.ARROW_DOWN_DOWN
            else -> null
        }
    }

    private fun trendFromHomeScreenAlert(cgmAlertIcon: Int?): Trend? {
        return when (cgmAlertIcon) {
            1 -> Trend.ARROW_UP_UP
            2 -> Trend.ARROW_UP
            3 -> Trend.ARROW_UP_45
            4 -> Trend.FLAT
            5 -> Trend.ARROW_DOWN_45
            6 -> Trend.ARROW_DOWN
            7 -> Trend.ARROW_DOWN_DOWN
            else -> null
        }
    }

    companion object {
        private const val TAG = "FacePumpMessageBridge"
    }
}
