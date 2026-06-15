package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

// TODO(SS-053): Docstring, etc.
internal class SpellPotencyRoll(
    private val combatMutator: CombatMutator,
    private val diceTotal: Int,
) {
    operator fun invoke() = combatMutator.resolveSpellPotency()

    private fun CombatMutator.resolveSpellPotency() {
        val splashDamage = diceTotal / 2
        addSpellPotencyMessage(splashDamage = splashDamage)
        applyFullDamageToDefender()
        applySplashDamage(splashDamage = splashDamage)

        if (!triggerHolyShieldIfApplicable()) {
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.applyFullDamageToDefender() {
        updateDefenderClass { defender ->
            defender.copy(hitPoints = defender.hitPoints - diceTotal)
        }
    }

    private fun CombatMutator.applySplashDamage(splashDamage: Int) {
        consciousPlayers
            .filter { it.userId != attackerUserId && it.userId != defenderUserId }
            .forEach { player ->
                updatePlayerClass(userId = player.userId) { playerClass ->
                    playerClass.copy(hitPoints = playerClass.hitPoints - splashDamage)
                }
            }
    }

    private fun CombatMutator.addSpellPotencyMessage(splashDamage: Int) {
        addMessage(
            message =
                ArenaMessage.SpellPotency(
                    mageUserId = attackerUserId,
                    mainTargetUserId = defenderUserId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                )
        )
    }

    private fun CombatMutator.triggerHolyShieldIfApplicable(): Boolean {
        val nonAttackerPlayers = updatedPlayerMap.values.filter { it.userId != attackerUserId }

        return nonAttackerPlayers.any { player ->
            player.characterClass.triggerHolyShieldIfApplicable(holyShieldUserId = player.userId)
        }
    }

    private fun CharacterClass.triggerHolyShieldIfApplicable(holyShieldUserId: String): Boolean {
        if (isConscious()) return false

        return HolyShieldHandler.triggerHolyShieldIfApplicable(
            combatMutator = combatMutator,
            defenderDefensiveAbility = defensiveAbility,
            holyShieldUserId = holyShieldUserId,
        )
    }
}
