package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.Minion
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
                classType = attacker.type,
                singleDie = result,
                minionIndex = summonMinionAbility.minions.size,
            ),
        )

        updateAttackerClass { attacker ->
            val summonedMinion =
                when (result) {
                    in abilityConfig.summonSkeletonRange -> Minion.Skeleton
                    in abilityConfig.summonSkeletonAndHealRange -> Minion.Skeleton
                    abilityConfig.summonGolemRoll -> Minion.Golem
                    else -> null
                }
            val healsToFull =
                result in abilityConfig.summonSkeletonAndHealRange ||
                    result == abilityConfig.summonGolemRoll

            val newMinions =
                if (summonedMinion != null) {
                    summonMinionAbility.minions + summonedMinion
                } else {
                    summonMinionAbility.minions
                }

            val updatedSummonMinionAbility =
                summonMinionAbility.copy(
                    minions = newMinions,
                    attacksTakenThisTurn = newMinions.size,
                )
            attacker.copy(
                hitPoints = if (healsToFull) attacker.maxHitPoints else attacker.hitPoints,
                offensiveAbility =
                attacker.offensiveAbility.withEffective(updatedSummonMinionAbility),
            )
        }

        resolveTurnEnd()
    }
}
