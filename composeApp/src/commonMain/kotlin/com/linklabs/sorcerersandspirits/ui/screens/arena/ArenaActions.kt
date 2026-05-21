package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.utilities.EmptyTwoArgumentLambda
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

internal data class ArenaActions(
    val onSelectCharacterClass: (CharacterClassState) -> Unit = {},
    val onChooseCharacterClass: (PlayerState, CharacterClassState) -> Unit = EmptyTwoArgumentLambda,
    val onIncomingMessageHandled: () -> Unit = {},
    val onAttack: (targetUserId: String) -> Unit = {},
    val onUseAbility: (abilityName: String) -> Unit = {},
    val onRollDice: (arguments: RollDiceArguments) -> Unit = {},
    val onTryLeaveArena: () -> Unit = {},
    val onConfirmLeaveArena: () -> Unit = {},
    val onDismissForfeitDialog: () -> Unit = {},
)

// TODO: Different name if used in Composable as well
data class RollDiceArguments(val opponentUserId: String, val rollType: RollType)

internal val LocalArenaActions = staticCompositionLocalOf { ArenaActions() }

internal object ArenaActionsProvider {
    val current: ArenaActions
        @Composable @ReadOnlyComposable get() = LocalArenaActions.current
}
