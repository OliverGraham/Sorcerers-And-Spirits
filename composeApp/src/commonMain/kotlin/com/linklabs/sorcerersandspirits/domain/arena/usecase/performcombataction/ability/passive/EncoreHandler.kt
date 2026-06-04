package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class EncoreHandler(private val combatMutator: CombatMutator) {

    fun triggerEncore() = combatMutator.triggerEncore()

    fun rollEncore() = combatMutator.rollEncore()

    private fun CombatMutator.triggerEncore() {
        addMessage(message = ArenaMessage.Encore.Trigger)

        val purpose = AbilityAction.Passive.Encore
        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    activeUserIds = setOf(attackerUserId),
                )
        )
    }

    private fun CombatMutator.rollEncore() {
        val total = diceRoller.roll1D6(userId = attackerUserId)
        val isSuccessful = total in ENCORE_SUCCESS_RANGE

        addMessage(
            message = ArenaMessage.Encore.Roll(encoreDie = total, isSuccessful = isSuccessful)
        )

        if (isSuccessful) {
            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
            )
        } else {
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

    companion object {
        private val ENCORE_SUCCESS_RANGE = 5..6

        fun triggerEncoreIfApplicable(
            combatMutator: CombatMutator,
            offensiveAbility: OffensiveAbility,
        ): Boolean {
            if (offensiveAbility !is OffensiveAbility.Encore) return false

            EncoreHandler(combatMutator = combatMutator).triggerEncore()

            return true
        }
    }
}
