package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class AllInActivated(
    private val combatMutator: CombatMutator,
    private val allIn: OffensiveAbility.AllIn,
) {
    operator fun invoke() = combatMutator.handleOutcome()

    private fun CombatMutator.handleOutcome() {
        val result = diceRoller.roll1D6(userId = attackerUserId).total
        addMessage(message = ArenaMessage.AllIn(gamblerUserId = attackerUserId, singleDie = result))

        when (result) {
            1 -> {
                updateAttackerClass { attacker -> attacker.copy(hitPoints = 0) }

                resolveTurnEnd()
            }

            5,
            6 -> {
                updateAttackerClass { attacker ->
                    attacker.copy(
                        offensiveAbility = allIn.copy(hasAnotherAttack = true, usedThisTurn = true)
                    )
                }

                setNextCombatAction(
                    nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
                )
            }

            else -> {
                updateAttackerClass { attacker ->
                    attacker.copy(offensiveAbility = allIn.copy(usedThisTurn = true))
                }

                setNextCombatAction(
                    nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
                )
            }
        }
    }
}
