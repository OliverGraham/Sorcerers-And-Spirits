package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class LuckyCoinOutcome(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val damage = roll1D6()

        combatMutator.updateAttackerClass { playerClass ->
            playerClass.copy(hitPoints = playerClass.hitPoints - damage)
        }

        combatMutator.addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin(
                    gamblerUserId = combatMutator.defenderUserId,
                    attackerUserId = combatMutator.attackerUserId,
                    armorBonus = 6, // We know it was a 6 to get here
                    reflectDamage = damage,
                )
        )

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutator.attackerUserId,
            nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
        )
    }
}
