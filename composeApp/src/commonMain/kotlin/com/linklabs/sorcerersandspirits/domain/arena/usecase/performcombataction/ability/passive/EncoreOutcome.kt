package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class EncoreOutcome(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()
        val triggered = roll in 5..6

        combatMutator.addMessage(
            message =
                OutgoingArenaMessage.Encore(
                    bardUserId = combatMutator.attackerUserId,
                    triggered = triggered,
                )
        )

        return if (triggered) {
            // Attack again. Return Idle so attacker can choose action.
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.Idle,
            )
        } else {
            combatMutator.resolveTurnEnd()
        }
    }
}
