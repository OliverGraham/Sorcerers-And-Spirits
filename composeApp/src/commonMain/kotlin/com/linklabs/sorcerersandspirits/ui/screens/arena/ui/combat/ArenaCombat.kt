package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.headIconPlayersFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.resolveTargetUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatedAvatarUserIds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatedTargetPlayer
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceRoll

private const val AVATAR_SEATING_TRANSITION_LABEL = "ArenaCombatAvatarSeatingTransition"

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    state: ArenaState.Combat,
    modifier: Modifier = Modifier,
) {
    val diceRoll = rememberDiceRoll()

    var selectedOpponentUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = "") }

    var inspectedUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf<String?>(value = null) }

    var isTargetLockedOptimistically by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = false) }

    val targetedOpponentUserId =
        state.resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)

    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }

    var isMessagesExpanded by remember { mutableStateOf(value = false) }
    var unreadMessageCount by remember { mutableStateOf(value = 0) }
    LaunchedEffect(key1 = screenState.messages.size) {
        unreadMessageCount =
            if (isMessagesExpanded) 0 else unreadMessageCount + screenState.numberOfNewMessages
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state.seatedAvatarUserIds(targetUserId = targetedOpponentUserId),
                    label = AVATAR_SEATING_TRANSITION_LABEL,
                    modifier = Modifier.weight(weight = 1f),
                ) { seatedUserIds ->
                    SharedTransitionProvider(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    ) {
                        CombatStage(
                            state = state,
                            seatedUserIds = seatedUserIds,
                            targetedOpponentUserId = targetedOpponentUserId,
                            onSelectTarget = { selectedOpponentUserId = it },
                            onInspect = { inspectedUserId = it },
                            isTargetable =
                                state.isItThisUsersTurn &&
                                    state.lockedTargetUserId == null &&
                                    !isTargetLockedOptimistically,
                            onDiceHomeBoundsChanged = { diceHomeBounds = it },
                        )
                    }
                }

                BottomControls(
                    state = state,
                    targetedOpponentUserId = targetedOpponentUserId,
                    lockTarget = { isTargetLockedOptimistically = true },
                    diceRoll = diceRoll,
                )
            }

            // TODO Yourself: Make Messages and TurnTimer part of Screen Column
            Messages(
                messages = screenState.messages,
                unreadMessageCount = unreadMessageCount,
                isExpanded = isMessagesExpanded,
                onToggleExpanded = {
                    isMessagesExpanded = !isMessagesExpanded
                    unreadMessageCount = 0
                },
            )

            TurnTimer(
                secondsRemaining = screenState.secondsRemaining,
                isVisible = screenState.isTimerVisible,
                isUrgent = screenState.isTimerUrgent,
                modifier = Modifier.align(alignment = Alignment.TopEnd),
            )

            // TODO Yourself: These things truly go on top of the screen column, but perhaps
            //                Should be elsewhere
            DiceOverlay(
                diceRoll = diceRoll,
                homeBounds = diceHomeBounds,
                modifier = Modifier.fillMaxSize(),
            )

            FlashOverlay(
                flashMessages = screenState.flashMessages,
                modifier = Modifier.fillMaxSize(),
            )

            InspectDrawer(
                player = inspectedUserId?.let { state.playerMap[it] },
                onClose = { inspectedUserId = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The main combat area: the two seated combatant avatars on the left two-thirds and the remaining
 * players as a column of class heads on the right third. The dice rest between the two avatars; the
 * buttons and overlays live outside this stage so they do not take part in the avatar morph.
 */
@Composable
private fun CombatStage(
    state: ArenaState.Combat,
    seatedUserIds: Set<String>,
    targetedOpponentUserId: String,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headIconPlayers = state.headIconPlayersFor(seatedUserIds = seatedUserIds)
    val hasHeadIcons = headIconPlayers.isNotEmpty()

    Row(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        CombatAvatarColumn(
            topPlayer = state.attackingPlayer,
            bottomPlayer = state.seatedTargetPlayer(targetUserId = targetedOpponentUserId),
            userPlayerId = state.userPlayerId,
            activeUserIds = state.activeUserIds,
            onInspect = onInspect,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            modifier =
                Modifier.weight(weight = if (hasHeadIcons) 0.7f else 1f).padding(start = 16.dp),
        )

        if (hasHeadIcons) {
            ClassIcons(
                headIconPlayers = headIconPlayers,
                userPlayerId = state.userPlayerId,
                isTargetable = isTargetable,
                onSelectTarget = onSelectTarget,
                onInspect = onInspect,
                modifier = Modifier.weight(weight = 0.3f),
            )
        }
    }
}
