package com.linklabs.sorcerersandspirits.domain.game

sealed interface CharacterClass {
    val hitPoints: Int
    val armorClass: Int
    val speed: Int
    val offensiveAbility: OffensiveAbility
    val defensiveAbility: DefensiveAbility
    val className: String

    data class Rogue(
        override val hitPoints: Int = 6,
        override val armorClass: Int = 5,
        override val speed: Int = 8,
        override val offensiveAbility: OffensiveAbility = OffensiveAbility.KillingMomentum,
        override val defensiveAbility: DefensiveAbility = DefensiveAbility.Anticipation,
        override val className: String = ROGUE,
    ) : CharacterClass

    data class Mage(
        override val hitPoints: Int = 4,
        override val armorClass: Int = 4,
        override val speed: Int = 2,
        override val offensiveAbility: OffensiveAbility = OffensiveAbility.SpellNova,
        override val defensiveAbility: DefensiveAbility = DefensiveAbility.Blink,
        override val className: String = MAGE,
    ) : CharacterClass

    data class Paladin(
        override val hitPoints: Int = 7,
        override val armorClass: Int = 7,
        override val speed: Int = 1,
        override val offensiveAbility: OffensiveAbility = OffensiveAbility.ReflectiveShield,
        override val defensiveAbility: DefensiveAbility = DefensiveAbility.HolyPrayer,
        override val className: String = PALADIN,
    ) : CharacterClass

    companion object {
        fun getAllClasses() = listOf(Rogue(), Mage(), Paladin())

        private const val ROGUE = "Rogue"
        private const val MAGE = "Mage"
        private const val PALADIN = "Paladin"
    }
}

sealed interface OffensiveAbility {
    data object KillingMomentum : OffensiveAbility

    data object SpellNova : OffensiveAbility

    data object ReflectiveShield : OffensiveAbility
}

sealed interface DefensiveAbility {
    data object Anticipation : DefensiveAbility

    data object Blink : DefensiveAbility

    data object HolyPrayer : DefensiveAbility
}
