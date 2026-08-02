package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ArenaMessageGroupState {
    val messages: List<ArenaMessageState>

    @Immutable
    data class Setup(override val messages: List<ArenaMessageState>) : ArenaMessageGroupState

    @Immutable
    data class Round(val roundNumber: Int, override val messages: List<ArenaMessageState>) :
        ArenaMessageGroupState

    fun withMessages(messages: List<ArenaMessageState>): ArenaMessageGroupState =
        when (this) {
            is Setup -> Setup(messages = messages)
            is Round -> Round(roundNumber = roundNumber, messages = messages)
        }
}

fun List<ArenaMessageGroupState>.plusMessage(
    message: ArenaMessageState
): List<ArenaMessageGroupState> {
    if (message is ArenaMessageState.NewRound) {
        val roundNumber = message.roundNumber.toIntOrNull()
        val newGroup =
            if (roundNumber == null) {
                ArenaMessageGroupState.Setup(messages = listOf(message))
            } else {
                ArenaMessageGroupState.Round(roundNumber = roundNumber, messages = listOf(message))
            }
        return this + newGroup
    }

    val lastGroup =
        lastOrNull() ?: return listOf(ArenaMessageGroupState.Setup(messages = listOf(message)))
    return dropLast(n = 1) + lastGroup.withMessages(messages = lastGroup.messages + message)
}

fun List<ArenaMessageState>.toMessageGroups(): List<ArenaMessageGroupState> =
    fold(initial = emptyList()) { groups, message -> groups.plusMessage(message = message) }
