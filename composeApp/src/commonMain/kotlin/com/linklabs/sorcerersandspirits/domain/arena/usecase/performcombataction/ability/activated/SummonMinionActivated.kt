package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class SummonMinionActivated(
    private val combatMutator: CombatMutator,
    private val summonMinionAbility: OffensiveAbility.SummonMinion,
) {
    operator fun invoke() = combatMutator.summonMinionOutcome()

    private fun CombatMutator.summonMinionOutcome() {
        val singleDie = roll1D6()

        addMessage(
            message =
                OutgoingArenaMessage.SummonMinion(
                    necroUserId = attackerUserId,
                    singleDie = singleDie,
                )
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

        resolveTurnEnd()
    }
}
