package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class AllInActivated(
    private val combatMutator: CombatMutator,
    private val allIn: OffensiveAbility.AllIn,
) {
    operator fun invoke() = combatMutator.handleOutcome()

    private fun CombatMutator.handleOutcome() {
        val singleDie = roll1D6()
        addMessage(
            message =
                OutgoingArenaMessage.AllIn(gamblerUserId = attackerUserId, singleDie = singleDie)
        )

        when (singleDie) {
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
                    nextAction = NextCombatAction.Idle(activeUserId = attackerUserId)
                )
            }

            else -> {
                updateAttackerClass { attacker ->
                    attacker.copy(offensiveAbility = allIn.copy(usedThisTurn = true))
                }

                setNextCombatAction(
                    nextAction = NextCombatAction.Idle(activeUserId = attackerUserId)
                )
            }
        }
    }
}
