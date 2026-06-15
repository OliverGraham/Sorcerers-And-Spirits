package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.utilities.EmptyTwoArgumentLambda
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

internal data class ArenaActions(
    val onSelectCharacterClass: (CharacterClassState) -> Unit = {},
    val onChooseCharacterClass: (PlayerState, CharacterClassState) -> Unit = EmptyTwoArgumentLambda,
    val onIncomingMessageHandled: () -> Unit = {},
    val onAttack: (arguments: AttackArguments) -> Unit = {},
    val onUseAbility: (arguments: UseAbilityArguments) -> Unit = {},
    val onRollDice: (arguments: RollDiceArguments) -> Unit = {},
    val onTryLeaveArena: () -> Unit = {},
    val onConfirmLeaveArena: () -> Unit = {},
    val onDismissForfeitDialog: () -> Unit = {},
    val onStayForAnotherMatch: () -> Unit = {},
)

data class AttackArguments(
    val combatActionState: CombatActionState,
    val targetedOpponentUserId: String,
    val userPlayerId: String,
)

// TODO(SS-015): Delete
data class RollDiceArguments(
    val combatActionState: CombatActionState,
    val targetedOpponentUserId: String,
    val userPlayerId: String,
)

// TODO(SS-016): Rename/refactor
data class UseAbilityArguments(
    val abilityActionState: AbilityActionState,
    val targetedOpponentUserId: String,
    val userPlayerId: String,
)

internal val LocalArenaActions = staticCompositionLocalOf { ArenaActions() }

internal object ArenaActionsProvider {
    val current: ArenaActions
        @Composable @ReadOnlyComposable get() = LocalArenaActions.current
}
