package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

data class ArenaPlayer(
    val userId: String,
    val displayName: String,
    val characterClass: CharacterClass?,
)
