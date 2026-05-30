package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class HolyShieldWrath(private val combatMutator: CombatMutator) {
    operator fun invoke() = combatMutator.holyShieldWrath()

    private fun CombatMutator.holyShieldWrath() {
        val singleDie = roll1D6()

        addMessage(
            message =
                OutgoingArenaMessage.HolyShield.Wrath(
                    paladinUserId = defenderUserId,
                    attackerUserId =
                        attackerUserId, // TODO SpellPotency - what if is splash damage?
                    singleDie = singleDie,
                )
        )

        updateDefenderClass { defender ->
            defender.copy(hitPoints = defender.hitPoints - singleDie)
        }

        resolveWrath()
    }

    private fun CombatMutator.resolveWrath() {
        val isAttackerConscious = getAttacker().isConscious()
        val attackerAbility = getAttackerOffensiveAbility()

        when {
            isAttackerConscious && attackerAbility == OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            isAttackerConscious && attackerAbility == OffensiveAbility.Encore -> {
                // TODO Encore
            }

            else -> resolveTurnEnd()
        }
    }
}
