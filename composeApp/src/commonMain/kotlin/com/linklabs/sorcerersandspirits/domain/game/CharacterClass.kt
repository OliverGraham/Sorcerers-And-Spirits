package com.linklabs.sorcerersandspirits.domain.game

import kotlin.Int

data class CharacterClass(
    val type: CharacterClassType,
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbility,
    val defensiveAbility: DefensiveAbility,
    val hasActedThisRound: Boolean = false,
) {
    val className = type.name

    fun isConscious() = hitPoints > 0

    companion object {
        /**
         * The classes a player can pick, in display order: every [CharacterClassType] except
         * [CharacterClassType.None].
         */
        fun playableTypes(): List<CharacterClassType> =
            CharacterClassType.entries.filter { type -> type != CharacterClassType.None }

        /**
         * Builds a class from its remotely-tuned stats, wiring the fixed abilities that belong to
         * [type]. Stats are the only tunable data and always come from config; the ability wiring
         * is code, resolved through [offensiveAbilityFor]/[defensiveAbilityFor].
         */
        fun create(
            type: CharacterClassType,
            hitPoints: Int,
            maxHitPoints: Int,
            armorClass: Int,
            speed: Int,
        ): CharacterClass =
            CharacterClass(
                type = type,
                hitPoints = hitPoints,
                maxHitPoints = maxHitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbilityFor(type = type),
                defensiveAbility = defensiveAbilityFor(type = type),
            )

        private fun offensiveAbilityFor(type: CharacterClassType): OffensiveAbility =
            when (type) {
                CharacterClassType.Rogue -> OffensiveAbility.KillingMomentum
                CharacterClassType.Gambler -> OffensiveAbility.AllIn()
                CharacterClassType.Duelist -> OffensiveAbility.PrepareAttack
                CharacterClassType.Necromancer -> OffensiveAbility.SummonMinion()
                CharacterClassType.Bard -> OffensiveAbility.Encore
                CharacterClassType.Illusionist -> OffensiveAbility.Impersonate()
                CharacterClassType.Mage -> OffensiveAbility.SpellPotency
                CharacterClassType.Paladin -> OffensiveAbility.None
                CharacterClassType.None -> OffensiveAbility.None
            }

        private fun defensiveAbilityFor(type: CharacterClassType): DefensiveAbility =
            when (type) {
                CharacterClassType.Rogue -> DefensiveAbility.Dodge()
                CharacterClassType.Gambler -> DefensiveAbility.LuckyCoin
                CharacterClassType.Duelist -> DefensiveAbility.Riposte
                CharacterClassType.Necromancer -> DefensiveAbility.None
                CharacterClassType.Bard -> DefensiveAbility.Persuade
                CharacterClassType.Illusionist -> DefensiveAbility.MirrorImage()
                CharacterClassType.Mage -> DefensiveAbility.Teleport
                CharacterClassType.Paladin -> DefensiveAbility.HolyShield()
                CharacterClassType.None -> DefensiveAbility.None
            }
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
