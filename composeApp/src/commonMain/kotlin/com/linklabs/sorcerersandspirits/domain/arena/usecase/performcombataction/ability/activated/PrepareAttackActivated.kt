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
                addPrepareAttackMessages(
                    result = result,
                    isSuccessful = true,
                    isTargetKnockedOut = true,
                )
                resolveTurnEnd()
            }

            total in abilityConfig.prepareAttackKnockoutRange -> {
                updateDefenderHitPoints(hitPoints = 0)
                val isHolyShieldAvailable =
                    HolyShieldHandler.isHolyShieldAvailable(
                        defenderDefensiveAbility = defender.defensiveAbility,
                    )
                addPrepareAttackMessages(
                    result = result,
                    isSuccessful = true,
                    isTargetKnockedOut = !isHolyShieldAvailable,
                )

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
                addPrepareAttackMessages(
                    result = result,
                    isSuccessful = false,
                    isTargetKnockedOut = false,
                )
                resolveTurnEnd()
            }
        }
    }

    private fun CombatMutator.addPrepareAttackMessages(
        result: RollResult.TwoDice,
        isSuccessful: Boolean,
        isTargetKnockedOut: Boolean,
    ) {
        addMessage(
            message =
            ArenaMessage.PrepareAttack.Strike(
                duelistUserId = attackerUserId,
                classType = attacker.type,
                targetUserId = defenderUserId,
                isSuccessful = isSuccessful,
                isTargetKnockedOut = isTargetKnockedOut,
            ),
        )
        addMessage(
            message =
            ArenaMessage.PrepareAttack.Outcome(
                duelistUserId = attackerUserId,
                classType = attacker.type,
                duelistName = attacker.className,
                targetClassName = defender.className,
                firstDie = result.firstDie,
                secondDie = result.secondDie,
                isSuccessful = isSuccessful,
            ),
        )
    }
}
