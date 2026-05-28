package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class TeleportReaction(private val combatMutations: CombatMutations) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutations.attackerUserId,
            nextStep =
                NextCombatAction.AbilityRoll(
                    purpose = AbilityRollPurpose.Passive.Outcome.Teleport(defenderRoll = roll)
                ),
        )
    }
}
