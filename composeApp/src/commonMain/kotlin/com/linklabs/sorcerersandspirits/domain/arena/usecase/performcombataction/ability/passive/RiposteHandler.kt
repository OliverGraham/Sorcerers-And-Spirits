package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class RiposteHandler(
    private val combatMutator: CombatMutator,
    private val attackerRollTotal: Int,
) {
    fun triggerRiposte() = combatMutator.triggerRiposte()

    fun rollRiposte() = combatMutator.rollRiposte()

    private fun CombatMutator.triggerRiposte() {
        addMessage(message = ArenaMessage.Riposte.Trigger)

        val purpose = AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)
        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    activeUserIds = setOf(defenderUserId),
                )
        )
    }

    private fun CombatMutator.rollRiposte() {
        val result = diceRoller.roll2D6(userId = defenderUserId)
        val isSuccessful = result.total in SUCCESS_RANGE

        addMessage(
            message =
                ArenaMessage.Riposte.EnGarde(
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    isSuccessful = isSuccessful,
                )
        )

        if (isSuccessful) {
            handleSuccess()
        } else {
            handleFailure()
        }
    }

    /** Duelist is now attacking the attacker - swapping attacker and defender should do it? */
    private fun CombatMutator.handleSuccess() {
        val armorClassRoll =
            NormalAttackRoll.ArmorClass(
                attackerUserId = defenderUserId,
                defenderUserId = attackerUserId,
                originalTurnUserId = attackerUserId,
            )
        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = armorClassRoll,
                    activeUserIds = setOf(defenderUserId),
                )
        )
    }

    private fun CombatMutator.handleFailure() {
        val healthRoll =
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )
        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = healthRoll,
                    activeUserIds = setOf(attackerUserId),
                )
        )
    }

    companion object {
        private val SUCCESS_RANGE = 6..8
    }
}
