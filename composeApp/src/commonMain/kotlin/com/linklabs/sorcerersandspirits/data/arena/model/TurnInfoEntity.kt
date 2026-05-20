package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import kotlinx.serialization.Serializable

@Serializable
data class TurnInfoEntity(
    val activePlayerUserId: String,
    val actedThisRoundUserIds: Set<String>,
    val roundNumber: Int,
)

internal fun TurnInfo.toEntity() =
    TurnInfoEntity(
        activePlayerUserId = activePlayerUserId,
        actedThisRoundUserIds = actedThisRoundUserIds,
        roundNumber = roundNumber,
    )

internal fun TurnInfoEntity.toDomain() =
    TurnInfo(
        activePlayerUserId = activePlayerUserId,
        actedThisRoundUserIds = actedThisRoundUserIds,
        roundNumber = roundNumber,
    )
