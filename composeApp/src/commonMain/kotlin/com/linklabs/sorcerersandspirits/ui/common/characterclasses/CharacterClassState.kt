package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
data class CharacterClassState(
    val type: CharacterClassType,
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbilityState,
    val defensiveAbility: DefensiveAbilityState,
) {
    val className: String = type.name
}

internal fun CharacterClass.toState() =
    CharacterClassState(
        type = type,
        hitPoints = hitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toState(),
        defensiveAbility = defensiveAbility.toState(),
    )

internal fun CharacterClassState.toDomain() =
    CharacterClass(
        type = type,
        hitPoints = hitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toDomain(),
        defensiveAbility = defensiveAbility.toDomain(),
    )
