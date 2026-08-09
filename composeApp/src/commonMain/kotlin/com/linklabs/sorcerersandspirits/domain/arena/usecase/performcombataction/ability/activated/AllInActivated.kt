package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.withEffective

internal class AllInActivated(
    private val combatMutator: CombatMutator,
    private val allIn: OffensiveAbility.AllIn,
) {
    operator fun invoke() = combatMutator.handleOutcome()

    private fun CombatMutator.handleOutcome() {
        val result =
            diceRoller
                .roll1D6(userId = attackerUserId, dieColor = dieColorFor(userId = attackerUserId))
                .total

        addMessage(
            message =
                ArenaMessage.AllIn(
                    gamblerUserId = attackerUserId,
                    gamblerName = attacker.className,
                    singleDie = result,
                )
        )

        when (result) {
            abilityConfig.allInSelfKnockoutRoll -> {
                updateAttackerClass { attacker -> attacker.copy(hitPoints = 0) }

                resolveTurnEnd()
            }

            in abilityConfig.allInAttackTwiceRange -> {
                updateAttackerClass { attacker ->
                    val updatedAllIn = allIn.copy(hasAnotherAttack = true, usedThisTurn = true)
                    attacker.copy(
                        offensiveAbility = attacker.offensiveAbility.withEffective(updatedAllIn)
                    )
                }
                grantBonusAttack()

                setNextCombatAction(
                    nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
                )
            }

            else -> {
                updateAttackerClass { attacker ->
                    val updatedAllIn = allIn.copy(usedThisTurn = true)
                    attacker.copy(
                        offensiveAbility = attacker.offensiveAbility.withEffective(updatedAllIn)
                    )
                }

                setNextCombatAction(
                    nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
                )
            }
        }
    }
}
