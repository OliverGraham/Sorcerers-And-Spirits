package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

data class MatchParticipant(
    val userId: String,
    val displayName: String,
    val classType: CharacterClassType,
    val status: ParticipantStatus,
)
