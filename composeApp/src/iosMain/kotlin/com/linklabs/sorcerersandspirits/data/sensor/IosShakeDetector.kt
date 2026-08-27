package com.linklabs.sorcerersandspirits.data.sensor

import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.sqrt

private const val SHAKE_G_FORCE_THRESHOLD = 1.2
private const val SHAKE_DEBOUNCE_SECONDS = 0.08
private const val ACCELEROMETER_UPDATE_INTERVAL_SECONDS = 1.0 / 60.0

@OptIn(ExperimentalForeignApi::class)
class IosShakeDetector : ShakeDetector {

    private val motionManager = CMMotionManager()

    override val shakes: Flow<Unit> = callbackFlow {
        if (!motionManager.accelerometerAvailable) {
            channel.close()
            return@callbackFlow
        }

        var lastShakeTimestamp = 0.0

        motionManager.accelerometerUpdateInterval = ACCELEROMETER_UPDATE_INTERVAL_SECONDS
        motionManager.startAccelerometerUpdatesToQueue(queue = NSOperationQueue()) { data, _ ->
            if (data == null) return@startAccelerometerUpdatesToQueue

            val gForce = data.acceleration.useContents { sqrt(x * x + y * y + z * z) }
            val isShake = gForce > SHAKE_G_FORCE_THRESHOLD
            if (!isShake) return@startAccelerometerUpdatesToQueue

            val isDebounced = data.timestamp - lastShakeTimestamp < SHAKE_DEBOUNCE_SECONDS
            if (isDebounced) return@startAccelerometerUpdatesToQueue

            lastShakeTimestamp = data.timestamp
            trySend(element = Unit)
        }

        awaitClose { motionManager.stopAccelerometerUpdates() }
    }
}
