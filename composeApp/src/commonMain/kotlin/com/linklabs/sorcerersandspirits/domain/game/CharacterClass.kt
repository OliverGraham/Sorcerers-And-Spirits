package com.linklabs.sorcerersandspirits.domain.game

import kotlin.Int

data class CharacterClass(
    val type: CharacterClassType,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbility,
    val defensiveAbility: DefensiveAbility,
) {
    val className = type.name

    companion object {
        fun getAllClasses() = listOf(getRogue(), getMage(), getPaladin())

        // TODO: Get CharacterClass values from RemoteConfig

        fun getRogue() =
            CharacterClass(
                type = CharacterClassType.Rogue,
                hitPoints = 6,
                armorClass = 5,
                speed = 8,
                offensiveAbility = OffensiveAbility.KillingMomentum,
                defensiveAbility = DefensiveAbility.Anticipation,
            )

        fun getMage() =
            CharacterClass(
                type = CharacterClassType.Mage,
                hitPoints = 4,
                armorClass = 4,
                speed = 2,
                offensiveAbility = OffensiveAbility.SpellNova,
                defensiveAbility = DefensiveAbility.Blink,
            )

        fun getPaladin() =
            CharacterClass(
                type = CharacterClassType.Paladin,
                hitPoints = 7,
                armorClass = 7,
                speed = 1,
                offensiveAbility = OffensiveAbility.ReflectiveShield,
                defensiveAbility = DefensiveAbility.HolyPrayer,
            )

        fun fromType(type: CharacterClassType) =
            when (type) {
                CharacterClassType.Rogue -> getRogue()
                CharacterClassType.Mage -> getMage()
                CharacterClassType.Paladin -> getPaladin()
            }
    }
}

enum class CharacterClassType {
    Rogue,
    Mage,
    Paladin,
}

// TODO replace enum class OffensiveAbility and enum class DefensiveAbility with these definitions
/*
sealed interface Ability
sealed interface ActivatedAbility : Ability
sealed interface DefensiveAbility : Ability
sealed interface OffensiveAbility : Ability
sealed interface PassiveAbility : Ability

data object KillingMomentum : OffensiveAbility, PassiveAbility
data object SpellNova : OffensiveAbility, PassiveAbility
data object ReflectiveShield : OffensiveAbility, PassiveAbility

data object Blink: DefensiveAbility, PassiveAbility

data object PrepareAnAttack : OffensiveAbility, ActivatedAbility
*/

enum class OffensiveAbility {
    KillingMomentum,
    SpellNova,
    ReflectiveShield,
}

enum class DefensiveAbility {
    Anticipation,
    Blink,
    HolyPrayer,
}
