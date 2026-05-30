package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class HolyShieldHandler(private val combatMutator: CombatMutator) {

    fun triggerHolyShield(holyShield: DefensiveAbility.HolyShield) =
        combatMutator.triggerHolyShield(holyShield = holyShield)

    fun attemptPrayer() = combatMutator.attemptPrayer()

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


    private fun CombatMutator.attemptPrayer() {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val didSurvive = firstDie + secondDie in HOLY_SHIELD_SUCCESS_RANGE

        addMessage(
            message =
                OutgoingArenaMessage.HolyShield.Prayer(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    didSurvive = didSurvive,
                )
        )

        if (didSurvive) {
            prayerSuccess()
        } else {
            prayerFailure()
        }
    }

    private fun CombatMutator.prayerSuccess() {
        updateDefenderClass { defender -> defender.copy(hitPoints = 1) }

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose =
                        AbilityRollPurpose.Passive.HolyShield.Wrath(activeUserId = defenderUserId)
                )
        )
    }

    private fun CombatMutator.prayerFailure() {
        val attackerAbility = attacker.offensiveAbility

        when (attackerAbility) {
            OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            OffensiveAbility.Encore -> {
                // TODO Encore
            }

            else -> resolveTurnEnd()
        }
    }

    companion object {
        // TODO: From RemoteConfig in some way
        private val HOLY_SHIELD_SUCCESS_RANGE = 10..12

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
