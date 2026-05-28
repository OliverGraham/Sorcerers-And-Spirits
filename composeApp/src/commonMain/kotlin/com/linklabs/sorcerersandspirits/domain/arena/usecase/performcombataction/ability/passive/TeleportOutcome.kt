package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

class TeleportOutcome(private val combatMutations: CombatMutations, private val defenderRoll: Int) {
    operator fun invoke(): CombatActionUpdates {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        combatMutations.addMessage(
            message =
                OutgoingMessage.Teleport(
                    mageUserId = combatMutations.defenderUserId,
                    attackerUserId = combatMutations.attackerUserId,
                    escaped = escaped,
                )
        )

        return if (escaped) {
            combatMutations.resolveTurnEnd()
        } else {
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
            )
        }
    }
}
