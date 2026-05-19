package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState

@Immutable
data class ArenaScreenState(
    val messages: List<ArenaMessageState>,
    val unhandledMessagesCount: Int,
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val arenaState: ArenaState,
) {
    val latestMessage: ArenaMessageState? =
        messages.getOrNull(
            index = messages.size - unhandledMessagesCount.coerceAtLeast(minimumValue = 1)
        )

    val filteredMessages: List<ArenaMessageState> =
        messages.dropLast(n = unhandledMessagesCount.coerceAtLeast(minimumValue = 1))

    val characterClasses = CharacterClass.getAllClasses().map { it.toState() }
}

@Immutable
sealed interface ArenaState {

    @Immutable data object WaitingForAllPlayers : ArenaState

    @Immutable
    data class ChoosingClasses(
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
        override val activePlayer: PlayerState.Selected,
        override val userPlayer: PlayerState.Selected,
        override val otherPlayers: List<PlayerState.Selected>,
        override val isItThisUsersTurn: Boolean,
        val pendingRollType: RollType,
        val pendingTargetUserId: String?,
    ) : ActiveState

    @Immutable data object Finished : ArenaState
}

@Immutable
sealed interface ActiveState : ArenaState {
    val activePlayer: PlayerState
    val userPlayer: PlayerState
    val otherPlayers: List<PlayerState>
    val isItThisUsersTurn: Boolean
}

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaScreenState?,
    userId: String,
): ArenaStateUpdate {
    val messages = messages.map { message -> message.toState() }
    val unhandledMessagesCount = (previousState?.unhandledMessagesCount ?: 0) + numberOfNewMessages
    val previousSeconds = previousState?.secondsRemaining ?: 0
    val previousShowForfeitDialog = previousState?.showForfeitDialog ?: false

    val nextArenaState =
        when (this) {
            is ArenaMatch.WaitingForAllPlayers -> ArenaState.WaitingForAllPlayers

            is ArenaMatch.ChoosingClasses -> {
                val userPlayer = playerMap[userId]
                if (userPlayer == null) {
                    ArenaState.Finished
                } else {
                    val (unavailable, selections) =
                        playerMap.prepareClassSelectionInfo(userId = userId)
                    val activePlayerState = activePlayer.toState()
                    val userPlayerState = userPlayer.toState()
                    val otherPlayersState = playerMap.getOtherPlayers(userId = userId)

                    ArenaState.ChoosingClasses(
                        activePlayer = activePlayerState,
                        userPlayer = userPlayerState,
                        otherPlayers = otherPlayersState,
                        isItThisUsersTurn = activePlayerUserId == userId,
                        unavailableClasses = unavailable,
                        classSelections = selections,
                        selectedClass =
                            (previousState?.arenaState as? ArenaState.ChoosingClasses)
                                ?.selectedClass,
                    )
                }
            }

            is ArenaMatch.Combat -> {
                val userPlayer = playerMap[userId]
                if (userPlayer == null) {
                    ArenaState.Finished
                } else {
                    val activePlayerState = activePlayer.toSelectedState()
                    val userPlayerState = userPlayer.toSelectedState()
                    val otherPlayersState =
                        playerMap.values
                            .filter { player -> player.userId != userId }
                            .map { player -> player.toSelectedState() }

                    ArenaState.Combat(
                        activePlayer = activePlayerState,
                        userPlayer = userPlayerState,
                        otherPlayers = otherPlayersState,
                        isItThisUsersTurn = activePlayerUserId == userId,
                        pendingRollType = pendingRollType,
                        pendingTargetUserId = pendingTargetUserId,
                    )
                }
            }

            is ArenaMatch.Finished -> ArenaState.Finished
        }

    val nextScreenState =
        ArenaScreenState(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = previousSeconds,
            showForfeitDialog = previousShowForfeitDialog,
            arenaState = nextArenaState,
        )

    val timerAction =
        determineTimerAction(previous = previousState?.arenaState, next = nextArenaState)
    val finalState =
        when (timerAction) {
            is TimerAction.Start ->
                nextScreenState.copy(secondsRemaining = timerAction.timerDuration)

            TimerAction.Stop -> nextScreenState.copy(secondsRemaining = 0)

            TimerAction.None -> nextScreenState
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
        is ArenaState.WaitingForAllPlayers ->
            if (previous == null) {
                TimerAction.Start(timerDuration = 20)
            } else {
                TimerAction.None
            }

        is ArenaState.ChoosingClasses ->
            if (previous !is ArenaState.ChoosingClasses) {
                TimerAction.Start(timerDuration = 15)
            } else {
                TimerAction.None
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

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
