package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.Ability
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

@Serializable sealed interface ActivatedAbilityEntity : AbilityEntity

@Serializable sealed interface DefensiveAbilityEntity : AbilityEntity

@Serializable sealed interface OffensiveAbilityEntity : AbilityEntity

@Serializable sealed interface PassiveAbilityEntity : AbilityEntity

@Serializable data object KillingMomentumEntity : OffensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object SpellNovaEntity : OffensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object ReflectiveShieldEntity : OffensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object BlinkEntity : DefensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object AnticipationEntity : DefensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object HolyPrayerEntity : DefensiveAbilityEntity, PassiveAbilityEntity

@Serializable data object PrepareAnAttackEntity : OffensiveAbilityEntity, ActivatedAbilityEntity

internal fun Ability.toEntity() =
    when (this) {
        PrepareAnAttack -> PrepareAnAttackEntity
        is Anticipation -> AnticipationEntity
        Blink -> BlinkEntity
        HolyPrayer -> HolyPrayerEntity
        KillingMomentum -> KillingMomentumEntity
        ReflectiveShield -> ReflectiveShieldEntity
        SpellNova -> SpellNovaEntity
    }

internal fun AbilityEntity.toDomain() =
    when (this) {
        PrepareAnAttackEntity -> PrepareAnAttack
        AnticipationEntity -> Anticipation()
        BlinkEntity -> Blink
        HolyPrayerEntity -> HolyPrayer
        KillingMomentumEntity -> KillingMomentum
        ReflectiveShieldEntity -> ReflectiveShield
        SpellNovaEntity -> SpellNova
    }

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
        is AnticipationEntity -> Anticipation()
        is HolyPrayerEntity -> HolyPrayer
    }
