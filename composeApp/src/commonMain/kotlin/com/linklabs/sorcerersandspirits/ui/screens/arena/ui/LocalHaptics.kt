package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics

/**
 * Supplies device [Haptics] to the shake-to-roll gesture (SS-069). The default does nothing, so
 * previews and tests render without a Koin context; [ArenaScreen] provides the real injected
 * vibrator for the running app.
 */
internal val LocalHaptics =
    staticCompositionLocalOf<Haptics> {
        object : Haptics {
            override fun vibrate(strength: HapticStrength) = Unit

            override fun startContinuous(strength: HapticStrength) = Unit

            override fun stopContinuous() = Unit
        }
    }
