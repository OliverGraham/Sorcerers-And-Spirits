package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.Teleport.TeleportDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

// TODO Teleport Messages
internal class TeleportHandler(private val combatMutator: CombatMutator) {

    fun triggerTeleport() = combatMutator.triggerTeleport()

    fun handleTeleport(teleport: AbilityAction.Passive.Teleport) =
        combatMutator.handleTeleport(teleport = teleport)

    private fun CombatMutator.triggerTeleport() {
        addMessage(message = ArenaMessage.Teleport.Trigger)

        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = attackerUserId),
                defenderRoll = TeleportDie(userId = defenderUserId),
            )

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    activeUserIds = setOf(attackerUserId, defenderUserId),
                )
        )
    }

    private fun CombatMutator.handleTeleport(teleport: AbilityAction.Passive.Teleport) {
        val updatedTeleport = rollCurrentUserDieOrNull(teleport = teleport) ?: return

        handleUpdatedTeleport(updatedTeleport = updatedTeleport)
    }

    private fun CombatMutator.handleUpdatedTeleport(
        updatedTeleport: AbilityAction.Passive.Teleport
    ) {
        val attackerRoll = updatedTeleport.attackerRoll.roll
        val defenderRoll = updatedTeleport.defenderRoll.roll

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

    private fun CombatMutator.rollCurrentUserDieOrNull(teleport: AbilityAction.Passive.Teleport) =
        when {
            teleport.attackerRoll.roll == null ->
                teleport.copy(
                    attackerRoll = rollAndMessage(unrolledTeleportDie = teleport.attackerRoll)
                )

            teleport.defenderRoll.roll == null ->
                teleport.copy(
                    defenderRoll = rollAndMessage(unrolledTeleportDie = teleport.defenderRoll)
                )

            else -> null
        }

    private fun CombatMutator.rollAndMessage(unrolledTeleportDie: TeleportDie): TeleportDie {
        val userId = unrolledTeleportDie.userId
        val result = diceRoller.roll1D6(userId = userId).total

        addMessage(message = ArenaMessage.Teleport.Roll(userId = userId, singleDie = result))

        return unrolledTeleportDie.copy(roll = result)
    }

    private fun CombatMutator.handleTeleportSuccess() {
        addMessage(message = ArenaMessage.Teleport.Result(didDefenderEscape = true))

        resolveTurnEnd()
    }

    private fun CombatMutator.handleTeleportFailure() {
        addMessage(message = ArenaMessage.Teleport.Result(didDefenderEscape = false))

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
