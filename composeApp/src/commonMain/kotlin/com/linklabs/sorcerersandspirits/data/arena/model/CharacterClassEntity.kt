package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.Serializable

@Serializable
data class CharacterClassEntity(
    val type: CharacterClassType,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbilityEntity,
    val defensiveAbility: DefensiveAbilityEntity,
) {
    val className = type.name
}

internal fun CharacterClassEntity.toDomain(): CharacterClass =
    CharacterClass(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toDomain(),
        defensiveAbility = defensiveAbility.toDomain(),
    )

internal fun CharacterClass.toEntity(): CharacterClassEntity =
    CharacterClassEntity(
        type = type,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toEntity(),
        defensiveAbility = defensiveAbility.toEntity(),
    )
