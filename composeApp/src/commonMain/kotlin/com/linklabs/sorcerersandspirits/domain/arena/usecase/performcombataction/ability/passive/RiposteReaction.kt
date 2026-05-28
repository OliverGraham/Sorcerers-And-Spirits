package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class RiposteReaction(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val triggered = total in 6..8

        combatMutations.addMessage(
            message =
                OutgoingMessage.Riposte(
                    duelistUserId = combatMutations.defenderUserId,
                    attackerUserId = combatMutations.attackerUserId,
                    triggered = triggered,
                )
        )

        return if (triggered) {
            // Flip attack: Defender now attacks Attacker
            // We need to swap attacker and defender roles for the next step.
            // But wait, the combat state machine expects current attacker/defender.
            // If Riposte triggers, the original defender becomes the attacker.
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.defenderUserId,
                nextStep = NextCombatAction.NormalAttackRoll.ArmorClassRoll,
            )
        } else {
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
            )
        }
    }
}
