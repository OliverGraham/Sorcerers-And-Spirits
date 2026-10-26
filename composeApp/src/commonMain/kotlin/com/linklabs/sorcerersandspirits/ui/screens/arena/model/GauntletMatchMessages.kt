package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class GauntletMatchMessages(
    val matchNumber: Int,
    val groups: List<ArenaMessageGroupState>,
)
