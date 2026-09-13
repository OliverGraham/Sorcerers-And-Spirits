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

        addSpellPotencyMessage(splashTargets = splashTargets, splashDamage = splashDamage)
        applyFullDamageToDefender()
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
        applyDamageWithMinionShield(damage = diceTotal, userId = defenderUserId)
    }

    private fun CombatMutator.applySplashDamage(
        splashTargets: List<ArenaPlayer.Selected>,
        splashDamage: Int,
    ) {
        splashTargets.forEach { player ->
            applyDamageWithMinionShield(damage = splashDamage, userId = player.userId)
        }
    }

    private fun CombatMutator.applyDamageWithMinionShield(damage: Int, userId: String) {
        val hadAbsorbingMinions = hasAbsorbingMinions(userId = userId)
        val damageAfterMinionShield = takeDamage(damage = damage, userId = userId)
        if (hadAbsorbingMinions && damageAfterMinionShield > 0) {
            addMinionShieldResidualDamage(
                summonerUserId = userId,
                damageAfterMinionShield = damageAfterMinionShield,
            )
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
                isMainTargetKnockedOut = wouldBeKnockedOut(damage = diceTotal, userId = defenderUserId),
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
