package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.withEffective

internal class SummonMinionActivated(
    private val combatMutator: CombatMutator,
    private val summonMinionAbility: OffensiveAbility.SummonMinion,
) {
    operator fun invoke() = combatMutator.summonMinionOutcome()

    private fun CombatMutator.summonMinionOutcome() {
        val result =
            diceRoller
                .roll1D6(userId = attackerUserId, dieColor = dieColorFor(userId = attackerUserId))
                .total

        addMessage(
            message =
                ArenaMessage.SummonMinion(
                    necromancerUserId = attackerUserId,
                    className = attacker.className,
                    singleDie = result,
                )
        )

        updateAttackerClass { attacker ->
            var newSkeletonCount = summonMinionAbility.skeletonCount
            var newGolemCount = summonMinionAbility.golemCount
            var newHp = attacker.hitPoints

            when (result) {
                in abilityConfig.summonSkeletonRange -> newSkeletonCount++

                in abilityConfig.summonSkeletonAndHealRange -> {
                    newSkeletonCount++
                    newHp = attacker.maxHitPoints
                }

                abilityConfig.summonGolemRoll -> {
                    newGolemCount++
                    newHp = attacker.maxHitPoints
                }

                else -> Unit
            }

            val updatedSummonMinionAbility =
                summonMinionAbility.copy(
                    skeletonCount = newSkeletonCount,
                    golemCount = newGolemCount,
                    attacksTakenThisTurn = newSkeletonCount + newGolemCount,
                )
            attacker.copy(
                hitPoints = newHp,
                offensiveAbility =
                    attacker.offensiveAbility.withEffective(updatedSummonMinionAbility),
            )
        }

        resolveTurnEnd()
    }
}
