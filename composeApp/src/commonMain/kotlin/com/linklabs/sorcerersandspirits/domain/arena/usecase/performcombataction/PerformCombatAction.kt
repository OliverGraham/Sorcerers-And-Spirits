package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.AllInActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.ImpersonateActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.PrepareAttackActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.LuckyCoinHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TeleportHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.game.AbilityConfig

internal class PerformCombatAction(
    private val arguments: CombatActionArguments,
    private val diceRoller: DiceRoller,
    private val abilityConfig: AbilityConfig,
) {

    private lateinit var combatMutator: CombatMutator

    operator fun invoke(currentAction: CurrentCombatAction): ActionOutcome {
        combatMutator =
            CombatMutator(
                updatedPlayerMap = arguments.playerMap.toMutableMap(),
                attackerUserId = currentAction.attackTurn.attackerUserId,
                defenderUserId = currentAction.defenderUserId,
                rollingUserId = currentAction.rollingUserId(),
                roundNumber = arguments.roundNumber,
                originalTurnUserId = currentAction.attackTurn.turnOwnerUserId,
                diceRoller = diceRoller,
                abilityConfig = abilityConfig,
            )

        resolveAction(action = currentAction)
        return combatMutator.getOutcome()
    }

    private fun CurrentCombatAction.rollingUserId(): String =
        when (this) {
            is CurrentCombatAction.NormalAttack -> attackTurn.attackerUserId
            is CurrentCombatAction.AbilityRoll -> rollingUserId
        }

    private fun resolveAction(action: CurrentCombatAction) =
        when (action) {
            is CurrentCombatAction.NormalAttack ->
                NormalAttackRollHandler.handleNormalRoll(
                    currentAction = action,
                    combatMutator = combatMutator,
                )

            is CurrentCombatAction.AbilityRoll -> action.purpose.handleAbilityRoll()
        }

    private fun AbilityAction.handleAbilityRoll() =
        when (this) {
            is AbilityAction.Activated -> handleActivated()
            is AbilityAction.Passive -> handlePassive()
        }

    private fun AbilityAction.Activated.handleActivated() =
        when (this) {
            is AbilityAction.Activated.AllIn ->
                AllInActivated(combatMutator = combatMutator, allIn = allInAbility).invoke()

            is AbilityAction.Activated.Impersonate ->
                ImpersonateActivated(
                        combatMutator = combatMutator,
                        impersonateAbility = impersonateAbility,
                    )
                    .invoke()

            is AbilityAction.Activated.PrepareAttack ->
                PrepareAttackActivated(combatMutator = combatMutator).invoke()

            is AbilityAction.Activated.SummonMinion ->
                SummonMinionActivated(
                        combatMutator = combatMutator,
                        summonMinionAbility = summonMinionAbility,
                    )
                    .invoke()
        }

    private fun AbilityAction.Passive.handlePassive() {
        when (this) {
            is AbilityAction.Passive.Teleport ->
                TeleportHandler(combatMutator = combatMutator).handleTeleport(teleport = this)

            is AbilityAction.Passive.HolyShield -> handleHolyShield()

            is AbilityAction.Passive.LuckyCoin -> handleLuckyCoin()

            is AbilityAction.Passive.Riposte ->
                RiposteHandler(combatMutator = combatMutator, attackerRollTotal = attackerRollTotal)
                    .rollRiposte()

            is AbilityAction.Passive.Persuade -> handlePersuade()

            is AbilityAction.Passive.Encore ->
                EncoreHandler(combatMutator = combatMutator).rollEncore()

            is AbilityAction.Passive.MirrorImage ->
                MirrorImageHandler(combatMutator = combatMutator).temptFate(mirrorImage = this)
        }
    }

    private fun AbilityAction.Passive.LuckyCoin.handleLuckyCoin() {
        when (this) {
            is AbilityAction.Passive.LuckyCoin.Flip ->
                LuckyCoinHandler(
                        combatMutator = combatMutator,
                        attackerRollTotal = this.attackerRollTotal,
                    )
                    .flipLuckyCoin()

            is AbilityAction.Passive.LuckyCoin.HighRoller ->
                LuckyCoinHandler(
                        combatMutator = combatMutator,
                        attackerRollTotal = this.attackerRollTotal,
                    )
                    .handleHighRoller()
        }
    }

    private fun AbilityAction.Passive.Persuade.handlePersuade() {
        when (this) {
            is AbilityAction.Passive.Persuade.StrumLute ->
                PersuadeHandler(combatMutator = combatMutator).strumLute()

            is AbilityAction.Passive.Persuade.Charm ->
                PersuadeHandler(combatMutator = combatMutator).charm(charm = this)
        }
    }

    private fun AbilityAction.Passive.HolyShield.handleHolyShield() {
        when (this) {
            is AbilityAction.Passive.HolyShield.Prayer ->
                HolyShieldHandler(combatMutator = combatMutator).attemptPrayer()

            is AbilityAction.Passive.HolyShield.Wrath ->
                HolyShieldHandler(combatMutator = combatMutator).wrathRoll()
        }
    }
}
