package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
data class MatchParticipantState(
    val userId: String,
    val displayName: String,
    val classType: CharacterClassType,
    val status: ParticipantStatusState,
)

internal fun MatchParticipant.toState() = MatchParticipantState(
    userId = userId,
    displayName = displayName,
    classType = classType,
    status = status.toState(),
)

internal fun MatchParticipantState.toDomain() = MatchParticipant(
    userId = userId,
    displayName = displayName,
    classType = classType,
    status = status.toDomain(),
)
