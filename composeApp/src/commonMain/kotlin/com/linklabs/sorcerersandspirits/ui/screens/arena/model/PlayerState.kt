package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState

@Immutable
sealed interface PlayerState {
    val userId: String
    val displayName: String

    @Immutable
    data class Selected(
        override val userId: String,
        override val displayName: String,
        val characterClass: CharacterClassState,
    ) : PlayerState

    @Immutable
    data class Unselected(override val userId: String, override val displayName: String) : PlayerState
}

internal fun Map<String, ArenaPlayer>.toState() = mapValues { it.value.toState() }

internal fun ArenaPlayer.toState() = when (this) {
    is ArenaPlayer.Selected ->
        PlayerState.Selected(
            userId = userId,
            displayName = displayName,
            characterClass = characterClass.toState(),
        )

    is ArenaPlayer.Unselected ->
        PlayerState.Unselected(userId = userId, displayName = displayName)
}

internal fun ArenaPlayer.Selected.toSelectedState() = PlayerState.Selected(
    userId = userId,
    displayName = displayName,
    characterClass = characterClass.toState(),
)

internal fun PlayerState.toDomain() = when (this) {
    is PlayerState.Selected ->
        ArenaPlayer.Selected(
            userId = userId,
            displayName = displayName,
            characterClass = characterClass.toDomain(),
        )

    is PlayerState.Unselected ->
        ArenaPlayer.Unselected(userId = userId, displayName = displayName)
}

internal fun PlayerState.Selected.toDomain() = ArenaPlayer.Selected(
    userId = userId,
    displayName = displayName,
    characterClass = characterClass.toDomain(),
)
