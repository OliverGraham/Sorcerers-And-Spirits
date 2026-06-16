package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.Serializable

/**
 * The remotely-tunable stats for a single class. Only the numeric values live in RemoteConfig;
 * ability wiring stays in [CharacterClass]'s factory functions.
 */
@Serializable
data class CharacterClassStatsEntity(
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
)

internal fun CharacterClass.toStatsEntity(): CharacterClassStatsEntity =
    CharacterClassStatsEntity(
        hitPoints = hitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass,
        speed = speed,
    )

/**
 * In-app defaults keyed by [CharacterClassType] name, derived from the hardcoded class factories so
 * the offline/first-run values always match the shipped code.
 */
internal fun defaultCharacterClassStats(): Map<String, CharacterClassStatsEntity> =
    CharacterClass.getAllClasses().associate { characterClass ->
        characterClass.type.name to characterClass.toStatsEntity()
    }
