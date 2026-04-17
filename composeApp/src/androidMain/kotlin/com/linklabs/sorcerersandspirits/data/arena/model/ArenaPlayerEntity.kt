package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import kotlinx.serialization.Serializable

@Serializable
data class ArenaPlayerEntity(
    val userId: String = "",
    val displayName: String = "",
    val characterClassEntity: CharacterClassEntity? = null,
)

internal fun ArenaPlayerEntity.toDomain() =
    ArenaPlayer(
        userId = userId,
        displayName = displayName,
        characterClass = characterClassEntity?.toDomain(),
    )
