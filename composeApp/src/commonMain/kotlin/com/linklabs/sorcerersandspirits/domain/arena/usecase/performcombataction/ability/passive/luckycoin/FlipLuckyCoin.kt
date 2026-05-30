package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class FlipLuckyCoin(
    private val combatMutator: CombatMutator,
    private val flipTrigger: AbilityRollPurpose.Passive.LuckyCoin.Flip,
) {
    operator fun invoke() = combatMutator.flipLuckyCoin()

    private fun CombatMutator.flipLuckyCoin() {
        val singleDie = roll1D6()

        if (singleDie == 6) {
            handleHighRoller()
        } else {
            handleNormalRoll(singleDie = singleDie)
        }
    }

    private fun CombatMutator.handleHighRoller() {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = attackerUserId)
        )

        val purpose =
            AbilityRollPurpose.Passive.LuckyCoin.HighRoller(
                attackerUserId = attackerUserId,
                attackerRollTotal = flipTrigger.attackerRollTotal,
                activeUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.handleNormalRoll(singleDie: Int) {
        val effectiveArmorClass = defender.armorClass + singleDie
        if (effectiveArmorClass > flipTrigger.attackerRollTotal) {
            handleDefenderWins(singleDie = singleDie)
        } else {
            handleAttackerWins(singleDie = singleDie)
        }
    }

    private fun CombatMutator.handleDefenderWins(singleDie: Int) {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.Flip(singleDie = singleDie, didStopAttack = true)
        )

        resolveTurnEnd()
    }

    private fun CombatMutator.handleAttackerWins(singleDie: Int) {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.Flip(singleDie = singleDie, didStopAttack = false)
        )

        val healthRoll =
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.NormalAttack(roll = healthRoll))
    }
}
