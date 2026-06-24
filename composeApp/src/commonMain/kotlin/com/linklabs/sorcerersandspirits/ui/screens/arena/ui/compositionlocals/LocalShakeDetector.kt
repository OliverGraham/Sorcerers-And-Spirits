package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals

import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Supplies the [ShakeDetector] to the shake-to-roll gesture (SS-069). The default never emits, so
 * previews and tests render without a Koin context;
 * [com.linklabs.sorcerersandspirits.ui.screens.arena.ui.ArenaScreen] provides the real injected
 * detector for the running app.
 */
internal val LocalShakeDetector =
    staticCompositionLocalOf<ShakeDetector> {
        object : ShakeDetector {
            override val shakes: Flow<Unit> = emptyFlow()
        }
    }
