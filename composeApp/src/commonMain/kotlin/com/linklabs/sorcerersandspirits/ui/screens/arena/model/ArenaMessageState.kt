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
    }

private fun CharacterClassMessageInfoState.toDomain(): CharacterClassMessageInfo =
    CharacterClassMessageInfo(className = className, type = type)

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
