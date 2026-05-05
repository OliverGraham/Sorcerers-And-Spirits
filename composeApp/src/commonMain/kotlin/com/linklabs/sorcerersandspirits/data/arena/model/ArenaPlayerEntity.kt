package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaPlayerEntity {
    val userId: String
    val displayName: String

    @Serializable
    @SerialName(value = SELECTED)
    data class Selected(
        override val userId: String,
        override val displayName: String,
        val characterClassEntity: CharacterClassEntity,
    ) : ArenaPlayerEntity

    @Serializable
    @SerialName(value = UNSELECTED)
    data class Unselected(override val userId: String, override val displayName: String) :
        ArenaPlayerEntity

    companion object {
        private const val SELECTED = "Selected"
        private const val UNSELECTED = "Unselected"
    }
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
