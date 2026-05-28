package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.PersuadeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

// TODO: FIX
internal class PersuadeReaction(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                5 -> PersuadeOutcome.Stopped
                6 -> PersuadeOutcome.Redirected
                else -> PersuadeOutcome.Failed
            }

        combatMutator.addMessage(message = OutgoingArenaMessage.Persuade.Trigger)

        return when (outcome) {
            PersuadeOutcome.Stopped -> combatMutator.resolveTurnEnd()

            PersuadeOutcome.Redirected -> {
                // Attacker needs to pick new target. We return Idle so they can choose.
                CombatActionUpdates(
                    nextActivePlayerUserId = combatMutator.attackerUserId,
                    nextStep = NextCombatAction.Idle,
                )
            }

            PersuadeOutcome.Failed -> {
                CombatActionUpdates(
                    nextActivePlayerUserId = combatMutator.attackerUserId,
                    nextStep = NextCombatAction.NormalAttackRoll.Health,
                )
            }
        }
    }
}
