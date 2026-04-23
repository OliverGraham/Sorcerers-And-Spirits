package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.utilities.EmptyTwoArgumentLambda
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

internal data class ArenaActions(
    val onChooseCharacterClass: (PlayerState, CharacterClassState) -> Unit = EmptyTwoArgumentLambda
)

internal val LocalArenaActions = staticCompositionLocalOf { ArenaActions() }

internal object ArenaActionsProvider {
    val current: ArenaActions
        @Composable @ReadOnlyComposable get() = LocalArenaActions.current
}
