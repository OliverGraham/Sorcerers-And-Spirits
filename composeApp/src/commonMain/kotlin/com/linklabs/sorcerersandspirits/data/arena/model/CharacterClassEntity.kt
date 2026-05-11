package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.Serializable

@Serializable
data class CharacterClassEntity(
    val type: CharacterClassType,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbility,
    val defensiveAbility: DefensiveAbility,
) {
    val className = type.name
}

internal fun CharacterClassEntity.toDomain(): CharacterClass =
    CharacterClass(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility,
        defensiveAbility = defensiveAbility,
    )

internal fun CharacterClass.toEntity(): CharacterClassEntity =
    CharacterClassEntity(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility,
        defensiveAbility = defensiveAbility,
    )
