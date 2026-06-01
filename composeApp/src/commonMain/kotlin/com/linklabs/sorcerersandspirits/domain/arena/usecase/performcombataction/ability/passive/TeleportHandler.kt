package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.SingleDieRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class TeleportHandler(private val combatMutator: CombatMutator) {

    fun triggerTeleport() = combatMutator.triggerTeleport()

    fun handleTeleport(teleport: AbilityRollPurpose.Passive.Teleport, userId: String) =
        combatMutator.handleTeleport(teleport = teleport, userId = userId)

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

    private fun CombatMutator.handleTeleport(
        teleport: AbilityRollPurpose.Passive.Teleport,
        userId: String,
    ) {

    }

    private fun canUserRollThisDie(singleDieRoll: SingleDieRoll, rollingUserId: String)
        = singleDieRoll.roll == null && singleDieRoll.userId == rollingUserId
}
