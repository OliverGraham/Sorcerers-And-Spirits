package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.MyDiceRollState

private const val MINIMUM_PLAYERS_FOR_NEXT_MATCH = 2

@Immutable
sealed interface ArenaState {

    @Immutable data object WaitingForAllPlayers : ArenaState

    @Immutable
    data class ChoosingClasses(
        override val playerMap: Map<UserId, PlayerState>,
        override val userPlayerId: String,
        val activePlayerUserId: String,
        val selectedClass: CharacterClassState?,
        val unavailableClasses: Set<CharacterClassType>,
        val classSelections: Map<CharacterClassType, String>,
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
        val isMirrorImagePenaltyActive: Boolean,
        val turnOwnerUserId: String,
        val lockedTargetUserId: String?,
        val myDiceRollState: MyDiceRollState?,
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val isDoublesBonusLive = doublesBonus != null && doublesBonus.userId in activeUserIds
        val isItThisUsersTurn = userPlayerId in activeUserIds

        // TODO: Obviously, this was supposed to simply come from the domain
        val combatControls = deriveCombatControls()

        val diceBadge = deriveDiceBadge()
    }

    @Immutable
    data class Finished(
        val players: List<PlayerState.Selected>,
        val hasAnyPlayerReadied: Boolean = false,
    ) : ArenaState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
        val canStartNextMatch = players.size >= MINIMUM_PLAYERS_FOR_NEXT_MATCH
    }
}

@Immutable
sealed interface ActiveState : ArenaState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

private fun ArenaState.Combat.deriveDiceBadge() =
    when {
        isDoublesBonusLive -> DiceBadge.DoublesBonus
        isMirrorImagePenaltyActive -> DiceBadge.MirrorPenalty
        else -> null
    }
