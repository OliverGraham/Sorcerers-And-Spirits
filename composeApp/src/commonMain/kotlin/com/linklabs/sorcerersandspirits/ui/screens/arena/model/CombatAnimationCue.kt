package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

enum class AvatarGesture {
    Lunge,
    LungeStrong,
    LungeOvershoot,
    Stab,
    StabFlurry,
    StabOvershoot,
    Slash,
    SlashFinish,
    SlashWhiff,
    Cast,
    PreparedThrust,
    PreparedThrustWhiff,
    Throw,
    ThrowStrong,
    ThrowOvershoot,
    AllInToss,
    AllInBust,
    ReArm,
    Flinch,
    FlinchHeavy,
    Sidestep,
    DodgeFlourish,
    DodgeBrace,
    Collapse,
}

// TODO: This can be removed since all weapons have been built
internal val builtWeaponClassTypes =
    listOf(
        CharacterClassType.Rogue,
        CharacterClassType.Gambler,
        CharacterClassType.Duelist,
        CharacterClassType.Necromancer,
        CharacterClassType.Bard,
        CharacterClassType.Mage,
        CharacterClassType.Paladin,
    )
