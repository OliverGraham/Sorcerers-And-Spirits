package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

sealed interface ArenaMessage {
    val id: String

    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : ArenaMessage

    data class ConnectedToArena(override val id: String, val displayName: String) : ArenaMessage

    data class NewRound(override val id: String, val roundNumber: Int) : ArenaMessage

    data class NewTurn(override val id: String, val characterClassInfo: CharacterClassMessageInfo) :
        ArenaMessage
}

data class CharacterClassMessageInfo(val className: String, val type: CharacterClassType)

// TODO Arena: Hmmm
enum class RollType {
    Ability,
    ArmorClass,
    Health,
}
