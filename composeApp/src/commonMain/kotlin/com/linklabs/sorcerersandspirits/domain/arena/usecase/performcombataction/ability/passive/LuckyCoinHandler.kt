package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class LuckyCoinHandler(
    private val combatMutator: CombatMutator,
    private val attackerRollTotal: Int,
) {
    fun triggerLuckyCoin() = combatMutator.triggerLuckyCoin()

    fun flipLuckyCoin() = combatMutator.flipLuckyCoin()

    fun handleHighRoller() = combatMutator.handleHighRoller()

    private fun CombatMutator.triggerLuckyCoin() {
        addMessage(message = ArenaMessage.LuckyCoin.Trigger)

        val purpose = AbilityAction.Passive.LuckyCoin.Flip(attackerRollTotal = attackerRollTotal)

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    activeUserIds = setOf(defenderUserId),
                )
        )
    }

    private fun CombatMutator.flipLuckyCoin() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total

        if (result == 6) {
            triggerHighRoller()
        } else {
            handleNormalRoll(luckyCoinArmorBonus = result)
        }
    }

    private fun CombatMutator.triggerHighRoller() {
        addMessage(
            message = ArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = attackerUserId)
        )

        val purpose =
            AbilityAction.Passive.LuckyCoin.HighRoller(
                attackerUserId = attackerUserId,
                attackerRollTotal = attackerRollTotal,
            )

        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    activeUserIds = setOf(defenderUserId),
                )
        )
    }

    private fun CombatMutator.handleHighRoller() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total

        updateAttackerClass { attacker -> attacker.copy(hitPoints = attacker.hitPoints - result) }

        val isKnockout = !attacker.isConscious()
        addMessage(
            message =
                ArenaMessage.LuckyCoin.HighRollerDamage(singleDie = result, isKnockout = isKnockout)
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
                ArenaMessage.LuckyCoin.Flip(singleDie = luckyCoinArmorBonus, didStopAttack = true)
        )

        resolveTurnEnd()
    }

    private fun CombatMutator.handleAttackerWins(luckyCoinArmorBonus: Int) {
        addMessage(
            message =
                ArenaMessage.LuckyCoin.Flip(singleDie = luckyCoinArmorBonus, didStopAttack = false)
        )

        val healthRoll =
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )

        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = healthRoll,
                    activeUserIds = setOf(attackerUserId),
                )
        )
    }
}
