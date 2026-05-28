package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class LuckyCoinReaction(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()

        combatMutations.updateDefenderClass { playerClass ->
            playerClass.copy(armorClass = playerClass.armorClass + roll)
        }

        if (roll == 6) {
            return CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.defenderUserId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.LuckyCoin
                    ),
            )
        }

        combatMutations.addMessage(
            message =
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = combatMutations.defenderUserId,
                    attackerUserId = combatMutations.attackerUserId,
                    armorBonus = roll,
                    reflectDamage = 0,
                )
        )

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutations.attackerUserId,
            nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
        )
    }
}
