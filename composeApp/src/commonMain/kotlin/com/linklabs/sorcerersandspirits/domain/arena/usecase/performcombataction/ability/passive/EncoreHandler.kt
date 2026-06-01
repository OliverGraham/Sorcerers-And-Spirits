package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class EncoreHandler(private val combatMutator: CombatMutator) {

    fun triggerEncore() = combatMutator.triggerEncore()

    fun rollEncore() = combatMutator.rollEncore()

    private fun CombatMutator.triggerEncore() {
        addMessage(message = OutgoingArenaMessage.Encore.Trigger)

        val purpose = AbilityAction.Passive.Encore(activeUserId = attackerUserId)
        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.rollEncore() {
        val encoreDie = roll1D6()
        val isSuccessful = encoreDie in ENCORE_SUCCESS_RANGE

        addMessage(
            message =
                OutgoingArenaMessage.Encore.Roll(encoreDie = encoreDie, isSuccessful = isSuccessful)
        )

        if (isSuccessful) {
            setNextCombatAction(nextAction = NextCombatAction.Idle(activeUserId = attackerUserId))
        } else {
            val healthRoll =
                NormalAttackRoll.Health(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                )
            setNextCombatAction(nextAction = NextCombatAction.NormalAttack(roll = healthRoll))
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
