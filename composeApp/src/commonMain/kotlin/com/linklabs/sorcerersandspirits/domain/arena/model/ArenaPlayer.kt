package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

sealed interface ArenaPlayer {
    val userId: String
    val displayName: String
    val isReadyForNextMatch: Boolean

    data class Selected(
        override val userId: String,
        override val displayName: String,
        override val isReadyForNextMatch: Boolean = false,
        val characterClass: CharacterClass,
    ) : ArenaPlayer

    data class Unselected(
        override val userId: String,
        override val displayName: String,
        override val isReadyForNextMatch: Boolean = false,
    ) : ArenaPlayer
}
