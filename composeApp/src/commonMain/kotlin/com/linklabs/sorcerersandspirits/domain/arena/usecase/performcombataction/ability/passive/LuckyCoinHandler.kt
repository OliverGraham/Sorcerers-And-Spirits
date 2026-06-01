package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class LuckyCoinHandler(
    private val combatMutator: CombatMutator,
    private val attackerRollTotal: Int,
) {
    fun triggerLuckyCoin() = combatMutator.triggerLuckyCoin()

    fun flipLuckyCoin() = combatMutator.flipLuckyCoin()

    fun handleHighRoller() = combatMutator.handleHighRoller()

    private fun CombatMutator.triggerLuckyCoin() {
        addMessage(message = OutgoingArenaMessage.LuckyCoin.Trigger)

        val purpose =
            AbilityAction.Passive.LuckyCoin.Flip(
                attackerRollTotal = attackerRollTotal,
                activeUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.flipLuckyCoin() {
        val luckyCoinArmorBonus = roll1D6()

        if (luckyCoinArmorBonus == 6) {
            triggerHighRoller()
        } else {
            handleNormalRoll(luckyCoinArmorBonus = luckyCoinArmorBonus)
        }
    }

    private fun CombatMutator.triggerHighRoller() {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = attackerUserId)
        )

        val purpose =
            AbilityAction.Passive.LuckyCoin.HighRoller(
                attackerUserId = attackerUserId,
                attackerRollTotal = attackerRollTotal,
                activeUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.handleHighRoller() {
        val highRollerDamageRoll = roll1D6()

        updateAttackerClass { attacker ->
            attacker.copy(hitPoints = attacker.hitPoints - highRollerDamageRoll)
        }

        val isKnockout = !attacker.isConscious()
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.HighRollerDamage(
                    singleDie = highRollerDamageRoll,
                    isKnockout = isKnockout,
                )
        )

        if (isKnockout) {
            handleAttackerKnockout()
        } else {
            handleNormalRoll(luckyCoinArmorBonus = 6)
        }
    }

    private fun CombatMutator.handleAttackerKnockout() {
        val wasHolyShieldTriggered =
            HolyShieldHandler.triggerHolyShieldIfApplicable(
                combatMutator = combatMutator,
                defenderDefensiveAbility = defender.defensiveAbility,
            )
        if (wasHolyShieldTriggered) return

        resolveTurnEnd()
    }

    private fun CombatMutator.handleNormalRoll(luckyCoinArmorBonus: Int) {
        val effectiveArmorClass = defender.armorClass + luckyCoinArmorBonus
        if (effectiveArmorClass > attackerRollTotal) {
            handleDefenderWins(luckyCoinArmorBonus = luckyCoinArmorBonus)
        } else {
            handleAttackerWins(luckyCoinArmorBonus = luckyCoinArmorBonus)
        }
    }

    private fun CombatMutator.handleDefenderWins(luckyCoinArmorBonus: Int) {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.Flip(
                    singleDie = luckyCoinArmorBonus,
                    didStopAttack = true,
                )
        )

        resolveTurnEnd()
    }

    private fun CombatMutator.handleAttackerWins(luckyCoinArmorBonus: Int) {
        addMessage(
            message =
                OutgoingArenaMessage.LuckyCoin.Flip(
                    singleDie = luckyCoinArmorBonus,
                    didStopAttack = false,
                )
        )

        val healthRoll =
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.NormalAttack(roll = healthRoll))
    }
}
