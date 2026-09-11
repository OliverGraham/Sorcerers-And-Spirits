package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass

internal class SpellPotencyHandler(
    private val combatMutator: CombatMutator,
    private val firstDie: Int,
    private val secondDie: Int,
    private val diceTotal: Int,
) {
    fun handleSpellPotency() = combatMutator.handleSpellPotency()

    private fun CombatMutator.handleSpellPotency() {
        val splashTargets = splashTargets()
        val splashDamage =
            if (splashTargets.isEmpty()) {
                0
            } else {
                diceTotal / abilityConfig.spellPotencySplashDivisor
            }

        applyFullDamageToDefender()
        addSpellPotencyMessage(splashTargets = splashTargets, splashDamage = splashDamage)
        applySplashDamage(splashTargets = splashTargets, splashDamage = splashDamage)

        if (!triggerHolyShieldIfApplicable()) {
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.splashTargets() = consciousPlayers.filter {
        it.userId !=
            attackerUserId &&
            it.userId != defenderUserId
    }

    private fun CombatMutator.applyFullDamageToDefender() {
        takeDamage(damage = diceTotal, userId = defenderUserId)
    }

    private fun CombatMutator.applySplashDamage(
        splashTargets: List<ArenaPlayer.Selected>,
        splashDamage: Int,
    ) {
        splashTargets.forEach { player ->
            takeDamage(damage = splashDamage, userId = player.userId)
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
                mageName = attacker.className,
                mainTargetUserId = defenderUserId,
                mainTargetClassName = defender.className,
                mainTargetClassType = defender.type,
                firstDie = firstDie,
                secondDie = secondDie,
                mainDamage = diceTotal,
                splashDamage = splashDamage,
                splashTargetUserIds = splashTargets.map { it.userId },
                isMainTargetKnockedOut = !defender.isConscious(),
            ),
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
