package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState

private const val TIMER_VISIBLE_THRESHOLD_SECONDS = 15
private const val TIMER_URGENT_THRESHOLD_SECONDS = 10
private const val WAITING_TIMER_DURATION_SECONDS = 30
private const val CHOOSING_CLASSES_TIMER_DURATION_SECONDS = 30
private const val COMBAT_TIMER_DURATION_SECONDS = 45

@Immutable
data class ArenaScreenState(
    val messages: List<ArenaMessageState>,
    val unhandledMessagesCount: Int,
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val isUserPlayerKnockedOut: Boolean,
    val hasReadiedForNextMatch: Boolean,
    val arenaState: ArenaState,
    val participantClassNames: Map<String, String>,
    val gauntletProgress: GauntletProgress = GauntletProgress(),
    val characterClasses: List<CharacterClassState> = emptyList(),
) {
    val isTimerVisible: Boolean = secondsRemaining in 1..TIMER_VISIBLE_THRESHOLD_SECONDS

    val isTimerUrgent: Boolean = secondsRemaining in 1..TIMER_URGENT_THRESHOLD_SECONDS

    val latestMessage: ArenaMessageState? =
        messages.getOrNull(
            index = messages.size - unhandledMessagesCount.coerceAtLeast(minimumValue = 1)
        )

    val filteredMessages: List<ArenaMessageState> =
        messages.dropLast(n = unhandledMessagesCount.coerceAtLeast(minimumValue = 1))

    /**
     * Faces of the most recent roll, so the shake-to-roll dice settle on the rolled value (SS-077).
     */
    val latestRolledDice: List<Int> = messages.latestRolledDice()

    private val isUserPlayerActing: Boolean =
        (arenaState as? ArenaState.Combat)?.isItThisUsersTurn ?: false

    /**
     * Whether to prompt a knocked-out user to observe or leave. A knocked-out Paladin who is still
     * acting (owes a Holy Shield survival roll) has not yet been definitively knocked out, so no
     * prompt is offered, and none is offered once the user has already chosen to stay.
     */
    val isKnockedOutDecisionOffered: Boolean =
        !hasReadiedForNextMatch && isUserPlayerKnockedOut && !isUserPlayerActing
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
        val doublesBonus: DoublesBonus?,
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val otherPlayers = playerMap.filterKeys { it !in activeUserIds }
        val isItThisUsersTurn = userPlayerId in activeUserIds
        val combatControls = deriveCombatControls()
        val lockedTargetUserId = deriveLockedTargetUserId()
    }

    @Immutable
    data class Finished(val players: List<PlayerState.Selected>) : ArenaState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
    }
}

@Immutable
sealed interface ActiveState : ArenaState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

/**
 * The opponent the user is committed to attacking while one of their own normal attacks is
 * mid-resolution — the Armor-class roll is already committed and a follow-up roll is owed. The UI
 * pins the target to this opponent and stops offering others, so the user cannot change targets
 * after rolling. Null when the user is free to choose a target: at the start of their turn or when
 * an ability selects its own target.
 */
private fun ArenaState.Combat.deriveLockedTargetUserId(): String? {
    val inFlightAttack = combatActionState as? CombatActionState.NormalAttack ?: return null
    val isUsersOwnAttack = inFlightAttack.attackerUserId == userPlayerId
    return if (isUsersOwnAttack) inFlightAttack.defenderUserId else null
}

// TODO(SS-031): Need to refactor this badly

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaScreenState?,
    userId: String,
    characterClasses: List<CharacterClassState> = emptyList(),
): ArenaStateUpdate {
    val userPlayer = playerMap.getValue(key = userId)

    // ArenaMessages refer to players by class name (SS-097), so the resolution map is keyed on the
    // class each player selected. Players who have left the arena are retained so their earlier
    // messages still resolve.
    val currentClassNames =
        playerMap.values.filterIsInstance<ArenaPlayer.Selected>().associate {
            it.userId to it.characterClass.className
        }
    val participantClassNames =
        (previousState?.participantClassNames ?: emptyMap()) + currentClassNames

    val messages =
        messages.map { incomingMessage ->
            incomingMessage.message.toState(
                id = incomingMessage.id,
                classNames = participantClassNames,
            )
        }
    // The log is capped (oldest entries scroll off) and resets to a fresh, small list when a new
    // match begins, so the running unhandled count can outrun what the log actually holds — e.g. a
    // big combat result, or leftover unhandled messages carried over from the prior match. If it
    // exceeds the list size, latestMessage resolves to null and filteredMessages to empty, wiping
    // the log with no way to recover, so it can never hold more than the messages that remain.
    val unhandledMessagesCount =
        ((previousState?.unhandledMessagesCount ?: 0) + numberOfNewMessages).coerceAtMost(
            maximumValue = messages.size
        )
    val previousSeconds = previousState?.secondsRemaining ?: 0
    val previousShowForfeitDialog = previousState?.showForfeitDialog ?: false

    val isUserPlayerKnockedOut =
        (userPlayer as? ArenaPlayer.Selected)?.characterClass?.isConscious()?.not() ?: false

    // The stay/leave choice is per-match, so it resets whenever a new match begins: at class
    // selection, and (with classes locked for the Gauntlet) when the next match starts combat
    // straight from the previous match's Finished screen.
    val isStartingNewMatch =
        this is ArenaMatch.Combat && previousState?.arenaState is ArenaState.Finished
    val hasReadiedForNextMatch =
        if (this is ArenaMatch.ChoosingClasses || isStartingNewMatch) {
            false
        } else {
            previousState?.hasReadiedForNextMatch ?: false
        }

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
                ArenaState.Combat(
                    playerMap = playerMap.mapValues { it.value.toSelectedState() },
                    userPlayerId = userId,
                    combatActionState = nextCombatAction.toCombatActionState(),
                    roundNumber = roundNumber,
                    isNewRound = isNewRound,
                    doublesBonus = doublesBonus,
                )
            }

            is ArenaMatch.Finished ->
                ArenaState.Finished(
                    players =
                        playerMap.values
                            .map { it.toState() }
                            .filterIsInstance<PlayerState.Selected>()
                )
        }

    val nextScreenState =
        ArenaScreenState(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = previousSeconds,
            showForfeitDialog = previousShowForfeitDialog,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = nextArenaState,
            participantClassNames = participantClassNames,
            gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet),
            characterClasses = characterClasses,
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
                TimerAction.Start(timerDuration = WAITING_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaState.ChoosingClasses ->
            if (
                previous !is ArenaState.ChoosingClasses ||
                    previous.activePlayerUserId != next.activePlayerUserId
            ) {
                TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        // The combat clock belongs to the action a player currently owes, so it (re)starts only
        // when that action changes (a new turn or roll) and keeps running across unrelated updates
        // such as incoming messages.
        is ArenaState.Combat ->
            if (
                previous !is ArenaState.Combat ||
                    previous.combatActionState != next.combatActionState
            ) {
                TimerAction.Start(timerDuration = COMBAT_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaState.Finished -> TimerAction.Stop
    }

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
