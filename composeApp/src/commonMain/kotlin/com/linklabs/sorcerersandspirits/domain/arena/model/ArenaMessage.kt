package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

sealed interface IncomingArenaMessage {
    val id: String

    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : IncomingArenaMessage

    data class ConnectedToArena(override val id: String, val displayName: String) :
        IncomingArenaMessage

    data class NewRound(override val id: String, val roundNumber: Int) : IncomingArenaMessage

    data class NewTurn(override val id: String, val characterClassInfo: CharacterClassMessageInfo) :
        IncomingArenaMessage

    data class DiceRoll(
        override val id: String,
        val rollerClassName: String,
        val rollValue: Int,
        val rollType: RollType,
    ) : IncomingArenaMessage

    data class AttackResult(
        override val id: String,
        val attackerClassName: String,
        val targetClassName: String,
        val isHit: Boolean,
        val rollType: RollType,
    ) : IncomingArenaMessage

    data class TargetDead(
        override val id: String,
        val targetClassName: String,
        val killerClassName: String,
    ) : IncomingArenaMessage

    data class GameFinished(override val id: String, val winnerClassName: String) :
        IncomingArenaMessage
}

sealed interface OutgoingArenaMessage {
    data class ChoseCharacterClass(
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : OutgoingArenaMessage

    data class ConnectedToArena(val displayName: String) : OutgoingArenaMessage

    data class NewRound(val roundNumber: Int) : OutgoingArenaMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : OutgoingArenaMessage

    data class DiceRoll(val rollerClassName: String, val rollValue: Int, val rollType: RollType) :
        OutgoingArenaMessage

    data class AttackResult(
        val attackerClassName: String,
        val targetClassName: String,
        val isHit: Boolean,
        val rollType: RollType,
    ) : OutgoingArenaMessage

    data class TargetDead(val targetClassName: String, val killerClassName: String) :
        OutgoingArenaMessage

    data class GameFinished(val winnerClassName: String) : OutgoingArenaMessage
}

data class CharacterClassMessageInfo(val className: String, val type: CharacterClassType)
