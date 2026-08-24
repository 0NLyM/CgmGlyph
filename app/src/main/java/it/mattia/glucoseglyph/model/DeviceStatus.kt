package it.mattia.glucoseglyph.model

import android.content.Context
import android.os.BatteryManager

/** Current phone battery level, 0-100, or null if the platform can't report it. */
fun currentBatteryPercent(context: Context): Int? {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return level.takeIf { it in 0..100 }
}
