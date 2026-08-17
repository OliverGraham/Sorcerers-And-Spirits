package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.isKnockedOut
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerUserId
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.toState

internal const val TIMER_VISIBLE_THRESHOLD_SECONDS = 15
internal const val TIMER_URGENT_THRESHOLD_SECONDS = 10
internal const val WAITING_TIMER_DURATION_SECONDS = 30
internal const val CHOOSING_CLASSES_TIMER_DURATION_SECONDS = 30
internal const val COMBAT_TIMER_DURATION_SECONDS = 45
internal const val MATCH_FINISHED_TIMER_DURATION_SECONDS = 30
internal const val GAUNTLET_FINISHED_TIMER_DURATION_SECONDS = 60

// TODO: Can this massive data class be reduced in some way?
//       Rework how ArenaState.Combat gets updated after the MessageQueue dequeues everything
//       Can messages be an ordered map? Also, why is there messages and messageGroups? should only
//       be messageGroups
//       Remove:
//       - all default arguments?
@Immutable
data class ArenaScreenState(
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val isUserPlayerKnockedOut: Boolean,
    val hasReadiedForNextMatch: Boolean,
    val arenaMatchState: ArenaMatchState,
    val messageGroups: List<ArenaMessageGroupState>,
    val gauntletProgress: GauntletProgress = GauntletProgress(),
    val characterClasses: List<CharacterClassState> = emptyList(),
    val classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
    val animatingMessage: ArenaMessageState? = null,
) {
    val isTimerVisible: Boolean = secondsRemaining in 1..TIMER_VISIBLE_THRESHOLD_SECONDS
    val isTimerUrgent: Boolean = secondsRemaining in 1..TIMER_URGENT_THRESHOLD_SECONDS
    val isMatchEnding: Boolean = (arenaMatchState as? ArenaMatchState.Combat)?.isMatchOver == true

    val latestCombatHeaderMessage = messageGroups.getLatestCombatHeaderMessage()
}

private fun List<ArenaMessageGroupState>.getLatestCombatHeaderMessage(): ArenaMessageState? {
    val latestGroupMessage = lastOrNull() ?: return null
    if (latestGroupMessage !is ArenaMessageGroupState.Round) return null

    return latestGroupMessage.messages.lastOrNull {
        it !is ArenaMessageState.NewRound &&
            it !is ArenaMessageState.NewTurn &&
            it !is ArenaMessageState.NormalRoll.Attack
    }
}

internal fun ArenaMatch.toState(
    userId: String,
    previousClassSelection: CharacterClassState?,
    characterClasses: List<CharacterClassState>,
) =
    when (this) {
        is ArenaMatch.WaitingForAllPlayers -> ArenaMatchState.WaitingForAllPlayers

        is ArenaMatch.ChoosingClasses -> {
            val (unavailable, selections) = playerMap.prepareClassSelectionInfo(userId = userId)

            ArenaMatchState.ChoosingClasses(
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

        is ArenaMatch.Combat ->
            ArenaMatchState.Combat(
                playerMap = playerMap.mapValues { it.value.toSelectedState() },
                userPlayerId = userId,
                combatActionState = nextCombatAction.toCombatActionState(),
                roundNumber = roundNumber,
                isNewRound = isNewRound,
                isMatchOver = isMatchOver,
                turnOwnerUserId = nextCombatAction.turnOwnerUserId(),
                lockedTargetUserId = nextCombatAction.lockedTargetUserId(userId = userId),
                diceRollState = diceRoll?.toState(),
                isActionInFlight = false,
            )

        is ArenaMatch.Finished ->
            ArenaMatchState.Finished(
                players =
                    playerMap.values.map { it.toState() }.filterIsInstance<PlayerState.Selected>(),
                hasAnyPlayerReadied = playerMap.values.any { it.isReadyForNextMatch },
            )
    }


// TODO: Look at update playerMap. See what classes are taken - those are unavailable
internal fun Map<String, ArenaPlayer>.prepareClassSelectionInfo(
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

// TODO: This shit isn't even working correctly, Claude. Can still get duplicate classes, for example
internal fun List<CharacterClassState>.resolveSelectedClass(
    previousSelection: CharacterClassState?,
    unavailableClasses: Set<CharacterClassType>,
): CharacterClassState? =
    previousSelection ?: defaultSelectedClass(unavailableClasses = unavailableClasses)


private fun List<CharacterClassState>.defaultSelectedClass(
    unavailableClasses: Set<CharacterClassType>
): CharacterClassState? {
    val rogue = firstOrNull { it.type == CharacterClassType.Rogue }
    val isRogueAvailable = rogue != null && rogue.type !in unavailableClasses

    return if (isRogueAvailable) rogue else firstOrNull { it.type !in unavailableClasses }
}


// TODO: Group similar code into separate files when it grows like this. 3 timer-related functions.
internal fun determineTimerAction(
    previous: ArenaMatchState?,
    next: ArenaMatchState,
    isFinalMatch: Boolean,
): TimerAction =
    when (next) {
        is ArenaMatchState.WaitingForAllPlayers ->
            if (previous == null) {
                TimerAction.Start(timerDuration = WAITING_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaMatchState.ChoosingClasses ->
            if (
                previous !is ArenaMatchState.ChoosingClasses ||
                    previous.activePlayerUserId != next.activePlayerUserId
            ) {
                TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaMatchState.Combat -> {
            val isNewCombatInteraction =
                previous !is ArenaMatchState.Combat ||
                    previous.combatActionState != next.combatActionState ||
                    previous.hasActivePlayerActedWhileStillActive(next = next)
            if (isNewCombatInteraction) {
                TimerAction.Start(timerDuration = COMBAT_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }
        }

        is ArenaMatchState.Finished ->
            if (isFinalMatch) {
                determineGauntletFinishedTimerAction(previous = previous, next = next)
            } else {
                determineMatchFinishedTimerAction(previous = previous, next = next)
            }
    }

// TODO: WTF is this?
private fun ArenaMatchState.Combat.hasActivePlayerActedWhileStillActive(
    next: ArenaMatchState.Combat
): Boolean {
    val previousIdle = combatActionState as? CombatActionState.Idle ?: return false
    val nextIdle = next.combatActionState as? CombatActionState.Idle ?: return false
    if (previousIdle.activeUserIds != nextIdle.activeUserIds) return false

    return nextIdle.activeUserIds.any { userId -> playerMap[userId] != next.playerMap[userId] }
}

private fun determineMatchFinishedTimerAction(
    previous: ArenaMatchState?,
    next: ArenaMatchState.Finished,
): TimerAction =
    when {
        !next.canStartNextMatch -> TimerAction.Stop
        previous is ArenaMatchState.Finished -> TimerAction.None
        else -> TimerAction.Start(timerDuration = MATCH_FINISHED_TIMER_DURATION_SECONDS)
    }

private fun determineGauntletFinishedTimerAction(
    previous: ArenaMatchState?,
    next: ArenaMatchState.Finished,
): TimerAction {
    val wasAlreadyCountingDown = previous is ArenaMatchState.Finished && previous.hasAnyPlayerReadied
    return when {
        !next.hasAnyPlayerReadied -> TimerAction.Stop
        wasAlreadyCountingDown -> TimerAction.None
        else -> TimerAction.Start(timerDuration = GAUNTLET_FINISHED_TIMER_DURATION_SECONDS)
    }
}

data class ArenaStateUpdate(
    val nextState: ArenaScreenState,
    val timerAction: TimerAction,
    val newMessages: List<ArenaMessageState>,
)
