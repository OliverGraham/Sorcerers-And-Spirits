package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState

private const val MINIMUM_PLAYERS_FOR_NEXT_MATCH = 2

private val DEFAULT_DOUBLES_DIE_POSITION = DiePosition.Second

// TODO: Call it ArenaMatchState
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
        val selectedOpponentUserId: String? = null,
        val isActionInFlight: Boolean = false,
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

        val isTeleportDoublesBonusSpent =
            combatActionState.teleportAction()?.let { it.attackerDoublesBonus == 0 } ?: false

        val turnOwner = playerMap.getValue(key = turnOwnerUserId)
        val turnOwnerCharacterClass = turnOwner.characterClass

        val targetedUserId = getTargetedPlayerUserId()

        // TODO: For "Choose your target avatar" if this is null, then show it
        val targetedPlayer = playerMap[targetedUserId]
        val playersInTheStands = getPlayersInTheStands(targetedUserId = targetedUserId)
        val areTherePlayersInTheStands = playersInTheStands.isNotEmpty()

        fun canTurnOwnerTargetAnOpponent(isAnAnimationActive: Boolean) =
            !isAnAnimationActive &&
                isItThisUsersTurn &&
                !isMatchOver &&
                lockedTargetUserId.isNullOrEmpty()
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

internal fun ArenaMatch.ChoosingClasses.toChoosingClassesState(
    userPlayerId: String
) = ArenaState.ChoosingClasses(
    playerMap = playerMap.toState(),
    userPlayerId = userPlayerId,
    activePlayerUserId = activePlayerUserId,
    selectedClass = null,
    unavailableClasses = emptySet(),
    classSelections = emptyMap()
)

private fun ArenaState.Combat.getTargetedPlayerUserId() =
    lockedTargetUserId
        ?: selectedOpponentUserId
        ?: playerMap.values
            .singleOrNull { it.userId != turnOwnerUserId && it.characterClass.isConscious }
            ?.userId

private fun ArenaState.Combat.getPlayersInTheStands(targetedUserId: String?) =
    playerMap.filterKeys { it != turnOwnerUserId && it != targetedUserId }.values.toList()

private fun ArenaState.Combat.deriveDiceBadge(): DiceBadgeState? {
    if (isMatchOver) return null

    val hasDoublesRoll = diceRollState?.doublesBonus != null
    if (hasDoublesRoll) {
        return DiceBadgeState.DoublesBonus(diePosition = doublesBonusDiePosition())
    }

    val currentRoll = diceRollState as? DiceRollState.OneDie ?: return null
    if (!currentRoll.isMirrorImageReroll) return null

    return DiceBadgeState.MirrorImageBonus(diePosition = currentRoll.diePosition)
}

private fun ArenaState.Combat.doublesBonusDiePosition(): DiePosition {
    val teleport = combatActionState.teleportAction() ?: return DEFAULT_DOUBLES_DIE_POSITION
    return teleport.attackerRoll.diePosition
}

internal fun CombatActionState.teleportAction(): AbilityActionState.Passive.Teleport? {
    val abilityRoll = this as? CombatActionState.AbilityRoll ?: return null

    return abilityRoll.action as? AbilityActionState.Passive.Teleport
}

private fun ArenaCombatControl.isImpersonate(): Boolean =
    this is ArenaCombatControl.ActivatedAbility &&
        action is AbilityActionState.Activated.Impersonate
