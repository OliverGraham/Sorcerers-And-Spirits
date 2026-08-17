package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState

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

internal fun ArenaMatchState.players(): List<PlayerState> =
    when (this) {
        is ActiveMatchState -> playerMap.values.toList()
        is ArenaMatchState.Finished -> players
        is ArenaMatchState.WaitingForAllPlayers -> emptyList()
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
