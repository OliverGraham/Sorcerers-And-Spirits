package com.linklabs.sorcerersandspirits.domain.game

/**
 * Factory-style class builders with the canonical stat values, for tests only. Production builds
 * classes from RemoteConfig (see [CharacterClass.create]); tests use these fixed values so combat
 * assertions stay deterministic without standing up RemoteConfig. Stats here mirror the bundled
 * character_class_stats.json; ability wiring is reused from production via [CharacterClass.create].
 */
object TestCharacterClasses {

    fun all(): List<CharacterClass> =
        listOf(
            rogue(),
            gambler(),
            duelist(),
            necromancer(),
            bard(),
            illusionist(),
            mage(),
            paladin(),
        )

    fun rogue(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Rogue,
            hitPoints = 5,
            maxHitPoints = 5,
            armorClass = 6,
            speed = 8,
        )

    fun gambler(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Gambler,
            hitPoints = 3,
            maxHitPoints = 3,
            armorClass = 4,
            speed = 7,
        )

    fun duelist(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Duelist,
            hitPoints = 4,
            maxHitPoints = 4,
            armorClass = 4,
            speed = 6,
        )

    fun necromancer(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Necromancer,
            hitPoints = 7,
            maxHitPoints = 7,
            armorClass = 7,
            speed = 5,
        )

    fun bard(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Bard,
            hitPoints = 5,
            maxHitPoints = 5,
            armorClass = 5,
            speed = 4,
        )

    fun illusionist(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Illusionist,
            hitPoints = 5,
            maxHitPoints = 5,
            armorClass = 5,
            speed = 3,
        )

    fun mage(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Mage,
            hitPoints = 4,
            maxHitPoints = 4,
            armorClass = 4,
            speed = 2,
        )

    fun paladin(): CharacterClass =
        CharacterClass.create(
            type = CharacterClassType.Paladin,
            hitPoints = 7,
            maxHitPoints = 7,
            armorClass = 7,
            speed = 1,
        )
}
