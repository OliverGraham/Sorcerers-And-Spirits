package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.AllInActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.ImpersonateActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.PrepareAttackActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollOutcome
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

data class CombatActionUpdates(val nextActivePlayerUserId: String, val nextStep: CombatStep)

class PerformCombatAction(
    private val arguments: CombatActionArguments,
    private val currentStep: CombatStep,
) {
    private val combatMutations =
        CombatMutations(
            updatedPlayerMap = arguments.playerMap.toMutableMap(),
            attackerUserId = arguments.attacker.userId,
            defenderUserId = arguments.defender.userId,
            roundNumber = arguments.roundNumber,
        )

    operator fun invoke(): RollOutcome {
        val updates = resolveStep(step = currentStep)
        val updatedProperties = combatMutations.getUpdatedProperties()

        return RollOutcome(
            messages = updatedProperties.messages,
            updatedPlayerMap = updatedProperties.updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    private fun resolveStep(step: CombatStep) =
        when (step) {
            is CombatStep.NormalAttackRoll -> step.handleNormalRoll()
            is CombatStep.AbilityRoll -> step.purpose.handleAbilityRoll()
            is CombatStep.TakeAction -> step.
        }

    private fun CombatStep.NormalAttackRoll.handleNormalRoll(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()

        val normalAttackRoll =
            NormalAttackRoll(
                combatMutations = combatMutations,
                firstDie = firstDie,
                secondDie = secondDie,
                attacker = arguments.attacker,
                defender = arguments.defender,
            )

        return normalAttackRoll.handleRoll(combatStep = this)
    }

    private fun AbilityRollPurpose.handleAbilityRoll(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Activated -> handleActivated()

            is AbilityRollPurpose.Passive ->
                when (this) {
                    is AbilityRollPurpose.Passive.Reaction -> handleReaction()
                    is AbilityRollPurpose.Passive.Outcome -> handleOutcome()
                }
        }

    private fun AbilityRollPurpose.Activated.handleActivated(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Activated.AllIn ->
                AllInActivated(combatMutations = combatMutations, allIn = allInAbility).invoke()

            is AbilityRollPurpose.Activated.Impersonate ->
                ImpersonateActivated(
                        combatMutations = combatMutations,
                        impersonateAbility = impersonateAbility,
                    )
                    .invoke()

            is AbilityRollPurpose.Activated.PrepareAttack ->
                PrepareAttackActivated(combatMutations = combatMutations).invoke()

            is AbilityRollPurpose.Activated.SummonMinion ->
                SummonMinionActivated(
                        combatMutations = combatMutations,
                        summonMinionAbility = summonMinionAbility,
                    )
                    .invoke()
        }

    private fun AbilityRollPurpose.Passive.Reaction.handleReaction(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Passive.Reaction.HolyShield -> TODO()
            is AbilityRollPurpose.Passive.Reaction.LuckyCoin -> TODO()
            is AbilityRollPurpose.Passive.Reaction.MirrorImage -> TODO()
            is AbilityRollPurpose.Passive.Reaction.Persuade -> TODO()
            is AbilityRollPurpose.Passive.Reaction.Riposte -> TODO()
            is AbilityRollPurpose.Passive.Reaction.Teleport -> TODO()
        }

    private fun AbilityRollPurpose.Passive.Outcome.handleOutcome(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Passive.Outcome.Encore -> TODO()
            is AbilityRollPurpose.Passive.Outcome.HolyShield -> TODO()
            is AbilityRollPurpose.Passive.Outcome.LuckyCoin -> TODO()
            is AbilityRollPurpose.Passive.Outcome.MirrorImagePass -> TODO()
            is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll -> TODO()
            is AbilityRollPurpose.Passive.Outcome.Teleport -> TODO()
        }
}
