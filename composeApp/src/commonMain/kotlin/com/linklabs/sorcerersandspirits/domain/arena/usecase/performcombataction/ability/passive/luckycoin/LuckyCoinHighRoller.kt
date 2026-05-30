package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.triggerHolyShieldIfApplicable
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class LuckyCoinHighRoller(
    private val combatMutator: CombatMutator,
    private val highRoller: AbilityRollPurpose.Passive.LuckyCoin.HighRoller,
) {
    operator fun invoke() = combatMutator.highRoller()

    private fun CombatMutator.highRoller() {
        val singleDie = roll1D6()

        updateAttackerClass { attacker ->
            attacker.copy(hitPoints = attacker.hitPoints - singleDie)
        }

        val isKnockout = !attacker.isConscious()
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.HighRollerDamage(
                    singleDie = singleDie,
                    isKnockout = isKnockout,
                )
        )

        if (isKnockout) {
            handleAttackerKnockout()
        } else {
            handleNormalRoll()
        }
    }

    private fun CombatMutator.handleAttackerKnockout() {
        val isHolyShieldTriggered =
            triggerHolyShieldIfApplicable(defenderDefensiveAbility = defender.defensiveAbility)
        if (isHolyShieldTriggered) return

        resolveTurnEnd()
    }

    private fun CombatMutator.handleNormalRoll() {
        val highRollerRoll = 6 // TODO: Can be a const property of the ability itself?
        val effectiveArmorClass = defender.armorClass + highRollerRoll
        if (effectiveArmorClass > highRoller.attackerRollTotal) {
            handleDefenderWins(singleDie = highRollerRoll)
        } else {
            handleAttackerWins(singleDie = highRollerRoll)
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
