package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState

@Immutable
data class PlayerState(
    val userId: String,
    val displayName: String,
    val characterClassState: CharacterClassState?,
)

internal fun ArenaPlayer.toState() =
    PlayerState(
        userId = userId,
        displayName = displayName,
        characterClassState = characterClass?.toState(),
    )

internal fun PlayerState.toDomain() =
    ArenaPlayer(
        userId = userId,
        displayName = displayName,
        characterClass = characterClassState?.toDomain(),
    )
