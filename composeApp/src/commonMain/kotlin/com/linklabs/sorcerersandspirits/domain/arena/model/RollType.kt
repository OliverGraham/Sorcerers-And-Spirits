package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.Ability

sealed interface RollType {

    data class AbilityRoll(val ability: Ability) : RollType

    data object ArmorClassRoll : RollType

    data object HealthRoll : RollType
}
