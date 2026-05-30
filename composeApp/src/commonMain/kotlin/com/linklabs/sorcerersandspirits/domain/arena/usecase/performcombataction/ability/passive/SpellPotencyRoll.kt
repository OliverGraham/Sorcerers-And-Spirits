package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

// TODO: Docstring, etc.
internal class SpellPotencyRoll(
    private val combatMutator: CombatMutator,
    private val diceTotal: Int,
) {
    operator fun invoke() = combatMutator.resolveSpellPotency()

    private fun CombatMutator.resolveSpellPotency() {
        val splashDamage = diceTotal / 2
        applyFullDamageToDefender()
        applySplashDamage(splashDamage = splashDamage)
        addSpellPotencyMessage(splashDamage = splashDamage)

        resolveTurnEnd()
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
                OutgoingArenaMessage.SpellPotency(
                    mageUserId = attackerUserId,
                    mainTargetUserId = defenderUserId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                )
        )
    }
}
