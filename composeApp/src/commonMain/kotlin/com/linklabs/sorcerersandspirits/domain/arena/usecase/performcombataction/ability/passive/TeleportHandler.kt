package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.SingleDieRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

// TODO Teleport Messages
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
        val updatedTeleport =
            rollCurrentUserDieOrNull(teleport = teleport, rollingUserId = userId) ?: return

        handleUpdatedTeleport(updatedTeleport = updatedTeleport)
    }

    private fun CombatMutator.handleUpdatedTeleport(
        updatedTeleport: AbilityRollPurpose.Passive.Teleport
    ) {
        val attackerRoll = updatedTeleport.attackerDieRoll.roll
        val defenderRoll = updatedTeleport.defenderDieRoll.roll

        if (attackerRoll != null && defenderRoll != null) {
            if (defenderRoll > attackerRoll) {
                handleTeleportSuccess()
            } else {
                handleTeleportFailure()
            }
        } else {
            setNextCombatAction(
                nextAction = NextCombatAction.AbilityRoll(purpose = updatedTeleport)
            )
        }
    }

    private fun CombatMutator.rollCurrentUserDieOrNull(
        teleport: AbilityRollPurpose.Passive.Teleport,
        rollingUserId: String,
    ) =
        when {
            canUserRollThisDie(
                singleDieRoll = teleport.attackerDieRoll,
                rollingUserId = rollingUserId,
            ) ->
                teleport.copy(
                    attackerDieRoll = rollAndMessage(singleDieRoll = teleport.attackerDieRoll)
                )

            canUserRollThisDie(
                singleDieRoll = teleport.defenderDieRoll,
                rollingUserId = rollingUserId,
            ) ->
                teleport.copy(
                    defenderDieRoll = rollAndMessage(singleDieRoll = teleport.defenderDieRoll)
                )

            else -> null
        }

    private fun canUserRollThisDie(singleDieRoll: SingleDieRoll, rollingUserId: String) =
        singleDieRoll.roll == null && singleDieRoll.userId == rollingUserId

    private fun CombatMutator.rollAndMessage(singleDieRoll: SingleDieRoll): SingleDieRoll {
        val newRoll = roll1D6()

        addMessage(
            message =
                OutgoingArenaMessage.Teleport.Roll(
                    userId = singleDieRoll.userId,
                    singleDie = newRoll,
                )
        )

        return singleDieRoll.copy(roll = newRoll)
    }

    private fun CombatMutator.handleTeleportSuccess() {
        addMessage(message = OutgoingArenaMessage.Teleport.Result(didDefenderEscape = true))

        resolveTurnEnd()
    }

    private fun CombatMutator.handleTeleportFailure() {
        addMessage(message = OutgoingArenaMessage.Teleport.Result(didDefenderEscape = false))

        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll =
                        NormalAttackRoll.Health(
                            attackerUserId = attackerUserId,
                            defenderUserId = defenderUserId,
                        )
                )
        )
    }
}
