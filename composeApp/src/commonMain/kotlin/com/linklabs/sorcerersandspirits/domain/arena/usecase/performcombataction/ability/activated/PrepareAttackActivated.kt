package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class PrepareAttackActivated(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.prepareAttackOutcome()

    private fun CombatMutator.prepareAttackOutcome(): CombatActionUpdates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val isSuccessful = firstDie + secondDie in 10..12

        if (isSuccessful) {
            updateDefenderHitPoints(hitPoints = 0)
        }

        addMessage(
            message =
                OutgoingMessage.PrepareAttack(
                    duelistUserId = attackerUserId,
                    targetUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )
        )

        return resolveTurnEnd()
    }
}
