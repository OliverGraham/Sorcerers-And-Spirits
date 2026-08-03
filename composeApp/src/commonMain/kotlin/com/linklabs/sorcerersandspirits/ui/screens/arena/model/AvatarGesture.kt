package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

// TODO: Which are used by more than one thing?
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


internal fun resolveWeaponClassType(
    attackerClassType: CharacterClassType,
    impersonatedClassType: CharacterClassType?,
    id: String,
): CharacterClassType {
    if (attackerClassType != CharacterClassType.Illusionist) return attackerClassType
    val borrowedWeapon = impersonatedClassType?.takeIf { it in CharacterClassType.entries }
    return borrowedWeapon ?: randomBuiltWeaponFor(id = id)
}

private fun randomBuiltWeaponFor(id: String): CharacterClassType {
    val characterClassTypes = CharacterClassType.entries
    val count = characterClassTypes.size
    val index = ((id.hashCode() % count) + count) % count

    return characterClassTypes[index]
}

internal fun attackerStrike(
    weaponClassType: CharacterClassType,
    messageType: NormalRollMessageType,
): AvatarGesture =
    when (weaponClassType) {
        CharacterClassType.Rogue -> rogueStrike(messageType = messageType)
        CharacterClassType.Gambler -> gamblerStrike(messageType = messageType)
        CharacterClassType.Duelist -> duelistStrike(messageType = messageType)
        CharacterClassType.Necromancer -> AvatarGesture.Cast
        CharacterClassType.Bard -> AvatarGesture.Cast
        CharacterClassType.Mage -> AvatarGesture.Cast
        CharacterClassType.Paladin -> paladinStrike(messageType = messageType)
        else -> genericStrike(messageType = messageType)
    }

private fun paladinStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun rogueStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Stab
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.StabOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Stab
        NormalRollMessageType.Knockout -> AvatarGesture.StabFlurry
    }

private fun gamblerStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Throw
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.ThrowOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Throw
        NormalRollMessageType.Knockout -> AvatarGesture.ThrowStrong
    }

private fun duelistStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun genericStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Lunge
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.LungeOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Lunge
        NormalRollMessageType.Knockout -> AvatarGesture.LungeStrong
    }

internal fun defenderReactionGesture(normalRollMessageType: NormalRollMessageType): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Flinch
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.Sidestep
        NormalRollMessageType.Damage -> AvatarGesture.FlinchHeavy
        NormalRollMessageType.Knockout -> AvatarGesture.Collapse
    }