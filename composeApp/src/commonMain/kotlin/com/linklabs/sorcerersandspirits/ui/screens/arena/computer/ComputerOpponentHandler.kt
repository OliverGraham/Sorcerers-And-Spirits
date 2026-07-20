package com.linklabs.sorcerersandspirits.ui.screens.arena.computer

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.isMinionAttackInProgress
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    fun onArenaState(arenaState: ArenaState, characterClasses: List<CharacterClassState>) {
        if (computerOpponentIds.isEmpty()) return

        val pendingComputerId = arenaState.pendingComputerActorId()
        if (pendingComputerId == null) {
            scheduledActionSignature = null
            actionJob?.cancel()
            return
        }

        val signature = arenaState.computerActionSignature(actorId = pendingComputerId)
        if (signature == scheduledActionSignature) return

        scheduledActionSignature = signature
        actionJob?.cancel()
        actionJob =
            coroutineScope.launch {
                delay(timeMillis = thinkingDelayMillis())
                performAction(
                    arenaState = arenaState,
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
        arenaState: ArenaState,
        computerId: String,
        characterClasses: List<CharacterClassState>,
    ) {
        when (arenaState) {
            is ArenaState.ChoosingClasses ->
                chooseClass(
                    arenaState = arenaState,
                    computerId = computerId,
                    characterClasses = characterClasses,
                )

            is ArenaState.Combat -> resolveCombat(arenaState = arenaState, computerId = computerId)

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> Unit
        }
    }

    private suspend fun chooseClass(
        arenaState: ArenaState.ChoosingClasses,
        computerId: String,
        characterClasses: List<CharacterClassState>,
    ) {
        if (arenaState.activePlayerUserId != computerId) return

        val availableClasses =
            characterClasses.filterNot { it.type in arenaState.unavailableClasses }
        val chosenClass = availableClasses.randomOrNull() ?: return
        val computerPlayer = arenaState.playerMap.getValue(key = computerId)

        val selectedArenaPlayer =
            ArenaPlayer.Selected(
                userId = computerPlayer.userId,
                displayName = computerPlayer.displayName,
                characterClass = chosenClass.toDomain(),
            )

        chooseCharacterClassUseCase(
            arenaId = arenaId,
            selectedArenaPlayer = selectedArenaPlayer,
            allPlayers = arenaState.otherPlayers.values.map { it.toDomain() } + selectedArenaPlayer,
        )
    }

    private suspend fun resolveCombat(arenaState: ArenaState.Combat, computerId: String) {
        if (arenaState.isMatchOver) return
        if (computerId !in arenaState.combatActionState.activeUserIds) return

        val currentAction =
            when (val actionState = arenaState.combatActionState) {
                is CombatActionState.Idle ->
                    chooseTurnAction(arenaState = arenaState, computerId = computerId)

                is CombatActionState.NormalAttack ->
                    AttackArguments(
                            targetedOpponentUserId = actionState.defenderUserId,
                            userPlayerId = computerId,
                        )
                        .toDomain(combatActionState = actionState)

                is CombatActionState.AbilityRoll ->
                    UseAbilityArguments(
                            abilityActionState = actionState.action,
                            targetedOpponentUserId = randomCharmTarget(action = actionState.action),
                            userPlayerId = computerId,
                        )
                        .toDomain(combatActionState = actionState)
            } ?: return

        arenaState.dispatchCombatAction(currentAction = currentAction)
    }

    private fun chooseTurnAction(
        arenaState: ArenaState.Combat,
        computerId: String,
    ): CurrentCombatAction? {
        val computerPlayer = arenaState.playerMap.getValue(key = computerId)
        val favorableAbility = computerPlayer.characterClass.favorableOffensiveAbility()

        if (favorableAbility != null) {
            return UseAbilityArguments(
                    abilityActionState = favorableAbility,
                    targetedOpponentUserId = "",
                    userPlayerId = computerId,
                )
                .toDomain(combatActionState = arenaState.combatActionState)
        }

        val target =
            weakestLivingOpponent(arenaState = arenaState, excludingPlayerId = computerId)
                ?: return null

        return AttackArguments(targetedOpponentUserId = target.userId, userPlayerId = computerId)
            .toDomain(combatActionState = arenaState.combatActionState)
    }

    private suspend fun ArenaState.Combat.dispatchCombatAction(currentAction: CurrentCombatAction) {
        val combatActionArguments =
            CombatActionArguments(
                arenaId = arenaId,
                playerMap = playerMap.mapValues { it.value.toDomain() },
                roundNumber = roundNumber,
                doublesBonus = diceRollState?.doublesBonus?.toDomain(),
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
    }

    private fun CharacterClassState.favorableOffensiveAbility(): AbilityActionState.Activated? {
        if (offensiveAbility.isMinionAttackInProgress) return null

        return when (val ability = offensiveAbility) {
            is OffensiveAbilityState.SummonMinion -> {
                val hasNoMinions = ability.skeletonCount == 0 && ability.golemCount == 0
                val isBelowMaxHp = hitPoints < maxHitPoints

                if (hasNoMinions || isBelowMaxHp) {
                    AbilityActionState.Activated.SummonMinion(
                        summonMinionAbility =
                            OffensiveAbility.SummonMinion(
                                skeletonCount = ability.skeletonCount,
                                golemCount = ability.golemCount,
                            )
                    )
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun weakestLivingOpponent(
        arenaState: ArenaState.Combat,
        excludingPlayerId: String,
    ): PlayerState.Selected? {
        val opponents =
            arenaState.playerMap.values.filter { player ->
                player.userId != excludingPlayerId && player.characterClass.hitPoints > 0
            }
        val lowestHitPoints = opponents.minOfOrNull { it.characterClass.hitPoints } ?: return null

        return opponents
            .filter { player -> player.characterClass.hitPoints == lowestHitPoints }
            .randomOrNull()
    }

    private fun randomCharmTarget(action: AbilityActionState): String =
        (action as? AbilityActionState.Passive.Persuade.Charm)
            ?.targetableUserIds
            ?.randomOrNull()
            .orEmpty()

    private fun ArenaState.pendingComputerActorId(): String? =
        when (this) {
            is ArenaState.ChoosingClasses -> activePlayerUserId.takeIf { it in computerOpponentIds }

            is ArenaState.Combat ->
                combatActionState.activeUserIds.firstOrNull { it in computerOpponentIds }

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> null
        }

    private fun ArenaState.computerActionSignature(actorId: String): String =
        when (this) {
            is ArenaState.ChoosingClasses -> "choosing:$actorId"

            is ArenaState.Combat -> "combat:$roundNumber:$actorId:$combatActionState"

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> "none"
        }

    private fun thinkingDelayMillis(): Long =
        Random.nextLong(from = MIN_THINKING_DELAY_MILLIS, until = MAX_THINKING_DELAY_MILLIS)
}
