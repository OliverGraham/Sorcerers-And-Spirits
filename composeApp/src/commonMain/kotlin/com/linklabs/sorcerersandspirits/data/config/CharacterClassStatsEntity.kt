package com.linklabs.sorcerersandspirits.data.config

import kotlinx.serialization.Serializable

/**
 * The remotely-tunable stats for a single class, keyed by class type in the RemoteConfig payload.
 * These numeric values are the single source of truth for class stats; ability wiring is resolved
 * in code from the class type (see CharacterClass.create).
 */
@Serializable
data class CharacterClassStatsEntity(
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
)
