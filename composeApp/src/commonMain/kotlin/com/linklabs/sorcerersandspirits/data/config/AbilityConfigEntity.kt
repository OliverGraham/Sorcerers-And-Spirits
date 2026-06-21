package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.AbilityConfig
import kotlinx.serialization.Serializable

/**
 * The remotely-tunable ability roll thresholds as carried in the RemoteConfig payload. Mirrors
 * every field of the domain [AbilityConfig]; ranges are serialized as [min, max] pairs and mapped
 * back to [IntRange] in [toDomain].
 */
@Serializable
data class AbilityConfigEntity(
    val holyShieldSuccessRange: IntRangeEntity,
    val riposteSuccessRange: IntRangeEntity,
    val prepareAttackKnockoutRange: IntRangeEntity,
    val prepareAttackBypassHolyShieldRoll: Int,
    val allInSelfKnockoutRoll: Int,
    val allInAttackTwiceRange: IntRangeEntity,
    val summonSkeletonRange: IntRangeEntity,
    val summonSkeletonAndHealRange: IntRangeEntity,
    val summonGolemRoll: Int,
    val encoreSuccessRange: IntRangeEntity,
    val persuadeLullRoll: Int,
    val persuadeCharmRoll: Int,
    val luckyCoinHighRollerRoll: Int,
    val dodgeMaxArmorBonus: Int,
    val spellPotencySplashDivisor: Int,
) {
    fun toDomain() =
        AbilityConfig(
            holyShieldSuccessRange = holyShieldSuccessRange.toRange(),
            riposteSuccessRange = riposteSuccessRange.toRange(),
            prepareAttackKnockoutRange = prepareAttackKnockoutRange.toRange(),
            prepareAttackBypassHolyShieldRoll = prepareAttackBypassHolyShieldRoll,
            allInSelfKnockoutRoll = allInSelfKnockoutRoll,
            allInAttackTwiceRange = allInAttackTwiceRange.toRange(),
            summonSkeletonRange = summonSkeletonRange.toRange(),
            summonSkeletonAndHealRange = summonSkeletonAndHealRange.toRange(),
            summonGolemRoll = summonGolemRoll,
            encoreSuccessRange = encoreSuccessRange.toRange(),
            persuadeLullRoll = persuadeLullRoll,
            persuadeCharmRoll = persuadeCharmRoll,
            luckyCoinHighRollerRoll = luckyCoinHighRollerRoll,
            dodgeMaxArmorBonus = dodgeMaxArmorBonus,
            spellPotencySplashDivisor = spellPotencySplashDivisor,
        )

    companion object {
        /**
         * The serialized form of [AbilityConfig.DEFAULT], used as the in-memory seed before the
         * bundled JSON loads so [RemoteConfigDataSource.abilityConfig] always has a valid value.
         */
        fun fromDomain(config: AbilityConfig) =
            AbilityConfigEntity(
                holyShieldSuccessRange = config.holyShieldSuccessRange.toEntity(),
                riposteSuccessRange = config.riposteSuccessRange.toEntity(),
                prepareAttackKnockoutRange = config.prepareAttackKnockoutRange.toEntity(),
                prepareAttackBypassHolyShieldRoll = config.prepareAttackBypassHolyShieldRoll,
                allInSelfKnockoutRoll = config.allInSelfKnockoutRoll,
                allInAttackTwiceRange = config.allInAttackTwiceRange.toEntity(),
                summonSkeletonRange = config.summonSkeletonRange.toEntity(),
                summonSkeletonAndHealRange = config.summonSkeletonAndHealRange.toEntity(),
                summonGolemRoll = config.summonGolemRoll,
                encoreSuccessRange = config.encoreSuccessRange.toEntity(),
                persuadeLullRoll = config.persuadeLullRoll,
                persuadeCharmRoll = config.persuadeCharmRoll,
                luckyCoinHighRollerRoll = config.luckyCoinHighRollerRoll,
                dodgeMaxArmorBonus = config.dodgeMaxArmorBonus,
                spellPotencySplashDivisor = config.spellPotencySplashDivisor,
            )
    }
}

@Serializable
data class IntRangeEntity(val min: Int, val max: Int) {
    fun toRange(): IntRange = min..max
}

private fun IntRange.toEntity() = IntRangeEntity(min = first, max = last)
