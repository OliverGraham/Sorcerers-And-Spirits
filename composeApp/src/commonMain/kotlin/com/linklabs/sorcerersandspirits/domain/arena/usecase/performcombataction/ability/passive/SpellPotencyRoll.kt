package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

/**
 * Resolves the Mage's Spell Potency: when the Mage rolls doubles to pass armor or deal damage, the
 * [diceTotal] is dealt to the main target and half of it (rounded down) splashes to every other
 * conscious opponent, then the turn ends.
 *
 * The casting Mage and the main target are excluded from the splash. Applying the damage can knock
 * a Paladin out, so the turn only ends if a downed player's Holy Shield does not trigger.
 */
internal class SpellPotencyRoll(
    private val combatMutator: CombatMutator,
    private val diceTotal: Int,
) {
    operator fun invoke() = combatMutator.resolveSpellPotency()

    private fun CombatMutator.resolveSpellPotency() {
        val splashTargets = splashTargets()
        val splashDamage =
            if (splashTargets.isEmpty()) {
                0
            } else {
                diceTotal / abilityConfig.spellPotencySplashDivisor
            }

        addSpellPotencyMessage(splashTargets = splashTargets, splashDamage = splashDamage)
        applyFullDamageToDefender()
        applySplashDamage(splashTargets = splashTargets, splashDamage = splashDamage)

        if (!triggerHolyShieldIfApplicable()) {
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.splashTargets() =
        consciousPlayers.filter { it.userId != attackerUserId && it.userId != defenderUserId }

    private fun CombatMutator.applyFullDamageToDefender() {
        updateDefenderClass { defender -> defender.takeDamage(damage = diceTotal) }
    }

    private fun CombatMutator.applySplashDamage(
        splashTargets: List<ArenaPlayer.Selected>,
        splashDamage: Int,
    ) {
        splashTargets.forEach { player ->
            updatePlayerClass(userId = player.userId) { playerClass ->
                playerClass.takeDamage(damage = splashDamage)
            }
        }
    }

    private fun CombatMutator.addSpellPotencyMessage(
        splashTargets: List<ArenaPlayer.Selected>,
        splashDamage: Int,
    ) {
        addMessage(
            message =
                ArenaMessage.SpellPotency(
                    mageUserId = attackerUserId,
                    mainTargetUserId = defenderUserId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                    splashTargetUserIds = splashTargets.map { it.userId },
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
