package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.Serializable

@Serializable
sealed interface CharacterClassEntity {
    val hitPoints: Int
    val armorClass: Int
    val speed: Int
    val offensiveAbility: OffensiveAbility
    val defensiveAbility: DefensiveAbility
    val className: String

    @Serializable
    data class Rogue(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassEntity

    @Serializable
    data class Mage(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassEntity

    @Serializable
    data class Paladin(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassEntity
}

internal fun CharacterClassEntity.toDomain() =
    when (this) {
        is CharacterClassEntity.Rogue ->
            CharacterClass.Rogue(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClassEntity.Mage ->
            CharacterClass.Mage(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClassEntity.Paladin ->
            CharacterClass.Paladin(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
    }

internal fun CharacterClass.toEntity() =
    when (this) {
        is CharacterClass.Rogue ->
            CharacterClassEntity.Rogue(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClass.Mage ->
            CharacterClassEntity.Mage(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClass.Paladin ->
            CharacterClassEntity.Paladin(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
    }
