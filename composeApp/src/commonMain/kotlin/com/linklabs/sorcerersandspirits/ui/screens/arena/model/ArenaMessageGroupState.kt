package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage

@Immutable
sealed interface ArenaMessageGroupState {
    val messages: List<ArenaMessageState>

    // TODO: Is Setup needed?
    @Immutable
    data class Setup(override val messages: List<ArenaMessageState>) : ArenaMessageGroupState

    @Immutable
    data class Round(val roundNumber: Int, override val messages: List<ArenaMessageState>) :
        ArenaMessageGroupState
}

// TODO: Have two sets of functions - one for appending a list all at once, and another
//       for appending a single message to a group (to be used for Combat)
internal fun appendNewMessages(
    incomingMessages: List<IncomingArenaMessage>,
    currentMessageGroups: List<ArenaMessageGroupState>,
): List<ArenaMessageGroupState> {
    val messageGroups = currentMessageGroups.ifEmpty {
        ArenaMessageGroupState.Round(
            roundNumber = 1,
            messages = emptyList()
        )
    }

    val incomingMessageStates = incomingMessages.toMessageStates()

    return currentMessageGroups
}



private fun List<ArenaMessageGroupState>.addNewGroup(
    newRoundMessage: ArenaMessageState.NewRound
): List<ArenaMessageGroupState> {
    val newGroupMessageState =
        ArenaMessageGroupState.Round(
            roundNumber = newRoundMessage.roundNumber,
            messages = listOf(element = newRoundMessage)
        )

    return this + newGroupMessageState
}

private fun List<ArenaMessageGroupState>.appendToGroup(
    message: ArenaMessageState
): List<ArenaMessageGroupState> {
    if (isEmpty()) return this

    val mutableMessageGroups = toMutableList()
    val lastGroup = mutableMessageGroups.last()

    mutableMessageGroups[lastIndex] = when (lastGroup) {
        is ArenaMessageGroupState.Round -> lastGroup.copy(messages = lastGroup.messages + message)
        is ArenaMessageGroupState.Setup -> lastGroup.copy(messages = lastGroup.messages + message)
    }

    return mutableMessageGroups
}

private fun List<ArenaMessageGroupState>.appendToGroup(
    messages: List<ArenaMessageState>
): List<ArenaMessageGroupState> {
    if (isEmpty()) return this

    val mutableMessageGroups = toMutableList()
    val lastGroup = mutableMessageGroups.last()

    mutableMessageGroups[lastIndex] = when (lastGroup) {
        is ArenaMessageGroupState.Round -> lastGroup.copy(messages = lastGroup.messages + messages)
        is ArenaMessageGroupState.Setup -> lastGroup.copy(messages = lastGroup.messages + messages)
    }

    return mutableMessageGroups
}
