package it.mattia.glucoseglyph.model

import android.content.Context
import android.content.SharedPreferences
import it.mattia.glucoseglyph.glyph.PixelFont

/** Thin wrapper around SharedPreferences holding the Glyph Toy's display personalization. */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("glucose_glyph_prefs", Context.MODE_PRIVATE)

    var useMmol: Boolean
        get() = prefs.getBoolean(KEY_USE_MMOL, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_MMOL, value).apply()

    // --- Personalizzazione: which glyph style to draw with, per element ---

    var arrowStyle: PixelFont.ArrowStyle
        get() = readEnum(KEY_ARROW_STYLE, PixelFont.ArrowStyle.entries, PixelFont.ArrowStyle.CURRENT)
            // A persisted style whose glyph set isn't (or is no longer) drawn in reads back as
            // CURRENT, so the app never resurrects a selection it can't render.
            .takeIf { it in PixelFont.arrowSets } ?: PixelFont.ArrowStyle.CURRENT
        set(value) = prefs.edit().putString(KEY_ARROW_STYLE, value.name).apply()

    var clockDigitStyle: PixelFont.DigitStyle
        get() = readEnum(KEY_CLOCK_DIGIT_STYLE, PixelFont.DigitStyle.entries, PixelFont.DigitStyle.CURRENT)
        set(value) = prefs.edit().putString(KEY_CLOCK_DIGIT_STYLE, value.name).apply()

    var valueDigitStyle: PixelFont.DigitStyle
        get() = readEnum(KEY_VALUE_DIGIT_STYLE, PixelFont.DigitStyle.entries, PixelFont.DigitStyle.CURRENT)
        set(value) = prefs.edit().putString(KEY_VALUE_DIGIT_STYLE, value.name).apply()

    // The pump doesn't report a CGM sensor's total lifespan, only when its current session
    // started -- so "days remaining" (one of the Glyph Toy's cycled display modes) needs this
    // to be told to it explicitly, picked from the Personalizzazione sheet.
    var sensorDurationDays: Int
        get() = prefs.getInt(KEY_SENSOR_DURATION_DAYS, 10)
        set(value) = prefs.edit().putInt(KEY_SENSOR_DURATION_DAYS, value).apply()

    private fun <T : Enum<T>> readEnum(key: String, values: List<T>, default: T): T =
        prefs.getString(key, null)?.let { saved -> values.find { it.name == saved } } ?: default

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        const val KEY_USE_MMOL = "use_mmol"
        const val KEY_ARROW_STYLE = "arrow_style"
        const val KEY_CLOCK_DIGIT_STYLE = "clock_digit_style"
        const val KEY_VALUE_DIGIT_STYLE = "value_digit_style"
        const val KEY_SENSOR_DURATION_DAYS = "sensor_duration_days"
    }
}
