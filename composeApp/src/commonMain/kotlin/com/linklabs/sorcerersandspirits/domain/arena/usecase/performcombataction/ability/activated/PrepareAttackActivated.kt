package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class PrepareAttackActivated(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.prepareAttackOutcome()

    private fun CombatMutator.prepareAttackOutcome() {
        val result = diceRoller.roll2D6(userId = attackerUserId)
        val isSuccessful = result.total in 10..12

        if (isSuccessful) {
            updateDefenderHitPoints(hitPoints = 0)
        }

        addMessage(
            message =
                ArenaMessage.PrepareAttack(
                    duelistUserId = attackerUserId,
                    targetUserId = defenderUserId,
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    isSuccessful = isSuccessful,
                )
        )

        resolveTurnEnd()
    }
}
