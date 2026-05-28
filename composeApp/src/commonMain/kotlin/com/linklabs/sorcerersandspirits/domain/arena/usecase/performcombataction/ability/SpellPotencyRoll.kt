package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

// TODO: Docstring, etc.
internal class SpellPotencyRoll(
    private val combatMutator: CombatMutator,
    private val diceTotal: Int,
) {

    operator fun invoke() = combatMutator.resolveSpellPotency()

    private fun CombatMutator.resolveSpellPotency(): CombatActionUpdates {
        val splashDamage = diceTotal / 2
        applyFullDamageToDefender()
        applySplashDamage(splashDamage = splashDamage)
        addSpellPotencyMessage(splashDamage = splashDamage)

        return resolveTurnEnd()
    }

    private fun CombatMutator.applyFullDamageToDefender() {
        updateDefenderClass { defender ->
            defender.copy(hitPoints = defender.hitPoints - diceTotal)
        }
    }

    private fun CombatMutator.applySplashDamage(splashDamage: Int) {
        updatedPlayerMap.values
            .filter { it.userId != attackerUserId && it.userId != defenderUserId }
            .filter { it.characterClass.isConscious() }
            .forEach { player ->
                updatePlayerClass(userId = player.userId) { playerClass ->
                    playerClass.copy(hitPoints = playerClass.hitPoints - splashDamage)
                }
            }
    }

    private fun CombatMutator.addSpellPotencyMessage(splashDamage: Int) {
        addMessage(
            message =
                OutgoingMessage.SpellPotency(
                    mageUserId = attackerUserId,
                    mainTargetUserId = defenderUserId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                )
        )
    }
}
