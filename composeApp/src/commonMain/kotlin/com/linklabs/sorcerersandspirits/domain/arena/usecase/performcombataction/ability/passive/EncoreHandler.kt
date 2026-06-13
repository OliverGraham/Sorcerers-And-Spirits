package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class EncoreHandler(private val combatMutator: CombatMutator) {

    fun triggerEncore() = combatMutator.triggerEncore()

    fun rollEncore() = combatMutator.rollEncore()

    private fun CombatMutator.triggerEncore() {
        addMessage(message = ArenaMessage.Encore.Trigger)

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Encore,
            activeUserIds = setOf(element = attackerUserId),
        )
    }

    private fun CombatMutator.rollEncore() {
        val result = diceRoller.roll1D6(userId = attackerUserId).total
        val isSuccessful = result in ENCORE_SUCCESS_RANGE

        addMessage(
            message = ArenaMessage.Encore.Roll(encoreDie = result, isSuccessful = isSuccessful)
        )

        if (isSuccessful) {
            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
            )
        } else {
            resolveTurnEnd()
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
