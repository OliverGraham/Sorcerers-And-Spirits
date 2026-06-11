package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
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
        override val playerMap: Map<UserId, PlayerState>,
        override val userPlayerId: String,
        val activePlayerUserId: String,
        val selectedClass: CharacterClassState?,
        val unavailableClasses: List<CharacterClassState>,
        val classSelections: Map<CharacterClassState, String>,
    ) : ActiveState {
        val otherPlayers = playerMap.filterKeys { it != activePlayerUserId }
        val isItThisUsersTurn = activePlayerUserId == userPlayerId
    }

    @Immutable
    data class Combat(
        override val playerMap: Map<UserId, PlayerState.Selected>,
        override val userPlayerId: String,
        val combatActionState: CombatActionState,
        val roundNumber: Int,
        val isNewRound: Boolean,
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val otherPlayers = playerMap.filterKeys { it !in activeUserIds }
        val isItThisUsersTurn = userPlayerId in activeUserIds
    }

    @Immutable data object UserPlayerKnockedOut : ArenaState

    @Immutable data object Finished : ArenaState
}

@Immutable
sealed interface ActiveState : ArenaState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaScreenState?,
    userId: String,
): ArenaStateUpdate {
    val displayNames =
        when (this) {
            is ArenaMatch.ChoosingClasses -> playerMap.mapValues { it.value.displayName }

            is ArenaMatch.Combat -> playerMap.mapValues { it.value.displayName }

            is ArenaMatch.WaitingForAllPlayers,
            is ArenaMatch.Finished -> emptyMap()
        }

    val messages =
        messages.map { incomingMessage ->
            incomingMessage.message.toState(id = incomingMessage.id, displayNames = displayNames)
        }
    val unhandledMessagesCount = (previousState?.unhandledMessagesCount ?: 0) + numberOfNewMessages
    val previousSeconds = previousState?.secondsRemaining ?: 0
    val previousShowForfeitDialog = previousState?.showForfeitDialog ?: false

    val nextArenaState =
        when (this) {
            is ArenaMatch.WaitingForAllPlayers -> ArenaState.WaitingForAllPlayers

            is ArenaMatch.ChoosingClasses -> {
                val (unavailable, selections) = playerMap.prepareClassSelectionInfo(userId = userId)

                ArenaState.ChoosingClasses(
                    playerMap = playerMap.mapValues { it.value.toState() },
                    activePlayerUserId = activePlayer.userId,
                    userPlayerId = userId,
                    unavailableClasses = unavailable,
                    classSelections = selections,
                    selectedClass =
                        (previousState?.arenaState as? ArenaState.ChoosingClasses)?.selectedClass,
                )
            }

            is ArenaMatch.Combat -> {
                // TODO: Pass along UserPlayerKnockedOut? Use isConscious on user to determine

                ArenaState.Combat(
                    playerMap = playerMap.mapValues { it.value.toSelectedState() },
                    userPlayerId = userId,
                    combatActionState = nextCombatAction.toCombatActionState(),
                    roundNumber = roundNumber,
                    isNewRound = isNewRound,
                )
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
            // TODO: Hmm, how should timers be started? Based on NextCombatAction?
            TimerAction.Start(timerDuration = 20)
        }

        is ArenaState.UserPlayerKnockedOut,
        is ArenaState.Finished -> TimerAction.Stop
    }

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
