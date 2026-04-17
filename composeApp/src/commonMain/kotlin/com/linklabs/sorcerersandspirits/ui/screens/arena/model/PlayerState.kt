package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

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
