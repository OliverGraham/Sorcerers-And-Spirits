package com.linklabs.sorcerersandspirits.data.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics

// Longer than any single roll, so the continuous buzz runs smoothly until explicitly stopped.
private const val CONTINUOUS_DURATION_MILLIS = 10_000L

class AndroidHaptics(context: Context) : Haptics {

    private val vibrator = context.resolveVibrator()

    private var isContinuousActive = false

    override fun vibrate(strength: HapticStrength) {
        if (!vibrator.hasVibrator()) return
        // A one-shot supersedes any continuous buzz, so a later stopContinuous() won't cancel it.
        isContinuousActive = false
        vibrateOneShot(durationMillis = strength.durationMillis(), amplitude = strength.amplitude())
    }

    override fun startContinuous(strength: HapticStrength) {
        val isAlreadyBuzzing = isContinuousActive
        if (!vibrator.hasVibrator() || isAlreadyBuzzing) return
        isContinuousActive = true
        vibrateOneShot(
            durationMillis = CONTINUOUS_DURATION_MILLIS,
            amplitude = strength.amplitude(),
        )
    }

    override fun stopContinuous() {
        if (!isContinuousActive) return
        isContinuousActive = false
        vibrator.cancel()
    }

    private fun vibrateOneShot(durationMillis: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
        } else {
            // createOneShot requires API 26; no non-deprecated alternative exists below it.
            @Suppress("DEPRECATION") vibrator.vibrate(durationMillis)
        }
    }

    private fun HapticStrength.durationMillis(): Long =
        when (this) {
            HapticStrength.LIGHT -> 25L
            HapticStrength.MEDIUM -> 55L
            HapticStrength.HEAVY -> 700L
        }

    private fun HapticStrength.amplitude(): Int =
        when (this) {
            HapticStrength.LIGHT -> 120
            HapticStrength.MEDIUM -> 190
            HapticStrength.HEAVY -> 255
        }
}

private fun Context.resolveVibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        // VibratorManager requires API 31; getSystemService(Vibrator) is the only option below it.
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
