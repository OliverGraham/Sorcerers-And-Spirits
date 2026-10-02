package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
data class FinishedPlayerRecap(
    val userId: String,
    val displayName: String,
    val classType: CharacterClassType,
    val outcome: FinishedOutcome,
)

@Immutable
sealed interface FinishedOutcome {

    @Immutable data object Winner : FinishedOutcome

    @Immutable data class Placement(val place: Int) : FinishedOutcome
}
