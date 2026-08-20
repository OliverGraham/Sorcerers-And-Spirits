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
    val isMatchEnding: Boolean = (arenaMatchState as? ArenaMatchState.Combat)?.isMatchOver == true

    val latestCombatHeaderMessage = messageGroups.getLatestCombatHeaderMessage()

    val displayedMessageCount = messageGroups.sumOf { it.messages.size }
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
