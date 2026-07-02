package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective
import com.linklabs.sorcerersandspirits.domain.game.minionAttacks

/**
 * The Doubles message announces the +1 bonus, so it is only added when the attacker's next roll is
 * consecutive: the dice stay in their hands. A defender ability that rolls in between (Riposte,
 * Persuade, Lucky Coin) nullifies the bonus, while Teleport's roll-off die and the attacker's own
 * Health roll keep it.
 *
 * An unused Mirror Image defers the decision to the Illusionist: the message is suppressed here and
 * added only if the Illusionist keeps the roll.
 */
internal fun CombatMutator.shouldMessageDoublesAfterArmorClassHit(): Boolean =
    when (val defensiveAbility = defender.defensiveAbility) {
        is DefensiveAbility.Teleport,
        is DefensiveAbility.Dodge,
        is DefensiveAbility.HolyShield,
        is DefensiveAbility.None -> true

        is DefensiveAbility.Riposte,
        is DefensiveAbility.Persuade,
        is DefensiveAbility.LuckyCoin -> false

        is DefensiveAbility.MirrorImage -> defensiveAbility.usedThisAttack
    }

internal fun CombatMutator.shouldMessageDoublesAfterArmorClassMiss(): Boolean =
    hasPendingExtraAttack()

internal fun CombatMutator.shouldMessageDoublesAfterKnockout(): Boolean {
    val isHolyShieldPending =
        HolyShieldHandler.isHolyShieldAvailable(
            defenderDefensiveAbility = defender.defensiveAbility
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
private fun CombatMutator.hasPendingExtraAttack(): Boolean {
    val isAttackerTheTurnPlayer = attackerUserId == originalTurnUserId
    if (!isAttackerTheTurnPlayer) return false

    return when (val offensiveAbility = attacker.offensiveAbility.effective) {
        is OffensiveAbility.AllIn -> offensiveAbility.hasAnotherAttack

        is OffensiveAbility.SummonMinion ->
            offensiveAbility.attacksTakenThisTurn < offensiveAbility.minionAttacks

        else -> false
    }
}
