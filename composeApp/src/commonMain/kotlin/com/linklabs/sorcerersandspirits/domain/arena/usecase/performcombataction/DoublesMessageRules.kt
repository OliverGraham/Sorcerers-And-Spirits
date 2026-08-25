package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective
import com.linklabs.sorcerersandspirits.domain.game.minionAttacks

internal fun CombatMutator.shouldMessageDoublesAfterArmorClassHit(): Boolean = when (val defensiveAbility = defender.defensiveAbility) {
    is DefensiveAbility.Teleport,
    is DefensiveAbility.Dodge,
    is DefensiveAbility.HolyShield,
    is DefensiveAbility.None,
    -> true

    is DefensiveAbility.Riposte,
    is DefensiveAbility.Persuade,
    is DefensiveAbility.LuckyCoin,
    -> false

    is DefensiveAbility.MirrorImage -> defensiveAbility.usedThisAttack
}

internal fun CombatMutator.shouldMessageDoublesAfterKnockout(): Boolean {
    val isHolyShieldPending =
        HolyShieldHandler.isHolyShieldAvailable(
            defenderDefensiveAbility = defender.defensiveAbility,
        )
    if (isHolyShieldPending) return false

    if (consciousPlayers.size <= 1) return false

    val attackerAbility = attacker.offensiveAbility.effective
    val isFollowUpRollEarned =
        attackerAbility == OffensiveAbility.KillingMomentum ||
            attackerAbility == OffensiveAbility.Encore

    return isFollowUpRollEarned || hasPendingExtraAttack()
}

internal fun CombatMutator.shouldMessageDoublesAfterDamage(): Boolean {
    val isEncoreEarned = attacker.offensiveAbility.effective == OffensiveAbility.Encore

    return isEncoreEarned || hasPendingExtraAttack()
}

/**
 * An extra attack keeps the dice with the attacker only while the turn is theirs; after a Riposte
 * reversal the Duelist is the attacker, so the turn player's pending attacks would change hands.
 */
internal fun CombatMutator.hasPendingExtraAttack(): Boolean {
    val isAttackerTheTurnPlayer = attackerUserId == originalTurnUserId
    if (!isAttackerTheTurnPlayer) return false

    return when (val offensiveAbility = attacker.offensiveAbility.effective) {
        is OffensiveAbility.AllIn -> offensiveAbility.hasAnotherAttack

        is OffensiveAbility.SummonMinion ->
            offensiveAbility.attacksTakenThisTurn < offensiveAbility.minionAttacks

        else -> false
    }
}
