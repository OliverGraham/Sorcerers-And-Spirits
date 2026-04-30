package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.Serializable

@Serializable
enum class CharacterClassType {
    Rogue,
    Mage,
    Paladin,
}

@Serializable
data class CharacterClassEntity(
    val type: CharacterClassType = CharacterClassType.Rogue,
    val hitPoints: Int = 0,
    val armorClass: Int = 0,
    val speed: Int = 0,
    val offensiveAbility: OffensiveAbilityEntity = OffensiveAbilityEntity.KillingMomentum,
    val defensiveAbility: DefensiveAbilityEntity = DefensiveAbilityEntity.Anticipation,
    val className: String = "",
)

internal fun CharacterClassEntity.toDomain(): CharacterClass =
    when (type) {
        CharacterClassType.Rogue ->
            CharacterClass.Rogue(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toDomain(),
                defensiveAbility = defensiveAbility.toDomain(),
                className = className,
            )

        CharacterClassType.Mage ->
            CharacterClass.Mage(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toDomain(),
                defensiveAbility = defensiveAbility.toDomain(),
                className = className,
            )

        CharacterClassType.Paladin ->
            CharacterClass.Paladin(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toDomain(),
                defensiveAbility = defensiveAbility.toDomain(),
                className = className,
            )
    }

internal fun CharacterClass.toEntity(): CharacterClassEntity =
    when (this) {
        is CharacterClass.Rogue ->
            CharacterClassEntity(
                type = CharacterClassType.Rogue,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toEntity(),
                defensiveAbility = defensiveAbility.toEntity(),
                className = className,
            )

        is CharacterClass.Mage ->
            CharacterClassEntity(
                type = CharacterClassType.Mage,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toEntity(),
                defensiveAbility = defensiveAbility.toEntity(),
                className = className,
            )

        is CharacterClass.Paladin ->
            CharacterClassEntity(
                type = CharacterClassType.Paladin,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility.toEntity(),
                defensiveAbility = defensiveAbility.toEntity(),
                className = className,
            )
    }

@Serializable
enum class OffensiveAbilityEntity {
    KillingMomentum,
    SpellNova,
    ReflectiveShield,
}

@Serializable
enum class DefensiveAbilityEntity {
    Anticipation,
    Blink,
    HolyPrayer,
}

internal fun OffensiveAbility.toEntity() =
    when (this) {
        OffensiveAbility.KillingMomentum -> OffensiveAbilityEntity.KillingMomentum
        OffensiveAbility.SpellNova -> OffensiveAbilityEntity.SpellNova
        OffensiveAbility.ReflectiveShield -> OffensiveAbilityEntity.ReflectiveShield
    }

internal fun DefensiveAbility.toEntity() =
    when (this) {
        DefensiveAbility.Anticipation -> DefensiveAbilityEntity.Anticipation
        DefensiveAbility.Blink -> DefensiveAbilityEntity.Blink
        DefensiveAbility.HolyPrayer -> DefensiveAbilityEntity.HolyPrayer
    }

internal fun OffensiveAbilityEntity.toDomain() =
    when (this) {
        OffensiveAbilityEntity.KillingMomentum -> OffensiveAbility.KillingMomentum
        OffensiveAbilityEntity.SpellNova -> OffensiveAbility.SpellNova
        OffensiveAbilityEntity.ReflectiveShield -> OffensiveAbility.ReflectiveShield
    }

internal fun DefensiveAbilityEntity.toDomain() =
    when (this) {
        DefensiveAbilityEntity.Anticipation -> DefensiveAbility.Anticipation
        DefensiveAbilityEntity.Blink -> DefensiveAbility.Blink
        DefensiveAbilityEntity.HolyPrayer -> DefensiveAbility.HolyPrayer
    }
