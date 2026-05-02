package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch

@Immutable
sealed interface ArenaState {
    val messages: List<ArenaMessageState>
    val unhandledMessagesCount: Int
    val secondsRemaining: Int

    val latestMessage: ArenaMessageState?
        get() =
            messages.getOrNull(
                index = messages.size - unhandledMessagesCount.coerceAtLeast(minimumValue = 1)
            )

    val filteredMessages: List<ArenaMessageState>
        get() = messages.dropLast(n = unhandledMessagesCount.coerceAtLeast(minimumValue = 1))

    @Immutable
    data class WaitingForAllPlayers(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
        override val secondsRemaining: Int,
    ) : ArenaState

    @Immutable
    data class ChoosingClasses(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
        override val secondsRemaining: Int,
        override val activePlayer: PlayerState,
        override val userPlayer: PlayerState,
        override val otherPlayers: List<PlayerState>,
        override val isItThisUsersTurn: Boolean,
    ) : ActiveState

    data class Combat(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
        override val secondsRemaining: Int,
        override val activePlayer: PlayerState.Selected,
        override val userPlayer: PlayerState.Selected,
        override val otherPlayers: List<PlayerState.Selected>,
        override val isItThisUsersTurn: Boolean,
    ) : ActiveState

    @Immutable
    data class Finished(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
        override val secondsRemaining: Int = 0,
    ) : ArenaState
}

@Immutable
sealed interface ActiveState : ArenaState {
    val activePlayer: PlayerState
    val userPlayer: PlayerState
    val otherPlayers: List<PlayerState>
    val isItThisUsersTurn: Boolean
}

fun ArenaState.withSeconds(seconds: Int): ArenaState =
    when (this) {
        is ArenaState.WaitingForAllPlayers -> copy(secondsRemaining = seconds)
        is ArenaState.ChoosingClasses -> copy(secondsRemaining = seconds)
        is ArenaState.Combat -> copy(secondsRemaining = seconds)
        is ArenaState.Finished -> copy(secondsRemaining = seconds)
    }

internal fun ArenaMatch.toState(
    previousUnhandledMessagesCount: Int,
    userId: String?,
    secondsRemaining: Int?,
): ArenaState {
    val messagesState = messages.map { it.toState() }
    val unhandledMessagesCount = previousUnhandledMessagesCount + numberOfNewMessages
    val seconds = secondsRemaining ?: 0

    return when (this) {
        is ArenaMatch.WaitingForAllPlayers -> {
            ArenaState.WaitingForAllPlayers(
                messages = messagesState,
                unhandledMessagesCount = unhandledMessagesCount,
                secondsRemaining = seconds,
            )
        }

        is ArenaMatch.ChoosingClasses -> {
            val activePlayer = playerMap[activePlayerUserId]
            val userPlayer = playerMap[userId]

            if (activePlayer == null || userPlayer == null) {
                ArenaState.Finished(
                    messages = messagesState,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = seconds,
                )
            } else {
                ArenaState.ChoosingClasses(
                    messages = messagesState,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = seconds,
                    activePlayer = activePlayer.toState(),
                    userPlayer = userPlayer.toState(),
                    otherPlayers =
                        playerMap.values.filter { it.userId != userId }.map { it.toState() },
                    isItThisUsersTurn = activePlayerUserId == userId,
                )
            }
        }

        is ArenaMatch.Combat -> {
            val activePlayer = playerMap[activePlayerUserId]
            val userPlayer = playerMap[userId]

            if (activePlayer == null || userPlayer == null) {
                ArenaState.Finished(
                    messages = messagesState,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = seconds,
                )
            } else {
                ArenaState.Combat(
                    messages = messagesState,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = seconds,
                    activePlayer = activePlayer.toSelectedState(),
                    userPlayer = userPlayer.toSelectedState(),
                    otherPlayers =
                        playerMap.values
                            .filter { it.userId != userId }
                            .map { it.toSelectedState() },
                    isItThisUsersTurn = activePlayerUserId == userId,
                )
            }
        }

        is ArenaMatch.Finished -> {
            ArenaState.Finished(
                messages = messagesState,
                unhandledMessagesCount = unhandledMessagesCount,
                secondsRemaining = seconds,
            )
        }
    }
}
