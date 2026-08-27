package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.utilities.EmptyTwoArgumentLambda
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey

internal data class ArenaActions(
    val onSelectCharacterClass: (CharacterClassState) -> Unit = {},
    val onChooseCharacterClass: (PlayerState, CharacterClassState) -> Unit = EmptyTwoArgumentLambda,
    val onAttack: (arguments: AttackArguments) -> Unit = {},
    val onUseAbility: (arguments: UseAbilityArguments) -> Unit = {},
    val onDiceSettled: (userId: String) -> Unit = {},
    val onAnimationComplete: (arguments: AnimationCompleteArguments) -> Unit = {},
    val onTryLeaveArena: () -> Unit = {},
    val onConfirmLeaveArena: () -> Unit = {},
    val onDismissForfeitDialog: () -> Unit = {},
    val onStayForAnotherMatch: () -> Unit = {},
    val onTargetOpponent: (userId: String) -> Unit = {},
)

data class AttackArguments(
    val targetedOpponentUserId: String,
    val userPlayerId: String,
    val combatActionState: CombatActionState,
)

data class UseAbilityArguments(
    val abilityActionState: AbilityActionState,
    val targetedOpponentUserId: String,
    val userPlayerId: String,
    val combatActionState: CombatActionState,
)

internal data class AnimationCompleteArguments(
    val messageId: String,
    val animationKey: AnimationKey,
)

internal val LocalArenaActions = staticCompositionLocalOf { ArenaActions() }

internal object ArenaActionsProvider {
    val current: ArenaActions
        @Composable @ReadOnlyComposable get() = LocalArenaActions.current
}
