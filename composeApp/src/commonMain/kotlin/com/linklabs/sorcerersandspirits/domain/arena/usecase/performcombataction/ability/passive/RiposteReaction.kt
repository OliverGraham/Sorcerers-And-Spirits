package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class RiposteReaction(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val triggered = total in 6..8

        combatMutator.addMessage(
            message =
                OutgoingArenaMessage.Riposte.EnGarde(firstDie = firstDie, secondDie = secondDie)
        )

        return if (triggered) {
            // Flip attack: Defender now attacks Attacker
            // We need to swap attacker and defender roles for the next step.
            // But wait, the combat state machine expects current attacker/defender.
            // If Riposte triggers, the original defender becomes the attacker.
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.defenderUserId,
                nextStep = NextCombatAction.NormalAttackRoll.ArmorClass,
            )
        } else {
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.Health,
            )
        }
    }
}
