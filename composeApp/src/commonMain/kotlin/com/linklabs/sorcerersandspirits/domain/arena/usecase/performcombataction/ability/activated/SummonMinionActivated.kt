package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.Minion
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

        handleSummonResult(result = result)

        resolveTurnEnd()
    }

    private fun CombatMutator.handleSummonResult(result: Int) {
        val summonResult = getSummonResult(result = result) ?: return

        val updatedMinions = summonMinionAbility.minions + summonResult.minion

        val updatedSummonMinionAbility =
            summonMinionAbility.copy(
                minions = updatedMinions,
                attacksTakenThisTurn = updatedMinions.size,
            )

        updateAttackerClass { attacker ->
            attacker.copy(
                hitPoints = if (summonResult.shouldHealToFull) attacker.maxHitPoints else attacker.hitPoints,
                offensiveAbility =
                attacker.offensiveAbility.withEffective(updated = updatedSummonMinionAbility),
            )
        }
    }

    private fun CombatMutator.getSummonResult(result: Int) = when (result) {
        in abilityConfig.summonSkeletonRange -> SummonResult(
            minion = Minion(type = MinionType.Skeleton, hasAttackedThisRound = false),
            shouldHealToFull = false,
        )

        in abilityConfig.summonSkeletonAndHealRange -> SummonResult(
            minion = Minion(type = MinionType.Skeleton, hasAttackedThisRound = false),
            shouldHealToFull = true,
        )

        abilityConfig.summonGolemRoll -> SummonResult(
            minion = Minion(type = MinionType.Golem, hasAttackedThisRound = false),
            shouldHealToFull = true,
        )

        else -> null
    }

    companion object {
        private data class SummonResult(
            val minion: Minion,
            val shouldHealToFull: Boolean,
        )
    }
}
