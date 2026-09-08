package it.mattia.controlx2face.data

import android.content.Context
import android.content.SharedPreferences

class FacePrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "controlx2face_prefs",
        Context.MODE_PRIVATE,
    )

    fun getSnapshot(): FaceSnapshot {
        val glucoseMgdl = prefs.getInt("glucose_mgdl", -1).takeIf { it >= 0 }
        val trendOrdinal = prefs.getInt("trend_ordinal", -1).takeIf { it >= 0 }
        val trend = trendOrdinal?.let { Trend.entries.getOrNull(it) }
        val iobUnits = prefs.getFloat("iob_units", -1f).takeIf { it >= 0 }
        val batteryPercent = prefs.getInt("battery_percent", -1).takeIf { it >= 0 }
        val sensorDaysRemaining = prefs.getInt("sensor_days_remaining", -1).takeIf { it >= 0 }
        val lastUpdateSeconds = prefs.getLong("last_update_seconds", 0)
        val unitOrdinal = prefs.getInt("glucose_unit", 0)
        val unit = GlucoseUnit.entries.getOrNull(unitOrdinal) ?: GlucoseUnit.MG_DL

        return FaceSnapshot(
            glucoseMgdl = glucoseMgdl,
            glucoseUnit = unit,
            trend = trend,
            iobUnits = iobUnits,
            batteryPercent = batteryPercent,
            sensorDaysRemaining = sensorDaysRemaining,
            lastUpdateEpochSeconds = lastUpdateSeconds,
        )
    }

    fun setSnapshot(snapshot: FaceSnapshot) {
        prefs.edit().apply {
            if (snapshot.glucoseMgdl != null) {
                putInt("glucose_mgdl", snapshot.glucoseMgdl)
            } else {
                remove("glucose_mgdl")
            }
            if (snapshot.trend != null) {
                putInt("trend_ordinal", snapshot.trend.ordinal)
            } else {
                remove("trend_ordinal")
            }
            if (snapshot.iobUnits != null) {
                putFloat("iob_units", snapshot.iobUnits)
            } else {
                remove("iob_units")
            }
            if (snapshot.batteryPercent != null) {
                putInt("battery_percent", snapshot.batteryPercent)
            } else {
                remove("battery_percent")
            }
            if (snapshot.sensorDaysRemaining != null) {
                putInt("sensor_days_remaining", snapshot.sensorDaysRemaining)
            } else {
                remove("sensor_days_remaining")
            }
            putInt("glucose_unit", snapshot.glucoseUnit.ordinal)
            putLong("last_update_seconds", snapshot.lastUpdateEpochSeconds)
            apply()
        }
    }
}
