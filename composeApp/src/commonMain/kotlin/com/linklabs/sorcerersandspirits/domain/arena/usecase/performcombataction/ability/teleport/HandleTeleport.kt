package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.SingleDieRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

/** Pass in userId from view model in order to signal who is rolling */
internal class HandleTeleport(
    private val combatMutator: CombatMutator,
    private val rollingDieUserId: String,
) {
    operator fun invoke(teleport: AbilityRollPurpose.Passive.Teleport) {
        teleport.handle()
    }

    private fun AbilityRollPurpose.Passive.Teleport.handle() {
        val updatedTeleport = rollCurrentUserDieOrNull() ?: return

        if (updatedTeleport.hasBothRolls()) {
            updatedTeleport.resolveTeleport()
        } else {
            combatMutator.setNextCombatAction(
                nextAction = NextCombatAction.AbilityRoll(purpose = updatedTeleport)
            )
        }
    }

    private fun AbilityRollPurpose.Passive.Teleport.rollCurrentUserDieOrNull() =
        when {
            attackerDieRoll.canUserRollThisDie() ->
                copy(attackerDieRoll = attackerDieRoll.rollAndMessage())

            defenderDieRoll.canUserRollThisDie() ->
                copy(defenderDieRoll = defenderDieRoll.rollAndMessage())

            else -> null
        }

    private fun SingleDieRoll.canUserRollThisDie() = roll == null && userId == rollingDieUserId

    private fun SingleDieRoll.rollAndMessage(): SingleDieRoll {
        val newRoll = roll1D6()

        combatMutator.addMessage(
            message = OutgoingArenaMessage.Teleport.Roll(userId = userId, singleDie = newRoll)
        )

        return copy(roll = newRoll)
    }

    private fun AbilityRollPurpose.Passive.Teleport.hasBothRolls() =
        attackerDieRoll.roll != null && defenderDieRoll.roll != null

    private fun AbilityRollPurpose.Passive.Teleport.resolveTeleport() {
        val attackerRoll = attackerDieRoll.roll ?: return
        val defenderRoll = defenderDieRoll.roll ?: return

        if (defenderRoll > attackerRoll) {
            handleTeleportSuccess()
        } else {
            handleTeleportFailure()
        }
    }

    private fun handleTeleportSuccess() {
        combatMutator.addMessage(
            message = OutgoingArenaMessage.Teleport.Result(didDefenderEscape = true)
        )

        combatMutator.resolveTurnEnd()
    }

    private fun AbilityRollPurpose.Passive.Teleport.handleTeleportFailure() {
        combatMutator.addMessage(
            message = OutgoingArenaMessage.Teleport.Result(didDefenderEscape = false)
        )

        combatMutator.setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll =
                        NormalAttackRoll.Health(
                            attackerUserId = attackerDieRoll.userId,
                            defenderUserId = defenderDieRoll.userId,
                        )
                )
        )
    }
}
