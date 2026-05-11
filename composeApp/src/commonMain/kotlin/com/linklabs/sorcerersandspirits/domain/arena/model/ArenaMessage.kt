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

    data class DiceRoll(
        override val id: String,
        val rollerDisplayName: String,
        val rollValue: Int,
        val rollType: RollType,
    ) : ArenaMessage

    data class AttackResult(
        override val id: String,
        val attackerDisplayName: String,
        val targetDisplayName: String,
        val isHit: Boolean,
        val rollType: RollType,
    ) : ArenaMessage

    data class TargetDead(
        override val id: String,
        val targetDisplayName: String,
        val killerDisplayName: String,
    ) : ArenaMessage

    data class GameFinished(override val id: String, val winnerDisplayName: String) : ArenaMessage
}

data class CharacterClassMessageInfo(val className: String, val type: CharacterClassType)

// TODO Arena: Hmmm
enum class RollType {
    Ability,
    ArmorClass,
    Health,
}
