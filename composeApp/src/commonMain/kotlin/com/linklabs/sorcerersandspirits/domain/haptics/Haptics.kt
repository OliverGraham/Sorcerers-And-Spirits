package com.linklabs.sorcerersandspirits.domain.haptics

/** Plays device vibration/haptic feedback. */
interface Haptics {
    /** A single one-shot buzz. */
    fun vibrate(strength: HapticStrength)

    /**
     * Starts one continuous, smooth buzz (no pulsing). Idempotent while already running, so it can
     * be kept alive by repeated calls. Stopped with [stopContinuous].
     */
    fun startContinuous(strength: HapticStrength)

    /**
     * Stops a buzz started by [startContinuous]. No-op if a [vibrate] one-shot has since taken
     * over.
     */
    fun stopContinuous()
}

enum class HapticStrength {
    LIGHT,
    MEDIUM,
    HEAVY,
}
