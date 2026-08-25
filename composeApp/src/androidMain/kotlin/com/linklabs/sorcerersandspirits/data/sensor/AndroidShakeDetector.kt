package com.linklabs.sorcerersandspirits.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

private const val SHAKE_G_FORCE_THRESHOLD = 1.2f
private const val SHAKE_DEBOUNCE_MILLIS = 80L

class AndroidShakeDetector(context: Context) : ShakeDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val shakes: Flow<Unit> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            channel.close()
            return@callbackFlow
        }

        var lastShakeTimestamp = 0L

        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val gForce = event.values.gForceMagnitude()
                    val isShake = gForce > SHAKE_G_FORCE_THRESHOLD
                    if (!isShake) return

                    val now = event.timestampMillis()
                    val isDebounced = now - lastShakeTimestamp < SHAKE_DEBOUNCE_MILLIS
                    if (isDebounced) return

                    lastShakeTimestamp = now
                    trySend(element = Unit)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun FloatArray.gForceMagnitude(): Float {
        val x = this[0] / SensorManager.GRAVITY_EARTH
        val y = this[1] / SensorManager.GRAVITY_EARTH
        val z = this[2] / SensorManager.GRAVITY_EARTH
        return sqrt(x * x + y * y + z * z)
    }

    // Sensor timestamps are nanoseconds since boot; collapse to millis for debouncing.
    private fun SensorEvent.timestampMillis(): Long = timestamp / 1_000_000L
}
