package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class SummonMinionActivated(
    private val combatMutator: CombatMutator,
    private val summonMinionAbility: OffensiveAbility.SummonMinion,
) {
    operator fun invoke() = combatMutator.summonMinionOutcome()

    private fun CombatMutator.summonMinionOutcome() {
        val result = diceRoller.roll1D6(userId = attackerUserId).total

        addMessage(
            message = ArenaMessage.SummonMinion(necroUserId = attackerUserId, singleDie = result)
        )

        updateAttackerClass { attacker ->
            var newSkeletonCount = summonMinionAbility.skeletonCount
            var newGolemCount = summonMinionAbility.golemCount
            var newHp = attacker.hitPoints

            when (result) {
                2,
                3 -> newSkeletonCount++

                4,
                5 -> {
                    newSkeletonCount++
                    newHp = attacker.maxHitPoints
                }

                6 -> {
                    newGolemCount++
                    newHp = attacker.maxHitPoints
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

        resolveTurnEnd()
    }
}
