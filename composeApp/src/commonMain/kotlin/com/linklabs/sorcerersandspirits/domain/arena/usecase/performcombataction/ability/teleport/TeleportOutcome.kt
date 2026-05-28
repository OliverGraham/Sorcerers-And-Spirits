package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class TeleportOutcome(
    private val combatMutator: CombatMutator,
    private val defenderRoll: Int,
) {
    operator fun invoke(): CombatActionUpdates {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        combatMutator.addMessage(
            message =
                OutgoingMessage.Teleport(
                    mageUserId = combatMutator.defenderUserId,
                    attackerUserId = combatMutator.attackerUserId,
                    escaped = escaped,
                )
        )

        return if (escaped) {
            combatMutator.resolveTurnEnd()
        } else {
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
            )
        }
    }
}
