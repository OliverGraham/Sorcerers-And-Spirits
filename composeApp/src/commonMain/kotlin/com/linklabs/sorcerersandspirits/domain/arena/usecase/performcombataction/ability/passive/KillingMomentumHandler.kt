package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class KillingMomentumHandler(private val combatMutator: CombatMutator) {

    fun triggerKillingMomentum() = combatMutator.triggerKillingMomentum()

    private fun CombatMutator.triggerKillingMomentum() {
        addMessage(message = ArenaMessage.KillingMomentum(attackerUserId = attackerUserId))
        grantBonusAttack()

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(element = attackerUserId)),
        )
    }
}
