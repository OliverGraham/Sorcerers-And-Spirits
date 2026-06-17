package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet

@Immutable
data class GauntletProgress(
    val currentMatch: Int = 1,
    val matchesPerGauntlet: Int = Gauntlet.MATCHES_PER_GAUNTLET,
) {
    val isFinalMatch: Boolean = currentMatch >= matchesPerGauntlet
}
