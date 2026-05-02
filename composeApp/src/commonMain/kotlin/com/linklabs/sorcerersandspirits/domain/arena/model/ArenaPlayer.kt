package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

sealed interface ArenaPlayer {
    val userId: String
    val displayName: String

    data class Selected(
        override val userId: String,
        override val displayName: String,
        val characterClass: CharacterClass,
    ) : ArenaPlayer

    data class Unselected(
        override val userId: String,
        override val displayName: String,
    ) : ArenaPlayer
}


