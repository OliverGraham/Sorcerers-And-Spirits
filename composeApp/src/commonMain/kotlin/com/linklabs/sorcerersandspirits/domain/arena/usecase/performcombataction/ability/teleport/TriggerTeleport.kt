package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class TriggerTeleport(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.triggerTeleport()

    private fun CombatMutator.triggerTeleport() {
        addMessage(message = OutgoingArenaMessage.Teleport.Trigger)

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose =
                        AbilityRollPurpose.Passive.Teleport.getPreRolledState(
                            attackerUserId = attackerUserId,
                            defenderUserId = defenderUserId,
                        )
                )
        )
    }
}
