package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class AllInActivated(
    private val combatMutations: CombatMutations,
    private val allIn: OffensiveAbility.AllIn,
) {
    operator fun invoke() = combatMutations.handleOutcome()

    private fun CombatMutations.handleOutcome(): CombatActionUpdates {
        val singleDie = roll1D6()
        addMessage(
            message = OutgoingMessage.AllIn(gamblerUserId = attackerUserId, singleDie = singleDie)
        )

        return when (singleDie) {
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

                CombatActionUpdates(
                    nextActivePlayerUserId = attackerUserId,
                    nextStep = NextCombatAction.Idle,
                )
            }

            else -> {
                updateAttackerClass { attacker ->
                    attacker.copy(offensiveAbility = allIn.copy(usedThisTurn = true))
                }

                CombatActionUpdates(
                    nextActivePlayerUserId = attackerUserId,
                    nextStep = NextCombatAction.Idle,
                )
            }
        }
    }
}
