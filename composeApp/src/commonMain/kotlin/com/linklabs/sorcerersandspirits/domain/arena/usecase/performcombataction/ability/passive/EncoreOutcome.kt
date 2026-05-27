package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class EncoreOutcome(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()
        val triggered = roll in 5..6

        combatMutations.addMessage(
            message =
                OutgoingMessage.Encore(
                    bardUserId = combatMutations.attackerUserId,
                    triggered = triggered,
                )
        )

        return if (triggered) {
            // Attack again. Return Idle so attacker can choose action.
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.attackerUserId,
                nextStep = NextCombatAction.Idle,
            )
        } else {
            combatMutations.resolveTurnEnd()
        }
    }
}
