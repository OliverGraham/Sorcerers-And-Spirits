package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class CombatMoment(
    val seating: CombatSeating,
    val activeUserIds: Set<String>,
    val turnOwnerUserId: String,
)
