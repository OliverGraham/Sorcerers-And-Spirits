package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class RiposteHandler(
    private val combatMutator: CombatMutator,
    private val attackerRollTotal: Int,
) {
    fun triggerRiposte() = combatMutator.triggerRiposte()

    fun rollRiposte() = combatMutator.rollRiposte()

    private fun CombatMutator.triggerRiposte() {
        addMessage(message = ArenaMessage.Riposte.Trigger)

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal),
            activeUserIds = setOf(element = defenderUserId),
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
            continueAttackWithHealthRoll()
        }
    }

    /** Duelist is now attacking the attacker - swapping attacker and defender should do it? */
    private fun CombatMutator.handleSuccess() {
        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = NormalAttackType.ArmorClass,
                    attackerUserId = defenderUserId,
                    defenderUserId = attackerUserId,
                    originalTurnUserId = originalTurnUserId,
                    activeUserIds = setOf(element = defenderUserId),
                )
        )
    }

    companion object {
        private val SUCCESS_RANGE = 6..8
    }
}
