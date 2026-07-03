package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class TriggerKillingMomentum(private val combatMutator: CombatMutator) {
    operator fun invoke() {
        combatMutator.triggerKillingMomentum()
    }

    private fun CombatMutator.triggerKillingMomentum() {
        addMessage(message = ArenaMessage.KillingMomentum)
        grantBonusAttack()

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
        )
    }
}
