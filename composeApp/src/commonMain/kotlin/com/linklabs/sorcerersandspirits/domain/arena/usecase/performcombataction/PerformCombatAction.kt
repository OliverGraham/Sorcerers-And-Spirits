package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.AllInActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.ImpersonateActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.PrepareAttackActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin.LuckyCoinHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport.HandleTeleport
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class PerformCombatAction(private val arguments: CombatActionArguments) {

    private val combatMutator =
        CombatMutator(
            updatedPlayerMap = arguments.playerMap.toMutableMap(),
            attackerUserId = arguments.attacker.userId,
            defenderUserId = arguments.defender.userId,
            roundNumber = arguments.roundNumber,
            originalTurnUserId = arguments.originalTurnUserId,
        )

    operator fun invoke(currentAction: CurrentCombatAction): RollOutcome {
        // TODO: Have everything set the state in combatMutator
        resolveAction(action = currentAction)
        return combatMutator.getOutcome()
    }

    private fun resolveAction(action: CurrentCombatAction) =
        when (action) {
            is CurrentCombatAction.NormalAttack ->
                NormalAttackRollHandler.handleNormalRoll(
                    attacker = arguments.attacker.characterClass,
                    defender = arguments.defender.characterClass,
                    currentAction = action,
                    combatMutator = combatMutator,
                )

            is CurrentCombatAction.AbilityRoll -> action.purpose.handleAbilityRoll()
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

            is AbilityRollPurpose.Passive.LuckyCoin -> handleLuckyCoin()

            is AbilityRollPurpose.Passive.Riposte ->
                RiposteHandler(combatMutator = combatMutator, attackerRollTotal = attackerRollTotal)
                    .rollRiposte()

            is AbilityRollPurpose.Passive.Persuade -> handlePersuade()

            is AbilityRollPurpose.Passive.Encore ->
                EncoreHandler(combatMutator = combatMutator).rollEncore()

            is AbilityRollPurpose.Passive.MirrorImage ->
                MirrorImageHandler(combatMutator = combatMutator).temptFate(mirrorImage = this)
        }
    }

    private fun AbilityRollPurpose.Passive.LuckyCoin.handleLuckyCoin() {
        when (this) {
            is AbilityRollPurpose.Passive.LuckyCoin.Flip ->
                LuckyCoinHandler(
                        combatMutator = combatMutator,
                        attackerRollTotal = this.attackerRollTotal,
                    )
                    .flipLuckyCoin()

            is AbilityRollPurpose.Passive.LuckyCoin.HighRoller ->
                LuckyCoinHandler(
                        combatMutator = combatMutator,
                        attackerRollTotal = this.attackerRollTotal,
                    )
                    .handleHighRoller()
        }
    }

    private fun AbilityRollPurpose.Passive.Persuade.handlePersuade() {
        when (this) {
            is AbilityRollPurpose.Passive.Persuade.StrumLute ->
                PersuadeHandler(combatMutator = combatMutator).strumLute()

            is AbilityRollPurpose.Passive.Persuade.Charm ->
                PersuadeHandler(combatMutator = combatMutator).charm(charm = this)
        }
    }

    private fun AbilityRollPurpose.Passive.HolyShield.handleHolyShield() {
        when (this) {
            is AbilityRollPurpose.Passive.HolyShield.Prayer ->
                HolyShieldHandler(combatMutator = combatMutator).attemptPrayer()

            is AbilityRollPurpose.Passive.HolyShield.Wrath ->
                HolyShieldHandler(combatMutator = combatMutator).wrathRoll()
        }
    }
}
