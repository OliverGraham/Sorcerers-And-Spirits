package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollResult

internal class PrepareAttackActivated(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.prepareAttackOutcome()

    private fun CombatMutator.prepareAttackOutcome() {
        val result =
            diceRoller.roll2D6(
                userId = attackerUserId,
                dieColor = dieColorFor(userId = attackerUserId),
            )
        val total = result.total

        when {
            total == abilityConfig.prepareAttackBypassHolyShieldRoll -> {
                updateDefenderHitPoints(hitPoints = 0)
                HolyShieldHandler.bypassHolyShieldIfApplicable(
                    combatMutator = this,
                    defenderDefensiveAbility = defender.defensiveAbility,
                )
                addPrepareAttackMessage(result = result, isSuccessful = true)
                resolveTurnEnd()
            }

            total in abilityConfig.prepareAttackKnockoutRange -> {
                updateDefenderHitPoints(hitPoints = 0)
                addPrepareAttackMessage(result = result, isSuccessful = true)

                val wasHolyShieldTriggered =
                    HolyShieldHandler.triggerHolyShieldIfApplicable(
                        combatMutator = this,
                        defenderDefensiveAbility = defender.defensiveAbility,
                    )

                if (!wasHolyShieldTriggered) {
                    resolveTurnEnd()
                }
            }

            else -> {
                addPrepareAttackMessage(result = result, isSuccessful = false)
                resolveTurnEnd()
            }
        }
    }

    private fun CombatMutator.addPrepareAttackMessage(
        result: RollResult.TwoDice,
        isSuccessful: Boolean,
    ) {
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
    }
}
