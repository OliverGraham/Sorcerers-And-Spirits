package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.Minion
import com.linklabs.sorcerersandspirits.domain.game.MinionNames
import com.linklabs.sorcerersandspirits.domain.game.MinionType
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

        val summonResult = getSummonResult(result = result)

        addMessage(
            message =
            ArenaMessage.SummonMinion(
                necromancerUserId = attackerUserId,
                className = attacker.className,
                classType = attacker.type,
                singleDie = result,
                minionIndex = summonMinionAbility.minions.size,
                minionName = summonResult?.minion?.name,
            ),
        )

        if (summonResult == null) {
            handleFailure()
        } else {
            handleSuccess(summonResult = summonResult)
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.getSummonResult(result: Int) = when (result) {
        in abilityConfig.summonSkeletonRange -> SummonResult(
            minion = summonedMinion(type = MinionType.Skeleton),
            shouldHealToFull = false,
        )

        in abilityConfig.summonSkeletonAndHealRange -> SummonResult(
            minion = summonedMinion(type = MinionType.Skeleton),
            shouldHealToFull = true,
        )

        abilityConfig.summonGolemRoll -> SummonResult(
            minion = summonedMinion(type = MinionType.Golem),
            shouldHealToFull = true,
        )

        else -> null
    }

    private fun CombatMutator.summonedMinion(type: MinionType): Minion = Minion(
        type = type,
        name = MinionNames.nameNotIn(usedNames = livingMinionNames()),
        hasAttackedThisRound = false,
    )

    private fun CombatMutator.handleFailure() {
        updateAttackerClass { attacker ->
            attacker.copy(
                offensiveAbility =
                attacker.offensiveAbility.withEffective(
                    updated = summonMinionAbility.copy(
                        usedThisTurn = true,
                    ),
                ),
            )
        }
    }

    private fun CombatMutator.handleSuccess(summonResult: SummonResult) {
        val updatedMinions = summonMinionAbility.minions + summonResult.minion

        val updatedSummonMinionAbility =
            summonMinionAbility.copy(
                minions = updatedMinions,
                usedThisTurn = true,
            )

        updateAttackerClass { attacker ->
            attacker.copy(
                hitPoints = if (summonResult.shouldHealToFull) attacker.maxHitPoints else attacker.hitPoints,
                offensiveAbility =
                attacker.offensiveAbility.withEffective(updated = updatedSummonMinionAbility),
            )
        }
    }

    companion object {
        private data class SummonResult(
            val minion: Minion,
            val shouldHealToFull: Boolean,
        )
    }
}
