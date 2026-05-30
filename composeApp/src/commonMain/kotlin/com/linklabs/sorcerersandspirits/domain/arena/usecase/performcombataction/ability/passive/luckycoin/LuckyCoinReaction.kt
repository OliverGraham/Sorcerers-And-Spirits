package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

// TODO: Very wrong
internal class LuckyCoinReaction(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()

        combatMutator.updateDefenderClass { playerClass ->
            playerClass.copy(armorClass = playerClass.armorClass + roll)
        }

        if (roll == 6) {
            return CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.defenderUserId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.LuckyCoin.Reflect
                    ),
            )
        }

        combatMutator.addMessage(
            message = OutgoingArenaMessage.LuckyCoin.ArmorClassRoll(singleDie = roll)
        )

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutator.attackerUserId,
            nextStep = NextCombatAction.NormalAttackRoll.Health,
        )
    }
}
