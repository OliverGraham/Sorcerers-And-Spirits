package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class TriggerLuckyCoin(
    private val combatMutator: CombatMutator,
    private val attackerRollTotal: Int,
) {
    operator fun invoke() = combatMutator.triggerLuckyCoin()

    private fun CombatMutator.triggerLuckyCoin() {
        addMessage(message = OutgoingArenaMessage.LuckyCoin.Trigger)

        val purpose =
            AbilityRollPurpose.Passive.LuckyCoin.Flip(
                attackerRollTotal = attackerRollTotal,
                activeUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }
}
