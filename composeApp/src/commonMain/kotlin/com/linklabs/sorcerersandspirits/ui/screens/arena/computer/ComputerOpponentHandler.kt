package com.linklabs.sorcerersandspirits.ui.screens.arena.computer

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.isMinionAttackInProgress
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isUserTheRoller
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toActivatedControls
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MIN_THINKING_DELAY_MILLIS = 1600L
private const val MAX_THINKING_DELAY_MILLIS = 3000L

/**
 * Drives the computer opponents in a match. A computer player has no client of its own, so the
 * challenging user's client owns this handler and plays the bots' turns through it.
 */
internal class ComputerOpponentHandler(
    private val arenaId: String,
    private val computerOpponentIds: Set<String>,
    private val coroutineScope: CoroutineScope,
    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase,
) {

    private var actionJob: Job? = null
    private var scheduledActionSignature: String? = null

    fun onArenaState(
        arenaMatchState: ArenaMatchState,
        characterClasses: List<CharacterClassState>,
    ) {
        if (computerOpponentIds.isEmpty()) return

        val pendingComputerId = arenaMatchState.pendingComputerActorId()
        if (pendingComputerId == null) {
            scheduledActionSignature = null
            actionJob?.cancel()
            return
        }

        val signature = arenaMatchState.computerActionSignature(actorId = pendingComputerId)
        if (signature == scheduledActionSignature) return

        scheduledActionSignature = signature
        actionJob?.cancel()
        actionJob =
            coroutineScope.launch {
                delay(timeMillis = thinkingDelayMillis())
                performAction(
                    arenaMatchState = arenaMatchState,
                    computerId = pendingComputerId,
                    characterClasses = characterClasses,
                )
            }
    }

    suspend fun readyUpForNextMatch() {
        computerOpponentIds.forEach { computerId ->
            stayForAnotherMatchUseCase(arenaId = arenaId, playerUserId = computerId)
        }
    }

    fun cancel() {
        actionJob?.cancel()
        scheduledActionSignature = null
    }

    private suspend fun performAction(
        arenaMatchState: ArenaMatchState,
        computerId: String,
        characterClasses: List<CharacterClassState>,
    ) {
        when (arenaMatchState) {
            is ArenaMatchState.ChoosingClasses ->
                chooseClass(
                    arenaMatchState = arenaMatchState,
                    computerId = computerId,
                    characterClasses = characterClasses,
                )

            is ArenaMatchState.Combat ->
                resolveCombat(arenaMatchState = arenaMatchState, computerId = computerId)

            is ArenaMatchState.WaitingForAllPlayers,
            is ArenaMatchState.Finished,
            -> Unit
        }
    }

    private suspend fun chooseClass(
        arenaMatchState: ArenaMatchState.ChoosingClasses,
        computerId: String,
        characterClasses: List<CharacterClassState>,
    ) {
        if (arenaMatchState.activePlayerUserId != computerId) return

        val availableClasses =
            characterClasses.filterNot { it.type in arenaMatchState.unavailableClasses }
        val chosenClass = availableClasses.randomOrNull() ?: return
        val computerPlayer = arenaMatchState.playerMap.getValue(key = computerId)

        val selectedArenaPlayer =
            ArenaPlayer.Selected(
                userId = computerPlayer.userId,
                displayName = computerPlayer.displayName,
                characterClass = chosenClass.toDomain(),
            )

        chooseCharacterClassUseCase(
            arenaId = arenaId,
            selectedArenaPlayer = selectedArenaPlayer,
            allPlayers =
            arenaMatchState.otherPlayers.values.map { it.toDomain() } + selectedArenaPlayer,
        )
    }

    private suspend fun resolveCombat(arenaMatchState: ArenaMatchState.Combat, computerId: String) {
        if (arenaMatchState.isMatchOver) return
        if (computerId !in arenaMatchState.combatActionState.activeUserIds) return

        val currentAction =
            when (val actionState = arenaMatchState.combatActionState) {
                is CombatActionState.Idle ->
                    chooseTurnAction(arenaMatchState = arenaMatchState, computerId = computerId)

                is CombatActionState.NormalAttack ->
                    AttackArguments(
                        targetedOpponentUserId = actionState.defenderUserId,
                        userPlayerId = computerId,
                        combatActionState = actionState,
                    )
                        .toDomain()

                is CombatActionState.AbilityRoll ->
                    UseAbilityArguments(
                        abilityActionState = actionState.action,
                        targetedOpponentUserId = randomCharmTarget(action = actionState.action),
                        userPlayerId = computerId,
                        combatActionState = actionState,
                    )
                        .toDomain()
            } ?: return

        arenaMatchState.dispatchCombatAction(currentAction = currentAction)
    }

    private fun chooseTurnAction(
        arenaMatchState: ArenaMatchState.Combat,
        computerId: String,
    ): CurrentCombatAction? {
        val target =
            randomLivingOpponent(arenaMatchState = arenaMatchState, excludingPlayerId = computerId)
                ?: return null

        val attackAction =
            AttackArguments(
                targetedOpponentUserId = target.userId,
                userPlayerId = computerId,
                combatActionState = arenaMatchState.combatActionState,
            ).toDomain()

        val abilityActions =
            arenaMatchState
                .availableActivatedAbilities(computerId = computerId)
                .mapNotNull { control ->
                    UseAbilityArguments(
                        abilityActionState =
                        control.action.withImpersonationResolved(target = target),
                        targetedOpponentUserId =
                        if (control.requiresTarget) target.userId else "",
                        userPlayerId = computerId,
                        combatActionState = arenaMatchState.combatActionState,
                    ).toDomain()
                }

        return (listOfNotNull(attackAction) + abilityActions).randomOrNull()
    }

    private fun ArenaMatchState.Combat.availableActivatedAbilities(
        computerId: String,
    ): List<ArenaCombatControl.ActivatedAbility> {
        val offensiveAbility = playerMap.getValue(key = computerId).characterClass.offensiveAbility
        if (offensiveAbility.isMinionAttackInProgress) return emptyList()

        return offensiveAbility.toActivatedControls().filter { it.enabled }
    }

    private fun AbilityActionState.Activated.withImpersonationResolved(
        target: PlayerState.Selected,
    ): AbilityActionState.Activated {
        if (this !is AbilityActionState.Activated.Impersonate) return this

        return AbilityActionState.Activated.Impersonate(
            impersonateAbility =
            OffensiveAbility.Impersonate(
                impersonatedAbility = target.characterClass.offensiveAbility.toDomain(),
            ),
        )
    }

    private suspend fun ArenaMatchState.Combat.dispatchCombatAction(
        currentAction: CurrentCombatAction,
    ) {
        val combatActionArguments =
            CombatActionArguments(
                arenaId = arenaId,
                playerMap = playerMap.mapValues { it.value.toDomain() },
                roundNumber = roundNumber,
                doublesBonus = diceRollState?.doublesBonus?.toDomain(),
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
    }

    private fun randomLivingOpponent(
        arenaMatchState: ArenaMatchState.Combat,
        excludingPlayerId: String,
    ): PlayerState.Selected? = arenaMatchState.playerMap.values
        .filter { player ->
            player.userId != excludingPlayerId && player.characterClass.isConscious
        }
        .randomOrNull()

    private fun randomCharmTarget(action: AbilityActionState): String = (action as? AbilityActionState.Passive.Persuade.Charm)
        ?.targetableUserIds
        ?.randomOrNull()
        .orEmpty()

    private fun ArenaMatchState.pendingComputerActorId(): String? = when (this) {
        is ArenaMatchState.ChoosingClasses ->
            activePlayerUserId.takeIf { it in computerOpponentIds }

        is ArenaMatchState.Combat ->
            combatActionState.activeUserIds.firstOrNull { userId ->
                val isComputer = userId in computerOpponentIds
                val isAwaitingRoll =
                    combatActionState !is CombatActionState.AbilityRoll ||
                        combatActionState.action.isUserTheRoller(userPlayerId = userId)
                isComputer && isAwaitingRoll
            }

        is ArenaMatchState.WaitingForAllPlayers,
        is ArenaMatchState.Finished,
        -> null
    }

    private fun ArenaMatchState.computerActionSignature(actorId: String): String = when (this) {
        is ArenaMatchState.ChoosingClasses -> "choosing:$actorId"

        is ArenaMatchState.Combat -> "combat:$roundNumber:$actorId:$combatActionState"

        is ArenaMatchState.WaitingForAllPlayers,
        is ArenaMatchState.Finished,
        -> "none"
    }

    private fun thinkingDelayMillis(): Long = Random.nextLong(from = MIN_THINKING_DELAY_MILLIS, until = MAX_THINKING_DELAY_MILLIS)
}
