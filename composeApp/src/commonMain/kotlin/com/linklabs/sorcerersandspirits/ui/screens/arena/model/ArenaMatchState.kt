package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState

private const val MINIMUM_PLAYERS_FOR_NEXT_MATCH = 2

@Immutable
sealed interface ArenaMatchState {

    @Immutable data object WaitingForAllPlayers : ArenaMatchState

    @Immutable
    data class ChoosingClasses(
        override val playerMap: Map<UserId, PlayerState>,
        override val userPlayerId: String,
        val activePlayerUserId: String,
        val selectedClass: CharacterClassState?,
        val unavailableClasses: Set<CharacterClassType>,
        val classSelections: Map<CharacterClassType, String>,
        val userConfirmedClassType: CharacterClassType?,
    ) : ActiveMatchState {
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
        val attackerUserId: String,
        val turnOwnerClassType: CharacterClassType,
        val lockedTargetUserId: String?,
        val diceRollState: DiceRollState?,
        val selectedOpponentUserId: String? = null,
        val isActionInFlight: Boolean = false,
    ) : ActiveMatchState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val isItThisUsersTurn = userPlayerId in activeUserIds

        // TODO(SS-029): Obviously, this was supposed to simply come from the domain
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

        val teleportRollOffKey = combatActionState.teleportRollOffKey()

        val attacker = playerMap.getValue(key = attackerUserId)

        val targetedUserId = getTargetedPlayerUserId()

        // TODO(SS-234): For "Choose your target avatar" if this is null, then show it
        val targetedPlayer = playerMap[targetedUserId]

        private val isTwoPlayerMatch = playerMap.size == 2
        val bottomPlayer = if (isTwoPlayerMatch) userPlayer else attacker
        val topPlayer =
            if (isTwoPlayerMatch) {
                playerMap.values.firstOrNull { it.userId != userPlayerId }
            } else {
                targetedPlayer
            }
        val animatedContentKey = "${bottomPlayer.userId}|${topPlayer?.userId}"

        val playersInTheStands = getPlayersInTheStands(targetedUserId = targetedUserId)
        val areTherePlayersInTheStands = playersInTheStands.isNotEmpty()

        fun canTurnOwnerTargetAnOpponent(isAnAnimationActive: Boolean) = !isAnAnimationActive &&
            isItThisUsersTurn &&
            !isMatchOver &&
            lockedTargetUserId.isNullOrEmpty()
    }

    @Immutable
    data class Finished(
        val players: List<PlayerState.Selected>,
    ) : ArenaMatchState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
        val canStartNextMatch = players.size >= MINIMUM_PLAYERS_FOR_NEXT_MATCH
    }
}

@Immutable
sealed interface ActiveMatchState : ArenaMatchState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

internal fun ArenaMatch.ChoosingClasses.toChoosingClassesState(
    userPlayerId: String,
    defaultSelectedClass: CharacterClassState?,
) = ArenaMatchState.ChoosingClasses(
    playerMap = playerMap.toState(),
    userPlayerId = userPlayerId,
    activePlayerUserId = activePlayerUserId,
    selectedClass = defaultSelectedClass,
    unavailableClasses = emptySet(),
    classSelections = emptyMap(),
    userConfirmedClassType = playerMap.userConfirmedClassType(userId = userPlayerId),
)

private fun ArenaMatchState.Combat.getTargetedPlayerUserId() = lockedTargetUserId
    ?: selectedOpponentUserId
    ?: combatActionState.defenderUserId()
    ?: playerMap.values
        .singleOrNull { it.userId != attackerUserId && it.characterClass.isConscious }
        ?.userId

private fun ArenaMatchState.Combat.getPlayersInTheStands(targetedUserId: String?) = playerMap.filterKeys { it != attackerUserId && it != targetedUserId }.values.toList()

internal fun CombatActionState.teleportAction(): AbilityActionState.Passive.Teleport? {
    val abilityRoll = this as? CombatActionState.AbilityRoll ?: return null

    return abilityRoll.action as? AbilityActionState.Passive.Teleport
}

private fun CombatActionState.teleportRollOffKey(): Int? = teleportAction()?.rollOffAttempt

private fun ArenaCombatControl.isImpersonate(): Boolean = this is ArenaCombatControl.ActivatedAbility &&
    action is AbilityActionState.Activated.Impersonate
