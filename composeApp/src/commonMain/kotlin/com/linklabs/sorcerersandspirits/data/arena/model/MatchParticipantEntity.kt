package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.arena.model.isKnockedOut
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.Serializable

@Serializable
data class MatchParticipantEntity(
    val userId: String,
    val displayName: String,
    val classType: CharacterClassType,
    val knockoutOrder: Int?,
)

internal fun MatchParticipantEntity.toDomain(): MatchParticipant = MatchParticipant(
    userId = userId,
    displayName = displayName,
    classType = classType,
    knockoutOrder = knockoutOrder,
)

internal fun Map<String, MatchParticipantEntity>.toDomain(): Map<String, MatchParticipant> = mapValues { it.value.toDomain() }

internal fun Map<String, ArenaPlayerEntity.Selected>.toInitialParticipantMap(): Map<String, MatchParticipantEntity> = mapValues { (_, player) ->
    MatchParticipantEntity(
        userId = player.userId,
        displayName = player.displayName,
        classType = player.characterClassEntity.type,
        knockoutOrder = null,
    )
}

internal fun Map<String, MatchParticipantEntity>.withUpdatedKnockoutOrder(
    updatedPlayerMap: Map<String, ArenaPlayer.Selected>,
): Map<String, MatchParticipantEntity> {
    val nextKnockoutOrder = values.count { it.knockoutOrder != null } + 1

    return mapValues { (userId, participant) ->
        val hasKnockoutOrder = participant.knockoutOrder != null
        val player = updatedPlayerMap[userId]
        val isNowKnockedOut = player != null && player.isKnockedOut()
        if (!hasKnockoutOrder && isNowKnockedOut) {
            participant.copy(knockoutOrder = nextKnockoutOrder)
        } else {
            participant
        }
    }
}
