package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
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
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class RollOutcome(
    val messages: List<OutgoingMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextActivePlayerUserId: String,
    val nextStep: NextCombatAction,
)

data class CombatActionUpdates(val nextActivePlayerUserId: String, val nextStep: NextCombatAction)

class PerformCombatAction(private val arguments: CombatActionArguments) {

    private val combatMutator =
        CombatMutator(
            updatedPlayerMap = arguments.playerMap.toMutableMap(),
            attackerUserId = arguments.attacker.userId,
            defenderUserId = arguments.defender.userId,
            roundNumber = arguments.roundNumber,
        )

    operator fun invoke(currentAction: CurrentCombatAction): RollOutcome {
        val updates = resolveAction(action = currentAction)
        val updatedProperties = combatMutator.getUpdatedProperties()

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
                combatMutator = combatMutator,
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

    private fun AbilityRollPurpose.Passive.Reaction.handleReaction(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Passive.Reaction.HolyShield ->
                HolyShieldReaction(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Reaction.LuckyCoin ->
                LuckyCoinReaction(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Reaction.MirrorImage ->
                MirrorImageReaction(
                        combatMutator = combatMutator,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Reaction.Persuade ->
                PersuadeReaction(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Reaction.Riposte ->
                RiposteReaction(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Reaction.Teleport ->
                TeleportReaction(combatMutator = combatMutator).invoke()
        }

    private fun AbilityRollPurpose.Passive.Outcome.handleOutcome(): CombatActionUpdates =
        when (this) {
            is AbilityRollPurpose.Passive.Outcome.Encore ->
                EncoreOutcome(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Outcome.HolyShield ->
                when (this) {
                    is AbilityRollPurpose.Passive.Outcome.HolyShield.Survival ->
                        HolyShieldOutcome(combatMutator = combatMutator).invoke()

                    is AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect ->
                        HolyShieldOutcome(combatMutator = combatMutator).invoke()
                }

            is AbilityRollPurpose.Passive.Outcome.LuckyCoin ->
                LuckyCoinOutcome(combatMutator = combatMutator).invoke()

            is AbilityRollPurpose.Passive.Outcome.MirrorImagePass ->
                MirrorImagePassOutcome(
                        combatMutator = combatMutator,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll ->
                MirrorImageRerollOutcome(
                        combatMutator = combatMutator,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        rerollFirstDie = rerollFirstDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
                    .invoke()

            is AbilityRollPurpose.Passive.Outcome.Teleport ->
                TeleportOutcome(combatMutator = combatMutator, defenderRoll = defenderRoll).invoke()
        }
}
