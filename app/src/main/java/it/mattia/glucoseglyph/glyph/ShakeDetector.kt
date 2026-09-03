package it.mattia.glucoseglyph.glyph

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Fires [onShake] when the phone is shaken while listening -- a simple magnitude-over-gravity
 * threshold with a cooldown so one shake doesn't fire repeatedly. Start/stop alongside whatever
 * scope should be listening (here, GlucoseToyService only listens while the Glyph Toy is actually
 * bound/visible).
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeMillis = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z) = event.values
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
            if (gForce < SHAKE_THRESHOLD_G) return
            val now = System.currentTimeMillis()
            if (now - lastShakeMillis < COOLDOWN_MS) return
            lastShakeMillis = now
            onShake()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(listener)
    }

    private companion object {
        const val SHAKE_THRESHOLD_G = 2.2
        const val COOLDOWN_MS = 1000L
    }
}
