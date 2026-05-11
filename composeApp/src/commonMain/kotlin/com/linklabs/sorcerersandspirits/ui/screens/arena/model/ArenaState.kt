package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState

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
        val selectedClass: CharacterClassState?,
        val unavailableClasses: List<CharacterClassState>,
        val classSelections: Map<CharacterClassState, String>,
    ) : ActiveState

    @Immutable
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

fun ArenaState.ChoosingClasses.withSelectedClass(
    selectedClass: CharacterClassState?
): ArenaState.ChoosingClasses = copy(selectedClass = selectedClass)

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaState?,
    userId: String,
): ArenaStateUpdate {
    val messages = messages.map { message -> message.toState() }
    val unhandledMessagesCount = (previousState?.unhandledMessagesCount ?: 0) + numberOfNewMessages
    val previousSeconds = previousState?.secondsRemaining ?: 0

    val nextState =
        when (this) {
            is ArenaMatch.WaitingForAllPlayers ->
                ArenaState.WaitingForAllPlayers(
                    messages = messages,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = previousSeconds,
                )

            is ArenaMatch.ChoosingClasses -> {
                val userPlayer = playerMap[userId]
                if (userPlayer == null) {
                    ArenaState.Finished(
                        messages = messages,
                        unhandledMessagesCount = unhandledMessagesCount,
                        secondsRemaining = 0,
                    )
                } else {
                    val (unavailable, selections) =
                        playerMap.prepareClassSelectionInfo(userId = userId)
                    ArenaState.ChoosingClasses(
                        messages = messages,
                        unhandledMessagesCount = unhandledMessagesCount,
                        secondsRemaining = previousSeconds,
                        activePlayer = activePlayer.toState(),
                        userPlayer = userPlayer.toState(),
                        otherPlayers = playerMap.getOtherPlayers(userId = userId),
                        isItThisUsersTurn = activePlayerUserId == userId,
                        unavailableClasses = unavailable,
                        classSelections = selections,
                        selectedClass =
                            (previousState as? ArenaState.ChoosingClasses)?.selectedClass,
                    )
                }
            }

            is ArenaMatch.Combat -> {
                val userPlayer = playerMap[userId]
                if (userPlayer == null) {
                    ArenaState.Finished(
                        messages = messages,
                        unhandledMessagesCount = unhandledMessagesCount,
                        secondsRemaining = 0,
                    )
                } else {
                    ArenaState.Combat(
                        messages = messages,
                        unhandledMessagesCount = unhandledMessagesCount,
                        secondsRemaining = previousSeconds,
                        activePlayer = activePlayer.toSelectedState(),
                        userPlayer = userPlayer.toSelectedState(),
                        otherPlayers =
                            playerMap.values
                                .filter { player -> player.userId != userId }
                                .map { player -> player.toSelectedState() },
                        isItThisUsersTurn = activePlayerUserId == userId,
                    )
                }
            }

            is ArenaMatch.Finished ->
                ArenaState.Finished(
                    messages = messages,
                    unhandledMessagesCount = unhandledMessagesCount,
                    secondsRemaining = 0,
                )
        }

    val timerAction = determineTimerAction(previous = previousState, next = nextState)
    val finalState =
        when (timerAction) {
            is TimerAction.Start -> nextState.withSeconds(seconds = timerAction.timerDuration)
            TimerAction.Stop -> nextState.withSeconds(seconds = 0)
            TimerAction.None -> nextState
        }

    return ArenaStateUpdate(nextState = finalState, timerAction = timerAction)
}

private fun Map<String, ArenaPlayer>.prepareClassSelectionInfo(
    userId: String
): Pair<List<CharacterClassState>, Map<CharacterClassState, String>> {
    val allPlayers = values.map { player -> player.toState() }
    val unavailableClasses =
        allPlayers
            .filter { player -> player.userId != userId }
            .mapNotNull { player -> (player as? PlayerState.Selected)?.characterClass }

    val classSelections =
        allPlayers.filterIsInstance<PlayerState.Selected>().associate { player ->
            player.characterClass to player.displayName
        }

    return unavailableClasses to classSelections
}

private fun Map<String, ArenaPlayer>.getOtherPlayers(userId: String): List<PlayerState> =
    values.filter { player -> player.userId != userId }.map { player -> player.toState() }

private fun determineTimerAction(previous: ArenaState?, next: ArenaState): TimerAction =
    when (next) {
        is ArenaState.WaitingForAllPlayers -> {
            if (previous == null) {
                TimerAction.Start(timerDuration = 20)
            } else {
                TimerAction.None
            }
        }

        is ArenaState.ChoosingClasses -> {
            if (previous !is ArenaState.ChoosingClasses) {
                TimerAction.Start(timerDuration = 15)
            } else {
                TimerAction.None
            }
        }

        is ArenaState.Combat -> {
            val turnChanged =
                previous is ActiveState && previous.activePlayer.userId != next.activePlayer.userId
            if (previous !is ArenaState.Combat || turnChanged) {
                TimerAction.Start(timerDuration = 20)
            } else {
                TimerAction.None
            }
        }

        is ArenaState.Finished -> TimerAction.Stop
    }

data class ArenaStateUpdate(val nextState: ArenaState, val timerAction: TimerAction)
