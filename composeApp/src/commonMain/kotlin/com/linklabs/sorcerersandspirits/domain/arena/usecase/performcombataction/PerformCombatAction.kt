package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.AllInActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.ImpersonateActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.PrepareAttackActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldReaction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.LuckyCoinOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.LuckyCoinReaction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImagePassOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageReaction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageRerollOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeReaction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteReaction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TeleportOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TeleportReaction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollOutcome
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

data class CombatActionUpdates(val nextActivePlayerUserId: String, val nextStep: NextCombatAction)

class PerformCombatAction(private val arguments: CombatActionArguments) {
    private val combatMutations =
        CombatMutations(
            updatedPlayerMap = arguments.playerMap.toMutableMap(),
            attackerUserId = arguments.attacker.userId,
            defenderUserId = arguments.defender.userId,
            roundNumber = arguments.roundNumber,
        )

    operator fun invoke(currentAction: CurrentCombatAction): RollOutcome {
        val updates = resolveAction(action = currentAction)
        val updatedProperties = combatMutations.getUpdatedProperties()

        return RollOutcome(
            messages = updatedProperties.messages,
            updatedPlayerMap = updatedProperties.updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    private fun resolveAction(action: CurrentCombatAction) =
        when (action) {
            is CurrentCombatAction.NormalAttackRoll -> action.handleNormalRoll()
            is CurrentCombatAction.AbilityRoll -> action.purpose.handleAbilityRoll()
        }

    private fun CurrentCombatAction.NormalAttackRoll.handleNormalRoll(): CombatActionUpdates {
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

        return normalAttackRoll.handleRoll(currentAction = this)
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
            is AbilityRollPurpose.Passive.Reaction.HolyShield ->
                HolyShieldReaction(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Reaction.LuckyCoin ->
                LuckyCoinReaction(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Reaction.MirrorImage ->
                MirrorImageReaction(
                        combatMutations = combatMutations,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Reaction.Persuade ->
                PersuadeReaction(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Reaction.Riposte ->
                RiposteReaction(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Reaction.Teleport ->
                TeleportReaction(combatMutations).invoke()
        }

    private fun AbilityRollPurpose.Passive.Outcome.handleOutcome(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Passive.Outcome.Encore -> EncoreOutcome(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Outcome.HolyShield ->
                HolyShieldOutcome(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Outcome.LuckyCoin ->
                LuckyCoinOutcome(combatMutations).invoke()

            is AbilityRollPurpose.Passive.Outcome.MirrorImagePass ->
                MirrorImagePassOutcome(
                        combatMutations = combatMutations,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll ->
                MirrorImageRerollOutcome(
                        combatMutations = combatMutations,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        rerollFirstDie = rerollFirstDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Outcome.Teleport ->
                TeleportOutcome(combatMutations = combatMutations, defenderRoll = defenderRoll)
                    .invoke()
        }
}
