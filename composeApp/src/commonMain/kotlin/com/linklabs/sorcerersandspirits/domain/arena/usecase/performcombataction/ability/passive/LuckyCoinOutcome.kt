package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class LuckyCoinOutcome(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val damage = roll1D6()

        combatMutations.updateAttackerClass { playerClass ->
            playerClass.copy(hitPoints = playerClass.hitPoints - damage)
        }

        combatMutations.addMessage(
            message =
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = combatMutations.defenderUserId,
                    attackerUserId = combatMutations.attackerUserId,
                    armorBonus = 6, // We know it was a 6 to get here
                    reflectDamage = damage,
                )
        )

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutations.attackerUserId,
            nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
        )
    }
}
