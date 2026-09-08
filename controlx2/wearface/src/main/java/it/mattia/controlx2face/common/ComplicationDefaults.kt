package it.mattia.controlx2face.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy

/**
 * Picks what each complication slot shows before the user has chosen anything.
 *
 * ControlX2 already ships CGM/IOB/battery complication providers, and a face that defaults to
 * them is useful the moment it is selected rather than after a trip through the editor. They
 * can't be referenced as classes: they live in the `:wear` app module, and this is a library it
 * depends on, so the edge only runs one way. Referencing them by name instead means a future
 * upstream rename can't break the build -- but it also can't be caught by the compiler, hence
 * the [PackageManager][android.content.pm.PackageManager] probe and the system fallback: a
 * missing provider degrades to something sensible instead of an empty slot.
 */
object ComplicationDefaults {

    private const val CONTROLX2_COMPLICATIONS = "com.jwoglom.controlx2.complications"

    const val CGM_READING = "$CONTROLX2_COMPLICATIONS.CGMReadingComplicationDataSourceService"
    const val PUMP_IOB = "$CONTROLX2_COMPLICATIONS.PumpIOBComplicationDataSourceService"
    const val PUMP_BATTERY = "$CONTROLX2_COMPLICATIONS.PumpBatteryComplicationDataSourceService"

    /**
     * A policy naming [className] in this same package as the preferred source, falling back to
     * [systemFallback] when it isn't installed.
     */
    fun policyFor(
        context: Context,
        className: String,
        defaultType: ComplicationType = ComplicationType.SHORT_TEXT,
        systemFallback: Int = SystemDataSources.NO_DATA_SOURCE,
    ): DefaultComplicationDataSourcePolicy {
        val component = ComponentName(context.packageName, className)
        return if (isInstalled(context, component)) {
            DefaultComplicationDataSourcePolicy(
                primaryDataSource = component,
                primaryDataSourceDefaultType = defaultType,
                systemDataSourceFallback = systemFallback,
                systemDataSourceFallbackDefaultType = defaultType,
            )
        } else {
            DefaultComplicationDataSourcePolicy(systemFallback, defaultType)
        }
    }

    private fun isInstalled(context: Context, component: ComponentName): Boolean =
        runCatching {
            context.packageManager
                .queryIntentServices(Intent().setComponent(component), 0)
                .isNotEmpty()
        }.getOrDefault(false)
}
