package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
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
        val rollerDisplayName: String,
        val rollValue: Int,
        val rollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType,
    ) : ArenaMessageState

    @Immutable
    data class AttackResult(
        override val id: String,
        val attackerDisplayName: String,
        val targetDisplayName: String,
        val isHit: Boolean,
        val rollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType,
    ) : ArenaMessageState

    @Immutable
    data class TargetDead(
        override val id: String,
        val targetDisplayName: String,
        val killerDisplayName: String,
    ) : ArenaMessageState

    @Immutable
    data class GameFinished(override val id: String, val winnerDisplayName: String) :
        ArenaMessageState
}

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

fun ArenaMessageState.toDomain(): ArenaMessage =
    when (this) {
        is ArenaMessageState.NewRound ->
            ArenaMessage.NewRound(id = id, roundNumber = roundNumber.toIntOrNull() ?: 0)

        is ArenaMessageState.ChoseCharacterClass ->
            ArenaMessage.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toDomain(),
            )

        is ArenaMessageState.ConnectedToArena ->
            ArenaMessage.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessageState.NewTurn ->
            ArenaMessage.NewTurn(id = id, characterClassInfo = characterClassInfo.toDomain())

        is ArenaMessageState.DiceRoll ->
            ArenaMessage.DiceRoll(
                id = id,
                rollerDisplayName = rollerDisplayName,
                rollValue = rollValue,
                rollType = rollType,
            )

        is ArenaMessageState.AttackResult ->
            ArenaMessage.AttackResult(
                id = id,
                attackerDisplayName = attackerDisplayName,
                targetDisplayName = targetDisplayName,
                isHit = isHit,
                rollType = rollType,
            )

        is ArenaMessageState.TargetDead ->
            ArenaMessage.TargetDead(
                id = id,
                targetDisplayName = targetDisplayName,
                killerDisplayName = killerDisplayName,
            )

        is ArenaMessageState.GameFinished ->
            ArenaMessage.GameFinished(id = id, winnerDisplayName = winnerDisplayName)
    }

fun ArenaMessage.toState(): ArenaMessageState =
    when (this) {
        is ArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

        is ArenaMessage.ChoseCharacterClass ->
            ArenaMessageState.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toState(),
            )

        is ArenaMessage.ConnectedToArena ->
            ArenaMessageState.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessage.NewTurn ->
            ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

        is ArenaMessage.DiceRoll ->
            ArenaMessageState.DiceRoll(
                id = id,
                rollerDisplayName = rollerDisplayName,
                rollValue = rollValue,
                rollType = rollType,
            )

        is ArenaMessage.AttackResult ->
            ArenaMessageState.AttackResult(
                id = id,
                attackerDisplayName = attackerDisplayName,
                targetDisplayName = targetDisplayName,
                isHit = isHit,
                rollType = rollType,
            )

        is ArenaMessage.TargetDead ->
            ArenaMessageState.TargetDead(
                id = id,
                targetDisplayName = targetDisplayName,
                killerDisplayName = killerDisplayName,
            )

        is ArenaMessage.GameFinished ->
            ArenaMessageState.GameFinished(id = id, winnerDisplayName = winnerDisplayName)
    }

private fun CharacterClassMessageInfoState.toDomain(): CharacterClassMessageInfo =
    CharacterClassMessageInfo(className = className, type = type)

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
