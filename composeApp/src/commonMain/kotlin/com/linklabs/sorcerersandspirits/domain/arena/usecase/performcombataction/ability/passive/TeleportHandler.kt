package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.Teleport.TeleportDie
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
                    purpose =
                        AbilityAction.Passive.Teleport.getPreRolledState(
                            attackerUserId = attackerUserId,
                            defenderUserId = defenderUserId,
                        ),
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
        when {
            canUserRollThisDie(
                teleportDie = teleport.attackerDieRoll,
                rollingUserId = rollingUserId,
            ) ->
                teleport.copy(
                    attackerDieRoll = rollAndMessage(teleportDie = teleport.attackerDieRoll)
                )

            canUserRollThisDie(
                teleportDie = teleport.defenderDieRoll,
                rollingUserId = rollingUserId,
            ) ->
                teleport.copy(
                    defenderDieRoll = rollAndMessage(teleportDie = teleport.defenderDieRoll)
                )

            else -> null
        }

    private fun canUserRollThisDie(teleportDie: TeleportDie, rollingUserId: String) =
        teleportDie.roll == null && teleportDie.userId == rollingUserId

    private fun CombatMutator.rollAndMessage(teleportDie: TeleportDie): TeleportDie {
        val newRoll = roll1D6()

        addMessage(
            message =
                OutgoingArenaMessage.Teleport.Roll(userId = teleportDie.userId, singleDie = newRoll)
        )

        return teleportDie.copy(roll = newRoll)
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
