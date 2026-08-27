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
    val hasActedThisRound: Boolean,
    val attacksRemaining: Int = 1,
) {
    val className = type.name

    fun isConscious() = hitPoints > 0

    // TODO: Make another method that handles this from elsewhere. Also, branch off Necromancer
    //       only code
    fun takeDamage(damage: Int): CharacterClass {
        val minions =
            offensiveAbility.effective as? OffensiveAbility.SummonMinion
                ?: return copy(hitPoints = hitPoints - damage)

        val absorbedByShield = minOf(damage, minions.shieldPoints)
        val survivingShield = minions.shieldPoints - absorbedByShield
        val survivingGolems = minOf(minions.golemCount, survivingShield / 2)
        val survivingSkeletons = survivingShield - survivingGolems * 2

        val survivingMinions =
            minions.copy(skeletonCount = survivingSkeletons, golemCount = survivingGolems)
        return copy(
            hitPoints = hitPoints - (damage - absorbedByShield),
            offensiveAbility = offensiveAbility.withEffective(survivingMinions),
        )
    }

    companion object {
        fun playableTypes(): List<CharacterClassType> = CharacterClassType.entries.filter { type ->
            type !=
                CharacterClassType.None
        }

        fun create(
            type: CharacterClassType,
            hitPoints: Int,
            maxHitPoints: Int,
            armorClass: Int,
            speed: Int,
        ): CharacterClass = CharacterClass(
            type = type,
            hitPoints = hitPoints,
            maxHitPoints = maxHitPoints,
            armorClass = armorClass,
            speed = speed,
            offensiveAbility = offensiveAbilityFor(type = type),
            defensiveAbility = defensiveAbilityFor(type = type),
            hasActedThisRound = false,
        )

        private fun offensiveAbilityFor(type: CharacterClassType): OffensiveAbility = when (type) {
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

        private fun defensiveAbilityFor(type: CharacterClassType): DefensiveAbility = when (type) {
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
