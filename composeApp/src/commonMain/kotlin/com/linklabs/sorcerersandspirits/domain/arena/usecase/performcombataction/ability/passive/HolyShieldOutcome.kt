package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class HolyShieldOutcome(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val damage = roll1D6()

        combatMutations.updateAttackerClass { playerClass ->
            playerClass.copy(hitPoints = playerClass.hitPoints - damage)
        }

        combatMutations.addMessage(
            message =
                OutgoingMessage.HolyShield(
                    paladinUserId = combatMutations.defenderUserId,
                    survived = true,
                    retaliationDamage = damage,
                )
        )

        return combatMutations.resolveTurnEnd()
    }
}
