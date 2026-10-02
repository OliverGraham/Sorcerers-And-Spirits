package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.Serializable

@Serializable
data class MatchParticipantEntity(
    val userId: String,
    val displayName: String,
    val classType: CharacterClassType,
    val statusEntity: ParticipantStatusEntity,
)

internal fun MatchParticipantEntity.toDomain(): MatchParticipant = MatchParticipant(
    userId = userId,
    displayName = displayName,
    classType = classType,
    status = statusEntity.toDomain(),
)

internal fun Map<String, MatchParticipantEntity>.toDomain(): Map<String, MatchParticipant> = mapValues { it.value.toDomain() }

internal fun MatchParticipant.toEntity(): MatchParticipantEntity = MatchParticipantEntity(
    userId = userId,
    displayName = displayName,
    classType = classType,
    statusEntity = status.toEntity(),
)

internal fun Map<String, MatchParticipant>.toParticipantEntitiesMap(): Map<String, MatchParticipantEntity> = mapValues { it.value.toEntity() }

internal fun Map<String, ArenaPlayerEntity.Selected>.toInitialParticipantMap(): Map<String, MatchParticipantEntity> = mapValues { (_, player) ->
    MatchParticipantEntity(
        userId = player.userId,
        displayName = player.displayName,
        classType = player.characterClassEntity.type,
        statusEntity = ParticipantStatusEntity.Conscious,
    )
}
