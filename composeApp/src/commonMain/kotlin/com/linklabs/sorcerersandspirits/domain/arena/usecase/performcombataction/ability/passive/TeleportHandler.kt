package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

// TODO Teleport Messages
internal class TeleportHandler(private val combatMutator: CombatMutator) {

    fun triggerTeleport() = combatMutator.triggerTeleport()

    fun handleTeleport(teleport: AbilityAction.Passive.Teleport, userId: String) =
        combatMutator.handleTeleport(teleport = teleport, userId = userId)

    private fun CombatMutator.triggerTeleport() {
        addMessage(message = OutgoingArenaMessage.Teleport.Trigger)

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = AbilityAction.Passive.Teleport(),
                    activeUserIds = setOf(attackerUserId, defenderUserId),
                )
        )
    }

    private fun CombatMutator.handleTeleport(
        teleport: AbilityAction.Passive.Teleport,
        userId: String,
    ) {
        val updatedTeleport =
            rollCurrentUserDieOrNull(teleport = teleport, rollingUserId = userId) ?: return

        handleUpdatedTeleport(updatedTeleport = updatedTeleport)
    }

    private fun CombatMutator.handleUpdatedTeleport(
        updatedTeleport: AbilityAction.Passive.Teleport
    ) {
        val attackerRoll = updatedTeleport.attackerRoll
        val defenderRoll = updatedTeleport.defenderRoll

        if (attackerRoll != null && defenderRoll != null) {
            if (defenderRoll > attackerRoll) {
                handleTeleportSuccess()
            } else {
                handleTeleportFailure()
            }
        } else {
            setNextCombatAction(
                nextAction =
                    NextCombatAction.AbilityRoll(
                        purpose = updatedTeleport,
                        activeUserIds = setOf(attackerUserId, defenderUserId),
                    )
            )
        }
    }

    private fun CombatMutator.rollCurrentUserDieOrNull(
        teleport: AbilityAction.Passive.Teleport,
        rollingUserId: String,
    ) =
        when (rollingUserId) {
            attackerUserId if teleport.attackerRoll == null ->
                teleport.copy(attackerRoll = rollAndMessage(userId = attackerUserId))

            defenderUserId if teleport.defenderRoll == null ->
                teleport.copy(defenderRoll = rollAndMessage(userId = defenderUserId))

            else -> null
        }

    private fun CombatMutator.rollAndMessage(userId: String): Int {
        val newRoll = roll1D6()

        addMessage(
            message = OutgoingArenaMessage.Teleport.Roll(userId = userId, singleDie = newRoll)
        )

        return newRoll
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
                        ),
                    activeUserIds = setOf(attackerUserId),
                )
        )
    }
}
