package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
sealed interface ArenaMessageState {
    val id: String

    @Immutable
    data class NewRound(override val id: String, val roundNumber: String) : ArenaMessageState

    @Immutable
    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState

    @Immutable
    data class ConnectedToArena(override val id: String, val displayName: String) :
        ArenaMessageState

    @Immutable
    data class NewTurn(
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState

    @Immutable
    data class DiceRoll(
        override val id: String,
        val rollerClassName: String,
        val rollValue: Int,
        val rollType: RollTypeState,
    ) : ArenaMessageState

    @Immutable
    data class AttackResult(
        override val id: String,
        val attackerClassName: String,
        val targetClassName: String,
        val isHit: Boolean,
        val rollType: RollTypeState,
    ) : ArenaMessageState

    @Immutable
    data class TargetDead(
        override val id: String,
        val targetClassName: String,
        val killerClassName: String,
    ) : ArenaMessageState

    @Immutable
    data class GameFinished(override val id: String, val winnerClassName: String) :
        ArenaMessageState
}

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

fun IncomingArenaMessage.toState(): ArenaMessageState =
    when (this) {
        is IncomingArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

        is IncomingArenaMessage.ChoseCharacterClass ->
            ArenaMessageState.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toState(),
            )

        is IncomingArenaMessage.ConnectedToArena ->
            ArenaMessageState.ConnectedToArena(id = id, displayName = displayName)

        is IncomingArenaMessage.NewTurn ->
            ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

        is IncomingArenaMessage.DiceRoll ->
            ArenaMessageState.DiceRoll(
                id = id,
                rollerClassName = rollerClassName,
                rollValue = rollValue,
                rollType = rollType.toState(),
            )

        is IncomingArenaMessage.AttackResult ->
            ArenaMessageState.AttackResult(
                id = id,
                attackerClassName = attackerClassName,
                targetClassName = targetClassName,
                isHit = isHit,
                rollType = rollType.toState(),
            )

        is IncomingArenaMessage.TargetDead ->
            ArenaMessageState.TargetDead(
                id = id,
                targetClassName = targetClassName,
                killerClassName = killerClassName,
            )

        is IncomingArenaMessage.GameFinished ->
            ArenaMessageState.GameFinished(id = id, winnerClassName = winnerClassName)
    }

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
