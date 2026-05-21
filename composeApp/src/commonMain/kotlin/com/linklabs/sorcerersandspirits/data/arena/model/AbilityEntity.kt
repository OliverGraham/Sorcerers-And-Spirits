package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.Anticipation
import com.linklabs.sorcerersandspirits.domain.game.Blink
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.HolyPrayer
import com.linklabs.sorcerersandspirits.domain.game.KillingMomentum
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.PrepareAnAttack
import com.linklabs.sorcerersandspirits.domain.game.ReflectiveShield
import com.linklabs.sorcerersandspirits.domain.game.SpellNova
import kotlinx.serialization.Serializable

@Serializable sealed interface AbilityEntity

@Serializable sealed interface DefensiveAbilityEntity : AbilityEntity

@Serializable sealed interface OffensiveAbilityEntity : AbilityEntity

@Serializable data object KillingMomentumEntity : OffensiveAbilityEntity

@Serializable data object SpellNovaEntity : OffensiveAbilityEntity

@Serializable data object ReflectiveShieldEntity : OffensiveAbilityEntity

@Serializable data object BlinkEntity : DefensiveAbilityEntity

@Serializable data object AnticipationEntity : DefensiveAbilityEntity

@Serializable data object HolyPrayerEntity : DefensiveAbilityEntity

@Serializable data object PrepareAnAttackEntity : OffensiveAbilityEntity

internal fun OffensiveAbility.toEntity(): OffensiveAbilityEntity =
    when (this) {
        is KillingMomentum -> KillingMomentumEntity
        is SpellNova -> SpellNovaEntity
        is ReflectiveShield -> ReflectiveShieldEntity
        is PrepareAnAttack -> PrepareAnAttackEntity
    }

internal fun DefensiveAbility.toEntity(): DefensiveAbilityEntity =
    when (this) {
        is Blink -> BlinkEntity
        is Anticipation -> AnticipationEntity
        is HolyPrayer -> HolyPrayerEntity
    }

internal fun OffensiveAbilityEntity.toDomain(): OffensiveAbility =
    when (this) {
        is KillingMomentumEntity -> KillingMomentum
        is SpellNovaEntity -> SpellNova
        is ReflectiveShieldEntity -> ReflectiveShield
        is PrepareAnAttackEntity -> PrepareAnAttack
    }

internal fun DefensiveAbilityEntity.toDomain(): DefensiveAbility =
    when (this) {
        is BlinkEntity -> Blink
        is AnticipationEntity -> Anticipation
        is HolyPrayerEntity -> HolyPrayer
    }
