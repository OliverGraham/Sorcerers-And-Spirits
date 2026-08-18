package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain

internal class CombatHandler(
    private val arenaId: String,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
) {

    fun targetOpponent(screenState: ArenaScreenState, opponentUserId: String): ArenaScreenState {
        val arenaState = screenState.arenaMatchState.asCombatStateOrNull() ?: return screenState

        return screenState.copy(
            arenaMatchState = arenaState.copy(selectedOpponentUserId = opponentUserId)
        )
    }

    // TODO: Need to refactor ArenaCombatControl, CombatButtons and AttackArguments and
    // UseAbilityArguments
    fun updateLockedTargetUserId(
        screenState: ArenaScreenState,
        lockedTargetUserId: String,
    ): ArenaScreenState {
        val arenaCombatState =
            screenState.arenaMatchState.asCombatStateOrNull() ?: return screenState

        return updateLockedTargetUserId(
            screenState = screenState,
            arenaCombatState = arenaCombatState,
            lockedTargetUserId = lockedTargetUserId,
        )
    }

    suspend fun onNormalAttack(
        attackArguments: AttackArguments,
        arenaMatchState: ArenaMatchState?,
    ) {
        val arenaCombatState = arenaMatchState?.asCombatStateOrNull() ?: return
        val currentCombatAction = attackArguments.toDomain() ?: return

        arenaCombatState.performCombatAction(currentAction = currentCombatAction)
    }

    suspend fun onUseAbility(
        useAbilityArguments: UseAbilityArguments,
        arenaMatchState: ArenaMatchState?,
    ) {
        val arenaCombatState = arenaMatchState?.asCombatStateOrNull() ?: return
        val argumentsWithImpersonateUpdatedIfApplicable =
            useAbilityArguments.withImpersonatedTarget(arenaMatchState = arenaCombatState)

        val currentCombatAction = argumentsWithImpersonateUpdatedIfApplicable.toDomain() ?: return

        arenaCombatState.performCombatAction(currentAction = currentCombatAction)
    }

    fun updateIsActionInFlight(screenState: ArenaScreenState): ArenaScreenState {
        val arenaCombatState =
            screenState.arenaMatchState.asCombatStateOrNull() ?: return screenState

        return screenState.copy(arenaMatchState = arenaCombatState.copy(isActionInFlight = true))
    }

    private fun updateLockedTargetUserId(
        screenState: ArenaScreenState,
        arenaCombatState: ArenaMatchState.Combat,
        lockedTargetUserId: String,
    ): ArenaScreenState =
        screenState.copy(
            arenaMatchState =
                arenaCombatState.copy(
                    lockedTargetUserId = lockedTargetUserId,
                    isActionInFlight = true,
                )
        )

    private suspend fun ArenaMatchState.Combat.performCombatAction(
        currentAction: CurrentCombatAction
    ) {
        // TODO: CombatActionArguments needs a refactor - likely currentAction should be part of it
        val combatActionArguments =
            CombatActionArguments(
                arenaId = arenaId,
                playerMap = playerMap.mapValues { it.value.toDomain() },
                roundNumber = roundNumber,
                doublesBonus = diceRollState?.doublesBonus?.toDomain(),
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
    }

    private fun UseAbilityArguments.withImpersonatedTarget(
        arenaMatchState: ArenaMatchState.Combat
    ): UseAbilityArguments {
        if (abilityActionState !is AbilityActionState.Activated.Impersonate) return this

        val target = arenaMatchState.playerMap[targetedOpponentUserId] ?: return this
        val copiedAbility = target.characterClass.offensiveAbility.toDomain()

        return copy(
            abilityActionState =
                AbilityActionState.Activated.Impersonate(
                    impersonateAbility =
                        OffensiveAbility.Impersonate(impersonatedAbility = copiedAbility)
                )
        )
    }

    private fun ArenaMatchState.asCombatStateOrNull() = this as? ArenaMatchState.Combat
}
