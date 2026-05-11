package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

@Immutable
data class CharacterClassState(
    val type: CharacterClassType,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbility,
    val defensiveAbility: DefensiveAbility,
) {
    val className: String = type.name
}

internal fun CharacterClass.toState() =
    CharacterClassState(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility,
        defensiveAbility = defensiveAbility,
    )

internal fun CharacterClassState.toDomain() =
    CharacterClass(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility,
        defensiveAbility = defensiveAbility,
    )
