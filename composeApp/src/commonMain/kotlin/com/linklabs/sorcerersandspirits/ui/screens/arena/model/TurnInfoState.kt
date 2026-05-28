package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class TurnInfoState(
    val activePlayerUserId: String,
    val actedThisRoundUserIds: Set<String>,
    val roundNumber: Int,
)

internal fun TurnInfo.toState() =
    TurnInfoState(
        activePlayerUserId = activePlayerUserId,
        actedThisRoundUserIds = actedThisRoundUserIds,
        roundNumber = roundNumber,
    )

internal fun TurnInfoState.toDomain() =
    TurnInfo(
        activePlayerUserId = activePlayerUserId,
        actedThisRoundUserIds = actedThisRoundUserIds,
        roundNumber = roundNumber,
    )
