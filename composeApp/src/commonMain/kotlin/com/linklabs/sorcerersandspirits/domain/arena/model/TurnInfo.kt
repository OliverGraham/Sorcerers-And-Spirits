package com.linklabs.sorcerersandspirits.domain.arena.model

data class TurnInfo(
    val activePlayerUserId: String,
    val actedThisRoundUserIds: Set<String>,
    val roundNumber: Int,
)
