package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
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

        val isAwaitingTeleportRoll =
            combatControls.any {
                it is ArenaCombatControl.AbilityRoll &&
                    it.action is AbilityActionState.Passive.Teleport
            }

        val diceBadgeState = deriveDiceBadge()

        private val diceTints = deriveTeleportDiceTints()
        val firstDieTint = diceTints.first
        val secondDieTint = diceTints.second
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

    val currentRoll = diceRollState as? DiceRollState.OneDie ?: return null
    if (!currentRoll.isMirrorImageReroll) return null

    return DiceBadgeState.MirrorImageBonus(diePosition = currentRoll.diePosition)
}

private fun ArenaState.Combat.deriveTeleportDiceTints(): Pair<Color?, Color?> {
    val abilityRoll = combatActionState as? CombatActionState.AbilityRoll ?: return null to null
    val teleport = abilityRoll.action as? AbilityActionState.Passive.Teleport ?: return null to null

    return classColorAt(teleport = teleport, position = DiePosition.First) to
        classColorAt(teleport = teleport, position = DiePosition.Second)
}

private fun ArenaState.Combat.classColorAt(
    teleport: AbilityActionState.Passive.Teleport,
    position: DiePosition,
): Color? {
    val ownerUserId =
        if (teleport.attackerRoll.diePosition == position) {
            teleport.attackerRoll.userId
        } else {
            teleport.defenderRoll.userId
        }

    val player = playerMap[ownerUserId] ?: return null
    return player.characterClass.type.toColor()
}

private fun ArenaCombatControl.isImpersonate(): Boolean =
    this is ArenaCombatControl.ActivatedAbility &&
        action is AbilityActionState.Activated.Impersonate
