package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class HolyShieldOutcome(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val damage = roll1D6()

        combatMutator.updateAttackerClass { playerClass ->
            playerClass.copy(hitPoints = playerClass.hitPoints - damage)
        }

        combatMutator.addMessage(
            message =
                OutgoingMessage.HolyShield.Reflect(
                    paladinUserId = combatMutator.defenderUserId,
                    singleDie = damage,
                )
        )

        return combatMutator.resolveTurnEnd()
    }
}
