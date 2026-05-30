package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class AttemptHolyShieldPrayer(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.holyShieldPrayer()

    private fun CombatMutator.holyShieldPrayer() {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val didSurvive = firstDie + secondDie in 10..12

        addMessage(
            message =
                OutgoingArenaMessage.HolyShield.Prayer(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    didSurvive = didSurvive,
                )
        )

        if (didSurvive) {
            handleSuccess()
        } else {
            handleFailure()
        }
    }

    private fun CombatMutator.handleSuccess() {
        updateDefenderClass { defender -> defender.copy(hitPoints = 1) }

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = AbilityRollPurpose.Passive.HolyShield.Wrath(userId = defenderUserId)
                )
        )
    }

    private fun CombatMutator.handleFailure() {
        val attackerAbility = getAttackerOffensiveAbility()

        when (attackerAbility) {
            OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            OffensiveAbility.Encore -> {
                // TODO Encore
            }

            else -> resolveTurnEnd()
        }
    }
}
