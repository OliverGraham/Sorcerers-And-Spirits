package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

// TODO: FIX
internal class TeleportOutcome(
    private val combatMutator: CombatMutator,
    private val defenderRoll: Int,
) {
    operator fun invoke(): CombatActionUpdates {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        combatMutator.addMessage(message = OutgoingArenaMessage.Teleport.Trigger)

        return if (escaped) {
            combatMutator.resolveTurnEnd()
        } else {
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.Health,
            )
        }
    }
}
