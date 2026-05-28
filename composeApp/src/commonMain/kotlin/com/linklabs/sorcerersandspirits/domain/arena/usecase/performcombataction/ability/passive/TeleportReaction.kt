package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class TeleportReaction(private val combatMutator: CombatMutator) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6()

        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutator.attackerUserId,
            nextStep =
                NextCombatAction.AbilityRoll(
                    purpose = AbilityRollPurpose.Passive.Outcome.Teleport(defenderRoll = roll)
                ),
        )
    }
}
