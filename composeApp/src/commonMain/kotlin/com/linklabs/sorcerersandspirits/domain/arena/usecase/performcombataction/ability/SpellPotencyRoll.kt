package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations

// TODO: Docstring, etc.
class SpellPotencyRoll(private val combatMutations: CombatMutations, private val diceTotal: Int) {

    operator fun invoke() = combatMutations.resolveSpellPotency()

    private fun CombatMutations.resolveSpellPotency(): CombatActionUpdates {
        val splashDamage = diceTotal / 2
        applyFullDamageToDefender()
        applySplashDamage(splashDamage = splashDamage)
        addSpellPotencyMessage(splashDamage = splashDamage)

        return resolveTurnEnd()
    }

    private fun CombatMutations.applyFullDamageToDefender() {
        updateDefenderClass { defender ->
            defender.copy(hitPoints = defender.hitPoints - diceTotal)
        }
    }

    private fun CombatMutations.applySplashDamage(splashDamage: Int) {
        updatedPlayerMap.values
            .filter { it.userId != attackerUserId && it.userId != defenderUserId }
            .filter { it.characterClass.isConscious() }
            .forEach { player ->
                updatePlayerClass(userId = player.userId) { playerClass ->
                    playerClass.copy(hitPoints = playerClass.hitPoints - splashDamage)
                }
            }
    }

    private fun CombatMutations.addSpellPotencyMessage(splashDamage: Int) {
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
