package com.linklabs.sorcerersandspirits.domain.arena.model

// TODO: I think this could contain all the weird ability-related things, like Persuade

data class TurnInfo(
    val activePlayerUserId: String,
    val actedThisRoundUserIds: Set<String>,
    val roundNumber: Int,
)
