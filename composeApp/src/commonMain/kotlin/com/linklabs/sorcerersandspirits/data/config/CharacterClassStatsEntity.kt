package com.linklabs.sorcerersandspirits.data.config

import kotlinx.serialization.Serializable

@Serializable
data class CharacterClassStatsEntity(
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
)
