package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.game.Ability

@Immutable
sealed interface RollTypeState {
    @Immutable data class AbilityRoll(val ability: Ability) : RollTypeState

    @Immutable data object ArmorClassRoll : RollTypeState

    @Immutable data object HealthRoll : RollTypeState
}

// TODO: need AbilityState?

internal fun RollType.toState(): RollTypeState =
    when (this) {
        is RollType.AbilityRoll -> RollTypeState.AbilityRoll(ability = ability)
        is RollType.ArmorClassRoll -> RollTypeState.ArmorClassRoll
        is RollType.HealthRoll -> RollTypeState.HealthRoll
    }

internal fun RollTypeState.toDomain(): RollType =
    when (this) {
        is RollTypeState.AbilityRoll -> RollType.AbilityRoll(ability = ability)
        is RollTypeState.ArmorClassRoll -> RollType.ArmorClassRoll
        is RollTypeState.HealthRoll -> RollType.HealthRoll
    }
