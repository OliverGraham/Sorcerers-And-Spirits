package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

enum class DieColor {
    NormalAttack,
    Disabled,
    Rogue,
    Gambler,
    Duelist,
    Necromancer,
    Bard,
    Illusionist,
    Mage,
    Paladin,
}

fun CharacterClassType.toDieColor(): DieColor = when (this) {
    CharacterClassType.Rogue -> DieColor.Rogue
    CharacterClassType.Gambler -> DieColor.Gambler
    CharacterClassType.Duelist -> DieColor.Duelist
    CharacterClassType.Necromancer -> DieColor.Necromancer
    CharacterClassType.Bard -> DieColor.Bard
    CharacterClassType.Illusionist -> DieColor.Illusionist
    CharacterClassType.Mage -> DieColor.Mage
    CharacterClassType.Paladin -> DieColor.Paladin
    CharacterClassType.None -> DieColor.NormalAttack
}
