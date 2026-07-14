package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
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
        addMessage(
            message =
                ArenaMessage.Riposte.Trigger(
                    duelistUserId = defenderUserId,
                    attackerUserId = attackerUserId,
                )
        )

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal),
            activeUserIds = setOf(element = defenderUserId),
        )
    }

    /**
     * A doubles Riposte only earns its +1 when the reversal follows: the Duelist keeps the dice and
     * rolls the reversal attack next. A failed Riposte hands the dice back to the attacker's Health
     * roll, so the bonus is not consecutive and is left unannounced.
     */
    private fun CombatMutator.rollRiposte() {
        val result = diceRoller.roll2D6(userId = defenderUserId)
        val isSuccessful = result.total in abilityConfig.riposteSuccessRange

        addMessage(
            message =
                ArenaMessage.Riposte.EnGarde(
                    duelistUserId = defenderUserId,
                    attackerUserId = attackerUserId,
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    isSuccessful = isSuccessful,
                )
        )

        if (isSuccessful) {
            if (result.isDoubles) {
                addMessage(message = ArenaMessage.Doubles(userId = defenderUserId))
            }
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
                    attackTurn =
                        AttackTurn.RiposteReversal(
                            attackerUserId = defenderUserId,
                            turnOwnerUserId = originalTurnUserId,
                        ),
                    defenderUserId = attackerUserId,
                    activeUserIds = setOf(element = defenderUserId),
                )
        )
    }
}
