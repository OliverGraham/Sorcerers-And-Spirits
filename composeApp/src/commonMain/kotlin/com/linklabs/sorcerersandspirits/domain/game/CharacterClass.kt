package com.linklabs.sorcerersandspirits.domain.game

import kotlin.Int

// TODO(SS-046): Add a maximum HP
data class CharacterClass(
    val type: CharacterClassType,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbility,
    val defensiveAbility: DefensiveAbility,
    val hasActedThisRound: Boolean = false,
) {
    val className = type.name

    fun isConscious() = hitPoints > 0

    companion object {
        fun getAllClasses() =
            listOf(
                getRogue(),
                getGambler(),
                getDuelist(),
                getNecromancer(),
                getBard(),
                getIllusionist(),
                getMage(),
                getPaladin(),
            )

        // TODO(SS-047): Get CharacterClass values from RemoteConfig

        fun getRogue() =
            CharacterClass(
                type = CharacterClassType.Rogue,
                hitPoints = 5,
                armorClass = 6,
                speed = 8,
                offensiveAbility = OffensiveAbility.KillingMomentum,
                defensiveAbility = DefensiveAbility.Dodge(),
            )

        fun getGambler() =
            CharacterClass(
                type = CharacterClassType.Gambler,
                hitPoints = 3,
                armorClass = 4,
                speed = 7,
                offensiveAbility = OffensiveAbility.AllIn(),
                defensiveAbility = DefensiveAbility.LuckyCoin,
            )

        fun getDuelist() =
            CharacterClass(
                type = CharacterClassType.Duelist,
                hitPoints = 4,
                armorClass = 4,
                speed = 6,
                offensiveAbility = OffensiveAbility.PrepareAttack,
                defensiveAbility = DefensiveAbility.Riposte,
            )

        fun getNecromancer() =
            CharacterClass(
                type = CharacterClassType.Necromancer,
                hitPoints = 7,
                armorClass = 7,
                speed = 5,
                offensiveAbility = OffensiveAbility.SummonMinion(),
                defensiveAbility = DefensiveAbility.None,
            )

        fun getBard() =
            CharacterClass(
                type = CharacterClassType.Bard,
                hitPoints = 5,
                armorClass = 5,
                speed = 4,
                offensiveAbility = OffensiveAbility.Encore,
                defensiveAbility = DefensiveAbility.Persuade,
            )

        fun getIllusionist() =
            CharacterClass(
                type = CharacterClassType.Illusionist,
                hitPoints = 5,
                armorClass = 5,
                speed = 3,
                offensiveAbility = OffensiveAbility.Impersonate(),
                defensiveAbility = DefensiveAbility.MirrorImage(),
            )

        fun getMage() =
            CharacterClass(
                type = CharacterClassType.Mage,
                hitPoints = 4,
                armorClass = 4,
                speed = 2,
                offensiveAbility = OffensiveAbility.SpellPotency,
                defensiveAbility = DefensiveAbility.Teleport,
            )

        fun getPaladin() =
            CharacterClass(
                type = CharacterClassType.Paladin,
                hitPoints = 7,
                armorClass = 7,
                speed = 1,
                offensiveAbility = OffensiveAbility.None,
                defensiveAbility = DefensiveAbility.HolyShield(),
            )
    }
}

enum class CharacterClassType {
    Rogue,
    Gambler,
    Duelist,
    Necromancer,
    Bard,
    Illusionist,
    Mage,
    Paladin,
    None,
}
