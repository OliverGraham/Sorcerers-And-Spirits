package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaPlayerEntity {
    val userId: String
    val displayName: String

    @Serializable
    data class Selected(
        override val userId: String,
        override val displayName: String,
        val characterClassEntity: CharacterClassEntity,
    ) : ArenaPlayerEntity

    @Serializable
    data class Unselected(override val userId: String, override val displayName: String) :
        ArenaPlayerEntity
}

internal fun ArenaPlayerEntity.toDomain() =
    when (this) {
        is ArenaPlayerEntity.Selected ->
            ArenaPlayer.Selected(
                userId = userId,
                displayName = displayName,
                characterClass = characterClassEntity.toDomain(),
            )

        is ArenaPlayerEntity.Unselected ->
            ArenaPlayer.Unselected(userId = userId, displayName = displayName)
    }

internal fun ArenaPlayerEntity.Selected.toDomain() =
    ArenaPlayer.Selected(
        userId = userId,
        displayName = displayName,
        characterClass = characterClassEntity.toDomain(),
    )

internal fun ArenaPlayer.toEntity() =
    when (this) {
        is ArenaPlayer.Selected ->
            ArenaPlayerEntity.Selected(
                userId = userId,
                displayName = displayName,
                characterClassEntity = characterClass.toEntity(),
            )

        is ArenaPlayer.Unselected ->
            ArenaPlayerEntity.Unselected(userId = userId, displayName = displayName)
    }

internal fun ArenaPlayer.Selected.toSelectedEntity() =
    ArenaPlayerEntity.Selected(
        userId = userId,
        displayName = displayName,
        characterClassEntity = characterClass.toEntity(),
    )
