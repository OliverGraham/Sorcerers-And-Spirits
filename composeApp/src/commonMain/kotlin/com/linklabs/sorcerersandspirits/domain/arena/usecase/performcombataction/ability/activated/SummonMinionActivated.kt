package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

class SummonMinionActivated(
    private val combatMutations: CombatMutations,
    private val summonMinionAbility: OffensiveAbility.SummonMinion,
) {
    operator fun invoke() = combatMutations.summonMinionOutcome()

    private fun CombatMutations.summonMinionOutcome(): CombatActionUpdates {
        val singleDie = roll1D6()

        addMessage(
            message =
                OutgoingMessage.SummonMinion(necroUserId = attackerUserId, singleDie = singleDie)
        )

        updateAttackerClass { attacker ->
            var newSkeletonCount = summonMinionAbility.skeletonCount
            var newGolemCount = summonMinionAbility.golemCount
            var newHp = attacker.hitPoints

            when (singleDie) {
                2,
                3 -> newSkeletonCount++

                4,
                5 -> {
                    newSkeletonCount++
                    newHp = 7 // Necro max HP
                }

                6 -> {
                    newGolemCount++
                    newHp = 7
                }

                else -> Unit
            }

            val updatedSummonMinionAbility =
                summonMinionAbility.copy(
                    skeletonCount = newSkeletonCount,
                    golemCount = newGolemCount,
                )
            attacker.copy(hitPoints = newHp, offensiveAbility = updatedSummonMinionAbility)
        }

        return resolveTurnEnd()
    }
}
