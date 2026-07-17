package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.isKnockedOut
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerUserId
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.toState

private const val TIMER_VISIBLE_THRESHOLD_SECONDS = 15
private const val TIMER_URGENT_THRESHOLD_SECONDS = 10
private const val WAITING_TIMER_DURATION_SECONDS = 30
private const val CHOOSING_CLASSES_TIMER_DURATION_SECONDS = 30
private const val COMBAT_TIMER_DURATION_SECONDS = 45
private const val MATCH_FINISHED_TIMER_DURATION_SECONDS = 30
private const val GAUNTLET_FINISHED_TIMER_DURATION_SECONDS = 60

// TODO: Can this massive data class be reduced in some way?
@Immutable
data class ArenaScreenState(
    val messages: List<ArenaMessageState>,
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val isUserPlayerKnockedOut: Boolean,
    val hasReadiedForNextMatch: Boolean,
    val arenaState: ArenaState,
    val participants: Map<String, ParticipantClass>,
    val numberOfNewMessages: Int,
    val gauntletProgress: GauntletProgress = GauntletProgress(),
    val characterClasses: List<CharacterClassState> = emptyList(),
    val classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
    val pendingFinishedState: ArenaState.Finished? = null,
    val playbackMessages: List<PlayableMessage> = messages.map { it.toPlayable() },
) {
    val isTimerVisible: Boolean = secondsRemaining in 1..TIMER_VISIBLE_THRESHOLD_SECONDS
    val isTimerUrgent: Boolean = secondsRemaining in 1..TIMER_URGENT_THRESHOLD_SECONDS
    val isFinishPending: Boolean = pendingFinishedState != null
}

// TODO(SS-031): Need to refactor this badly

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaScreenState?,
    userId: String,
    characterClasses: List<CharacterClassState> = emptyList(),
    classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
): ArenaStateUpdate {
    // TODO: Make function - do this later, Claude
    val participants =
        when (this) {
            is ArenaMatch.Combat -> this.participants
            is ArenaMatch.Finished -> this.participants
            else -> emptyMap()
        }.mapValues { ParticipantClass(className = it.value.name, type = it.value) }

    val messages = messages.toStates(participants = participants)

    val previousSeconds = previousState?.secondsRemaining ?: 0
    val previousShowForfeitDialog = previousState?.showForfeitDialog ?: false

    val computedArenaState =
        toState(
            userId = userId,
            previousClassSelection =
                (previousState?.arenaState as? ArenaState.ChoosingClasses)?.selectedClass,
            characterClasses = characterClasses,
        )

    val (displayedArenaState, pendingFinishedState) =
        holdCombatForClosingFlash(
            computedState = computedArenaState,
            previousState = previousState?.arenaState,
        )

    val gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet)

    val userPlayer = playerMap.getValue(key = userId)

    // TODO: CLAUDE YOU ASSHOLE!!!!! MAKE FUNCTIONS FOR TIMER-LOGIC
    // While the Finished transition is held, the match is over, so the combat clock is stopped
    // rather than left running against the held combat view.
    val timerAction =
        if (pendingFinishedState != null) {
            TimerAction.Stop
        } else {
            determineTimerAction(
                previous = previousState?.arenaState,
                next = displayedArenaState,
                isFinalMatch = gauntletProgress.isFinalMatch,
            )
        }

    val secondsRemaining =
        when (timerAction) {
            is TimerAction.Start -> timerAction.timerDuration
            TimerAction.Stop -> 0
            TimerAction.None -> previousSeconds
        }

    val finalState =
        ArenaScreenState(
            messages = messages,
            secondsRemaining = secondsRemaining,
            showForfeitDialog = previousShowForfeitDialog,
            isUserPlayerKnockedOut = userPlayer.isKnockedOut(),
            hasReadiedForNextMatch = userPlayer.isReadyForNextMatch,
            arenaState = displayedArenaState,
            participants = participants,
            gauntletProgress = gauntletProgress,
            characterClasses = characterClasses,
            classCopy = classCopy,
            pendingFinishedState = pendingFinishedState,
            numberOfNewMessages = numberOfNewMessages,
        )

    return ArenaStateUpdate(nextState = finalState, timerAction = timerAction)
}

private fun ArenaMatch.toState(
    userId: String,
    previousClassSelection: CharacterClassState?,
    characterClasses: List<CharacterClassState>,
) =
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
                    characterClasses.resolveSelectedClass(
                        previousSelection = previousClassSelection,
                        unavailableClasses = unavailable,
                    ),
            )
        }

        is ArenaMatch.Combat -> {
            ArenaState.Combat(
                playerMap = playerMap.mapValues { it.value.toSelectedState() },
                userPlayerId = userId,
                combatActionState = nextCombatAction.toCombatActionState(),
                roundNumber = roundNumber,
                isNewRound = isNewRound,
                turnOwnerUserId = nextCombatAction.turnOwnerUserId(),
                lockedTargetUserId = nextCombatAction.lockedTargetUserId(userId = userId),
                myDiceRollState = myDiceRoll?.toState(),
            )
        }

        is ArenaMatch.Finished ->
            ArenaState.Finished(
                players =
                    playerMap.values.map { it.toState() }.filterIsInstance<PlayerState.Selected>(),
                hasAnyPlayerReadied = playerMap.values.any { it.isReadyForNextMatch },
            )
    }

/**
 * Briefly holds the combat view on the Combat→Finished transition so the closing flash callout
 * (SS-148) can play before the Finished screen replaces it (SS-154). When the match has just
 * finished, the combat view is held in place—updated to show the final knockout—and the finished
 * state is returned as pending, for the ViewModel to advance to after a fixed hold. Otherwise the
 * computed state is shown as is.
 */
private fun holdCombatForClosingFlash(
    computedState: ArenaState,
    previousState: ArenaState?,
): Pair<ArenaState, ArenaState.Finished?> {
    if (computedState !is ArenaState.Finished) return computedState to null
    if (previousState !is ArenaState.Combat) return computedState to null

    val heldCombatState = previousState.withFinalPlayers(finishedPlayers = computedState.players)
    return heldCombatState to computedState
}

/**
 * Returns this combat view with each player replaced by their final, match-end state, so the held
 * view reflects the closing knockout while the Finished transition is held (SS-154).
 */
private fun ArenaState.Combat.withFinalPlayers(
    finishedPlayers: List<PlayerState.Selected>
): ArenaState.Combat {
    val finalPlayersById = finishedPlayers.associateBy { it.userId }
    val updatedPlayerMap =
        playerMap.mapValues { (userId, player) -> finalPlayersById[userId] ?: player }
    return copy(playerMap = updatedPlayerMap)
}

private fun List<CharacterClassState>.defaultSelectedClass(
    unavailableClasses: Set<CharacterClassType>
): CharacterClassState? {
    val rogue = firstOrNull { it.type == CharacterClassType.Rogue }
    val isRogueAvailable = rogue != null && rogue.type !in unavailableClasses

    return if (isRogueAvailable) rogue else firstOrNull { it.type !in unavailableClasses }
}

internal fun List<CharacterClassState>.resolveSelectedClass(
    previousSelection: CharacterClassState?,
    unavailableClasses: Set<CharacterClassType>,
): CharacterClassState? =
    previousSelection ?: defaultSelectedClass(unavailableClasses = unavailableClasses)

private fun Map<String, ArenaPlayer>.prepareClassSelectionInfo(
    userId: String
): Pair<Set<CharacterClassType>, Map<CharacterClassType, String>> {
    val allPlayers = values.map { player -> player.toState() }
    // Keyed on class type, not the full CharacterClassState: the no-duplicate-class rule and the
    // "chosen by" labels are about class identity, and a player's selected state can differ from
    // the master template by mutable fields (HP, ability counters), so value equality would let a
    // duplicate slip through (SS-122).
    val unavailableClasses =
        allPlayers
            .filter { player -> player.userId != userId }
            .mapNotNull { player -> (player as? PlayerState.Selected)?.characterClass?.type }
            .toSet()

    val classSelections =
        allPlayers.filterIsInstance<PlayerState.Selected>().associate { player ->
            player.characterClass.type to player.displayName
        }

    return unavailableClasses to classSelections
}

// TODO: Group similar code into separate files when it grows like this. 3 timer-related functions.
internal fun determineTimerAction(
    previous: ArenaState?,
    next: ArenaState,
    isFinalMatch: Boolean,
): TimerAction =
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

        is ArenaState.Finished ->
            if (isFinalMatch) {
                determineGauntletFinishedTimerAction(previous = previous, next = next)
            } else {
                determineMatchFinishedTimerAction(previous = previous, next = next)
            }
    }

private fun determineMatchFinishedTimerAction(
    previous: ArenaState?,
    next: ArenaState.Finished,
): TimerAction =
    when {
        !next.canStartNextMatch -> TimerAction.Stop
        previous is ArenaState.Finished -> TimerAction.None
        else -> TimerAction.Start(timerDuration = MATCH_FINISHED_TIMER_DURATION_SECONDS)
    }

private fun determineGauntletFinishedTimerAction(
    previous: ArenaState?,
    next: ArenaState.Finished,
): TimerAction {
    val wasAlreadyCountingDown = previous is ArenaState.Finished && previous.hasAnyPlayerReadied
    return when {
        !next.hasAnyPlayerReadied -> TimerAction.Stop
        wasAlreadyCountingDown -> TimerAction.None
        else -> TimerAction.Start(timerDuration = GAUNTLET_FINISHED_TIMER_DURATION_SECONDS)
    }
}

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
