package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class PrepareAttackActivated(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.prepareAttackOutcome()

    private fun CombatMutator.prepareAttackOutcome() {
        val (firstDie, secondDie) = diceRoller.roll2D6()
        val isSuccessful = firstDie + secondDie in 10..12

        if (isSuccessful) {
            updateDefenderHitPoints(hitPoints = 0)
        }

        addMessage(
            message =
                OutgoingArenaMessage.PrepareAttack(
                    duelistUserId = attackerUserId,
                    targetUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )
        )

        resolveTurnEnd()
    }
}
