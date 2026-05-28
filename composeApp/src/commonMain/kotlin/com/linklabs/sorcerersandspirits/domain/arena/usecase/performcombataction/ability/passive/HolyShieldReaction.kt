package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

class HolyShieldReaction(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val survived = total in 10..12

        return if (survived) {
            combatMutations.updateDefenderClass { playerClass ->
                val holyShield = playerClass.defensiveAbility as DefensiveAbility.HolyShield
                playerClass.copy(
                    hitPoints = 1,
                    defensiveAbility = holyShield.copy(usedThisMatch = true),
                )
            }
            combatMutations.addMessage(
                message =
                    OutgoingMessage.HolyShield(
                        paladinUserId = combatMutations.defenderUserId,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        survived = true,
                    )
            )
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.defenderUserId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.HolyShield
                    ),
            )
        } else {
            combatMutations.updateDefenderClass { it.copy(hitPoints = 0) }

            combatMutations.addMessage(
                message =
                    OutgoingMessage.HolyShield(
                        paladinUserId = combatMutations.defenderUserId,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        survived = false,
                        retaliationDamage = 0,
                    )
            )
            combatMutations.resolveTurnEnd()
        }
    }
}
