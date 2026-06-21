package com.linklabs.sorcerersandspirits.domain.game

/**
 * The remotely-tunable roll thresholds for the class special abilities. These replace the constants
 * that used to live inside each ability handler so combat balance can be tuned from RemoteConfig
 * without a release (SS-058).
 *
 * Out of scope here: Teleport (a pure roll-off, defender-beats-attacker, with no threshold to
 * tune), the Mirror Image -1 reroll adjustment (it lives on the serialized [AbilityAction] model,
 * not in a handler), and the doubles +1 bonus (a normal-combat rule, not an ability threshold).
 *
 * [DEFAULT] is the canonical shipped balance and the ultimate safety net when no remote/bundled
 * value can be resolved. The bundled JSON default mirrors it (see ability_config.json).
 */
data class AbilityConfig(
    val holyShieldSuccessRange: IntRange,
    val riposteSuccessRange: IntRange,
    val prepareAttackKnockoutRange: IntRange,
    val prepareAttackBypassHolyShieldRoll: Int,
    val allInSelfKnockoutRoll: Int,
    val allInAttackTwiceRange: IntRange,
    val summonSkeletonRange: IntRange,
    val summonSkeletonAndHealRange: IntRange,
    val summonGolemRoll: Int,
    val encoreSuccessRange: IntRange,
    val persuadeLullRoll: Int,
    val persuadeCharmRoll: Int,
    val luckyCoinHighRollerRoll: Int,
    val dodgeMaxArmorBonus: Int,
    val spellPotencySplashDivisor: Int,
) {
    companion object {
        val DEFAULT =
            AbilityConfig(
                holyShieldSuccessRange = 10..12,
                riposteSuccessRange = 6..8,
                prepareAttackKnockoutRange = 10..12,
                prepareAttackBypassHolyShieldRoll = 12,
                allInSelfKnockoutRoll = 1,
                allInAttackTwiceRange = 5..6,
                summonSkeletonRange = 2..3,
                summonSkeletonAndHealRange = 4..5,
                summonGolemRoll = 6,
                encoreSuccessRange = 5..6,
                persuadeLullRoll = 5,
                persuadeCharmRoll = 6,
                luckyCoinHighRollerRoll = 6,
                dodgeMaxArmorBonus = 2,
                spellPotencySplashDivisor = 2,
            )
    }
}
