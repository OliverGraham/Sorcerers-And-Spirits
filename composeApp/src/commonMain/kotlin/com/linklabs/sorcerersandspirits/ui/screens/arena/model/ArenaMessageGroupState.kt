package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage

@Immutable
sealed interface ArenaMessageGroupState {
    val messages: List<ArenaMessageState>

    @Immutable
    data class Setup(override val messages: List<ArenaMessageState>) : ArenaMessageGroupState

    @Immutable
    data class Round(val roundNumber: Int, override val messages: List<ArenaMessageState>) :
        ArenaMessageGroupState
}

// TODO: Use when NOT Combat
//        try to clean up - just so ugly and weird
internal fun List<ArenaMessageGroupState>.appendNewMessages(
    incomingMessages: List<IncomingArenaMessage>
): List<ArenaMessageGroupState> {
    var messageGroups = this

    val pendingMessages = mutableListOf<ArenaMessageState>()
    val newMessages = incomingMessages.toMessageStates()
    newMessages.forEach { message ->
        if (message is ArenaMessageState.NewRound) {
            messageGroups = messageGroups.appendToGroup(messages = pendingMessages)
            pendingMessages.clear()
            messageGroups = messageGroups.addNewGroup(newRoundMessage = message)
        } else {
            pendingMessages.add(element = message)
        }
    }

    return messageGroups.appendToGroup(messages = pendingMessages)
}

// TODO: Use when Combat ONLY
internal fun List<ArenaMessageGroupState>.addNewMessageDuringCombat(
    message: ArenaMessageState
): List<ArenaMessageGroupState> =
    if (message is ArenaMessageState.NewRound) {
        addNewGroup(newRoundMessage = message)
    } else {
        appendToGroup(message = message)
    }

private fun List<ArenaMessageGroupState>.addNewGroup(
    newRoundMessage: ArenaMessageState.NewRound
): List<ArenaMessageGroupState> {
    val newGroupMessageState =
        ArenaMessageGroupState.Round(
            roundNumber = newRoundMessage.roundNumber,
            messages = listOf(element = newRoundMessage),
        )

    return this + newGroupMessageState
}

private fun List<ArenaMessageGroupState>.appendToGroup(
    message: ArenaMessageState
): List<ArenaMessageGroupState> {
    if (isEmpty()) return this

    val mutableMessageGroups = toMutableList()
    val lastGroup = mutableMessageGroups.last()

    mutableMessageGroups[lastIndex] =
        when (lastGroup) {
            is ArenaMessageGroupState.Round ->
                lastGroup.copy(messages = lastGroup.messages + message)

            is ArenaMessageGroupState.Setup ->
                lastGroup.copy(messages = lastGroup.messages + message)
        }

    return mutableMessageGroups
}

// TODO: Look at this Setup part
private fun List<ArenaMessageGroupState>.appendToGroup(
    messages: List<ArenaMessageState>
): List<ArenaMessageGroupState> {
    if (messages.isEmpty()) return this
    if (isEmpty()) return this + ArenaMessageGroupState.Setup(messages = messages.toList())

    val mutableMessageGroups = toMutableList()
    val lastGroup = mutableMessageGroups.last()

    mutableMessageGroups[lastIndex] =
        when (lastGroup) {
            is ArenaMessageGroupState.Round ->
                lastGroup.copy(messages = lastGroup.messages + messages)

            is ArenaMessageGroupState.Setup ->
                lastGroup.copy(messages = lastGroup.messages + messages)
        }

    return mutableMessageGroups
}
