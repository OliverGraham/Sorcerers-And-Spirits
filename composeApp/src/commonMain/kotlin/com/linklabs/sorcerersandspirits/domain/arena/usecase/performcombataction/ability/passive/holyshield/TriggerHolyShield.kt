package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class TriggerHolyShield(
    private val combatMutator: CombatMutator,
    private val holyShield: DefensiveAbility.HolyShield,
) {
    operator fun invoke() = combatMutator.triggerHolyShield()

    private fun CombatMutator.triggerHolyShield() {
        addMessage(message = OutgoingArenaMessage.HolyShield.Trigger)

        updateDefenderClass { defender ->
            defender.copy(defensiveAbility = holyShield.copy(usedThisMatch = true))
        }

        NextCombatAction.AbilityRoll(
            purpose = AbilityRollPurpose.Passive.HolyShield.Prayer(userId = defenderUserId)
        )
    }
}
