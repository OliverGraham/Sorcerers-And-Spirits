package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.DefensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState

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
        val isMatchOver: Boolean,
        val turnOwnerUserId: String,
        val lockedTargetUserId: String?,
        val diceRollState: DiceRollState?,
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val isItThisUsersTurn = userPlayerId in activeUserIds

        // TODO: Obviously, this was supposed to simply come from the domain
        val combatControls = deriveCombatControls()
        val arenaCombatControl =
            combatControls.firstOrNull { it is ArenaCombatControl.NormalAttack }
        val impersonateControl = combatControls.firstOrNull { it.isImpersonate() }
        val abilityControls =
            combatControls.filter { it != arenaCombatControl && it != impersonateControl }

        val diceBadgeState = deriveDiceBadge()
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

private fun ArenaState.Combat.deriveDiceBadge(): DiceBadgeState? {
    if (isMatchOver) return null

    val doublesBonus = diceRollState?.doublesBonus
    val isDoublesBonusLive = doublesBonus != null && doublesBonus.userId in activeUserIds
    if (isDoublesBonusLive) return DiceBadgeState.DoublesBonus()

    val rerolledDiePosition =
        playerMap.values.firstNotNullOfOrNull {
            (it.characterClass.defensiveAbility as? DefensiveAbilityState.MirrorImage)
                ?.rerolledDiePosition
        } ?: return null

    val currentRoll = diceRollState
    val isRerollStillShowing =
        currentRoll is DiceRollState.OneDie && currentRoll.diePosition == rerolledDiePosition
    if (!isRerollStillShowing) return null

    return DiceBadgeState.MirrorImageBonus(diePosition = rerolledDiePosition)
}

private fun ArenaCombatControl.isImpersonate(): Boolean =
    this is ArenaCombatControl.ActivatedAbility &&
        action is AbilityActionState.Activated.Impersonate
