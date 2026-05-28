package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class HolyShieldReaction(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val survived = total in 10..12

        return if (survived) {
            combatMutator.updateDefenderClass { playerClass ->
                val holyShield = playerClass.defensiveAbility as DefensiveAbility.HolyShield
                playerClass.copy(
                    hitPoints = 1,
                    defensiveAbility = holyShield.copy(usedThisMatch = true),
                )
            }
            combatMutator.addMessage(
                message =
                    OutgoingMessage.HolyShield.Survival(
                        paladinUserId = combatMutator.defenderUserId,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        didSurvive = true,
                    )
            )
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.defenderUserId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect
                    ),
            )
        } else {
            combatMutator.updateDefenderClass { it.copy(hitPoints = 0) }

            combatMutator.addMessage(
                message =
                    OutgoingMessage.HolyShield.Survival(
                        paladinUserId = combatMutator.defenderUserId,
                        firstDie = firstDie,
                        secondDie = secondDie,
                        didSurvive = false,
                    )
            )
            combatMutator.resolveTurnEnd()
        }
    }
}
