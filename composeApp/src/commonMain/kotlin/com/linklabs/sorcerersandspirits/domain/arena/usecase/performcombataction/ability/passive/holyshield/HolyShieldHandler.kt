package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class HolyShieldHandler(private val combatMutator: CombatMutator) {

    fun triggerHolyShield(holyShield: DefensiveAbility.HolyShield) =
        combatMutator.triggerHolyShield(holyShield = holyShield)

    private fun CombatMutator.triggerHolyShield(holyShield: DefensiveAbility.HolyShield) {
        addMessage(message = OutgoingArenaMessage.HolyShield.Trigger)

        updateDefenderClass { defender ->
            defender.copy(defensiveAbility = holyShield.copy(usedThisMatch = true))
        }

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose =
                        AbilityRollPurpose.Passive.HolyShield.Prayer(activeUserId = defenderUserId)
                )
        )
    }

    companion object {
        fun triggerHolyShieldIfApplicable(
            combatMutator: CombatMutator,
            defenderDefensiveAbility: DefensiveAbility,
        ): Boolean {
            val shouldTriggerHolyShield =
                defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                    !defenderDefensiveAbility.usedThisMatch

            if (shouldTriggerHolyShield) {
                HolyShieldHandler(combatMutator = combatMutator)
                    .triggerHolyShield(holyShield = defenderDefensiveAbility)
            }

            return shouldTriggerHolyShield
        }
    }
}
