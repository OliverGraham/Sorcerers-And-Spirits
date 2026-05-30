package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.AllInActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.ImpersonateActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.PrepareAttackActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield.AttemptHolyShieldPrayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield.HolyShieldWrath
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport.HandleTeleport
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class PerformCombatAction(private val arguments: CombatActionArguments) {

    private val combatMutator =
        CombatMutator(
            updatedPlayerMap = arguments.playerMap.toMutableMap(),
            attackerUserId = arguments.attacker.userId,
            defenderUserId = arguments.defender.userId,
            roundNumber = arguments.roundNumber,
        )

    operator fun invoke(currentAction: CurrentCombatAction): RollOutcome {
        // TODO: Have everything set the state in combatMutator
        resolveAction(action = currentAction)
        return combatMutator.getOutcome()
    }

    private fun resolveAction(action: CurrentCombatAction) =
        when (action) {
            is CurrentCombatAction.NormalAttack -> action.handleNormalRoll()
            is CurrentCombatAction.AbilityRoll -> action.purpose.handleAbilityRoll()
        }

    private fun CurrentCombatAction.NormalAttack.handleNormalRoll() {
        val firstDie = roll1D6()
        val secondDie = roll1D6()

        val normalAttackRoll =
            NormalAttackRoll(
                firstDie = firstDie,
                secondDie = secondDie,
                attacker = arguments.attacker,
                defender = arguments.defender,
            )

        normalAttackRoll.handleRoll(currentAction = this, combatMutator = combatMutator)
    }

    private fun AbilityRollPurpose.handleAbilityRoll() =
        when (this) {
            is AbilityRollPurpose.Activated -> handleActivated()
            is AbilityRollPurpose.Passive -> handlePassive()
        }

    private fun AbilityRollPurpose.Activated.handleActivated() =
        when (this) {
            is AbilityRollPurpose.Activated.AllIn ->
                AllInActivated(combatMutator = combatMutator, allIn = allInAbility).invoke()

            is AbilityRollPurpose.Activated.Impersonate ->
                ImpersonateActivated(
                        combatMutator = combatMutator,
                        impersonateAbility = impersonateAbility,
                    )
                    .invoke()

            is AbilityRollPurpose.Activated.PrepareAttack ->
                PrepareAttackActivated(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Activated.SummonMinion ->
                SummonMinionActivated(
                        combatMutator = combatMutator,
                        summonMinionAbility = summonMinionAbility,
                    )
                    .invoke()
        }

    private fun AbilityRollPurpose.Passive.handlePassive() {
        when (this) {
            is AbilityRollPurpose.Passive.Teleport ->
                HandleTeleport(combatMutator = combatMutator, rollingDieUserId = arguments.userId)

            is AbilityRollPurpose.Passive.HolyShield -> handleHolyShield()

            is AbilityRollPurpose.Passive.Outcome.Encore -> TODO()

            is AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect -> TODO()

            is AbilityRollPurpose.Passive.Outcome.HolyShield.Survival -> TODO()

            is AbilityRollPurpose.Passive.Outcome.LuckyCoin.ArmorClass -> TODO()

            is AbilityRollPurpose.Passive.Outcome.LuckyCoin.Reflect -> TODO()

            is AbilityRollPurpose.Passive.Outcome.MirrorImagePass -> TODO()

            is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll -> TODO()

            is AbilityRollPurpose.Passive.Reaction.HolyShield -> TODO()

            is AbilityRollPurpose.Passive.Reaction.LuckyCoin -> TODO()

            is AbilityRollPurpose.Passive.Reaction.MirrorImage -> TODO()

            is AbilityRollPurpose.Passive.Reaction.Persuade -> TODO()

            is AbilityRollPurpose.Passive.Reaction.Riposte -> TODO()
        }
    }

    /** Moves from one state to another... */
    private fun AbilityRollPurpose.Passive.HolyShield.handleHolyShield() {
        when (this) {
            is AbilityRollPurpose.Passive.HolyShield.Trigger ->
                AttemptHolyShieldPrayer(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.HolyShield.Wrath ->
                HolyShieldWrath(combatMutator = combatMutator).invoke()
        }
    }
}
