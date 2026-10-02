package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.arena.model.ParticipantStatus
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState

private const val MINIMUM_PLAYERS_FOR_NEXT_MATCH = 2
private const val WINNER_PLACE = 1

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
        val participantMap: Map<UserId, MatchParticipantState>,
        override val userPlayerId: String,
        val combatActionState: CombatActionState,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val isMatchOver: Boolean,
        val attackerUserId: String,
        val turnOwnerClassType: CharacterClassType,
        val lockedTargetUserId: String?,
        val currentTargetUserId: String?,
        val diceRollState: DiceRollState?,
        val isActionInFlight: Boolean = false,
    ) : ActiveMatchState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val isItThisUsersTurn = userPlayerId in activeUserIds

        val charmTargetableUserIds: Set<String> =
            if (isItThisUsersTurn) {
                combatActionState.charmTargetableUserIds()
            } else {
                emptySet()
            }

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

        private val isTwoPlayerMatch = playerMap.size == 2
        val bottomPlayer = if (isTwoPlayerMatch) userPlayer else attacker

        fun resolveTargetUserId(selectedOpponentUserId: String?): String? = lockedTargetUserId
            ?: selectedOpponentUserId
            ?: currentTargetUserId
            ?: combatActionState.defenderUserId()
            ?: playerMap.values
                .singleOrNull { it.userId != attackerUserId && it.characterClass.isConscious }
                ?.userId

        // TODO(SS-234): For "Choose your target avatar" if this is null, then show it
        fun topPlayerFor(targetUserId: String?): PlayerState.Selected? = if (isTwoPlayerMatch) {
            playerMap.values.firstOrNull { it.userId != userPlayerId }
        } else {
            playerMap[targetUserId]
        }

        fun playersInTheStandsFor(targetUserId: String?): List<PlayerState.Selected> = playerMap.filterKeys { it != attackerUserId && it != targetUserId }.values.toList()

        fun canTurnOwnerTargetAnOpponent(isAnAnimationActive: Boolean) = !isAnAnimationActive &&
            isItThisUsersTurn &&
            !isMatchOver &&
            lockedTargetUserId.isNullOrEmpty()
    }

    @Immutable
    data class Finished(
        val players: List<PlayerState.Selected>,
        val recap: List<FinishedPlayerRecap>,
    ) : ArenaMatchState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
        val canStartNextMatch = players.size >= MINIMUM_PLAYERS_FOR_NEXT_MATCH
    }
}

internal fun Map<String, MatchParticipant>.toFinishedRecap(): List<FinishedPlayerRecap> {
    val playerCount = size

    return values
        .sortedBy { it.status.finishedPlace(playerCount = playerCount) }
        .map { participant ->
            val place = participant.status.finishedPlace(playerCount = playerCount)
            FinishedPlayerRecap(
                userId = participant.userId,
                displayName = participant.displayName,
                classType = participant.classType,
                outcome =
                if (place == WINNER_PLACE) {
                    FinishedOutcome.Winner
                } else {
                    FinishedOutcome.Placement(place = place)
                },
            )
        }
}

private fun ParticipantStatus.finishedPlace(playerCount: Int): Int = when (this) {
    is ParticipantStatus.Conscious -> WINNER_PLACE
    is ParticipantStatus.KnockedOut -> playerCount - knockoutOrder + 1
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

private fun CombatActionState.charmTargetableUserIds(): Set<String> {
    val abilityRoll = this as? CombatActionState.AbilityRoll ?: return emptySet()
    val charm = abilityRoll.action as? AbilityActionState.Passive.Persuade.Charm ?: return emptySet()

    return charm.targetableUserIds
}

internal fun CombatActionState.teleportAction(): AbilityActionState.Passive.Teleport? {
    val abilityRoll = this as? CombatActionState.AbilityRoll ?: return null

    return abilityRoll.action as? AbilityActionState.Passive.Teleport
}

private fun CombatActionState.teleportRollOffKey(): Int? = teleportAction()?.rollOffAttempt

private fun ArenaCombatControl.isImpersonate(): Boolean = this is ArenaCombatControl.ActivatedAbility &&
    action is AbilityActionState.Activated.Impersonate
