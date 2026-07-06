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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatSeating
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.collapsingUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.combatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.resolveTargetUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatedPlayer
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatingFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CombatAnimationOverlays
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CombatAvatarBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceRoll

private const val AVATAR_SEATING_TRANSITION_LABEL = "ArenaCombatAvatarSeatingTransition"

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    state: ArenaState.Combat,
    modifier: Modifier = Modifier,
) {
    val diceRoll = rememberDiceRoll()

    val flashPlayback = rememberFlashPlayback(flashMessages = screenState.flashMessages)

    var selectedOpponentUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = "") }

    var inspectedUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf<String?>(value = null) }

    var isTargetLockedOptimistically by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = false) }

    var isActionInFlight by
        remember(key1 = state.combatActionState, key2 = screenState.messages.lastOrNull()?.id) {
            mutableStateOf(value = false)
        }

    val targetedOpponentUserId =
        state.resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)

    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }
    val avatarBounds = remember { CombatAvatarBounds() }

    val presentClasses =
        remember(key1 = state.playerMap.keys) {
            state.playerMap.values.map { it.characterClass.type }.toSet()
        }

    var isMessagesExpanded by remember { mutableStateOf(value = false) }
    var unreadMessageCount by remember { mutableStateOf(value = 0) }
    LaunchedEffect(key1 = screenState.messages.size) {
        unreadMessageCount =
            if (isMessagesExpanded) 0 else unreadMessageCount + screenState.numberOfNewMessages
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TODO: Make MessagesBar and TurnTimer part of Screen Column
                MessagesBar(
                    latestMessage = screenState.messages.lastOrNull(),
                    secondsRemaining = screenState.secondsRemaining,
                    isTimerVisible = screenState.isTimerVisible,
                    isTimerUrgent = screenState.isTimerUrgent,
                    unreadMessageCount = unreadMessageCount,
                    onToggleExpanded = {
                        isMessagesExpanded = !isMessagesExpanded
                        unreadMessageCount = 0
                    },
                )

                Box(modifier = Modifier.weight(weight = 1f)) {
                    AnimatedContent(
                        targetState =
                            state.seatingFor(
                                targetUserId = targetedOpponentUserId,
                                isMatchEnding = screenState.isFinishPending,
                                collapsingTargetUserId =
                                    screenState.collapsingSeatUserId(playback = flashPlayback),
                            ),
                        label = AVATAR_SEATING_TRANSITION_LABEL,
                        modifier = Modifier.fillMaxSize(),
                    ) { seating ->
                        SharedTransitionProvider(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                        ) {
                            CombatStage(
                                state = state,
                                seating = seating,
                                animationCue = flashPlayback.combatAnimationCue,
                                onSelectTarget = { selectedOpponentUserId = it },
                                onInspect = { inspectedUserId = it },
                                isTargetable =
                                    state.isItThisUsersTurn &&
                                        state.lockedTargetUserId == null &&
                                        !isTargetLockedOptimistically,
                                onAvatarBoundsChanged = avatarBounds::reportAvatarBounds,
                                onDiceHomeBoundsChanged = { diceHomeBounds = it },
                            )
                        }
                    }

                    CombatAnimationOverlays(
                        animationCue = flashPlayback.combatAnimationCue,
                        avatarBounds = avatarBounds,
                        presentClasses = presentClasses,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                BottomControls(
                    state = state,
                    targetedOpponentUserId = targetedOpponentUserId,
                    lockTarget = { isTargetLockedOptimistically = true },
                    diceRoll = diceRoll,
                    areControlsVisible = !screenState.isFinishPending,
                    isActionInFlight = isActionInFlight,
                    onActionDispatched = { isActionInFlight = true },
                )
            }

            MessageLogOverlay(
                messages = screenState.messages,
                isExpanded = isMessagesExpanded,
                diceHomeBounds = diceHomeBounds,
            )

            // TODO Yourself: These things truly go on top of the screen column, but perhaps
            //                Should be elsewhere
            DiceOverlay(diceRoll = diceRoll, homeBounds = diceHomeBounds)

            FlashOverlay(playback = flashPlayback)

            InspectDrawer(
                player = inspectedUserId?.let { state.playerMap[it] },
                onClose = { inspectedUserId = null },
            )
        }
    }
}

/**
 * The player to hold in the second large seat because their collapse is in flight: the latest flash
 * message that knocks someone out, kept seated until the sequencer finishes playing it, or for the
 * whole finish hold when the match is ending so the downed avatar never becomes a head icon
 * (SS-213). Read straight from [ArenaScreenState.flashMessages] so the seat is held the same frame
 * the knockout arrives, before the sequenced cue goes live and the pane would otherwise swap.
 */
private fun ArenaScreenState.collapsingSeatUserId(playback: FlashPlaybackState): String? {
    val latestCollapse =
        flashMessages.lastOrNull { it.combatAnimationCue?.collapsingUserId != null } ?: return null
    val isCollapseFinished = latestCollapse.id == playback.finishedCollapseMessageId
    val shouldHoldSeat = isFinishPending || !isCollapseFinished
    return if (shouldHoldSeat) latestCollapse.combatAnimationCue?.collapsingUserId else null
}

/**
 * The main combat area: the two seated combatant avatars on the left two-thirds and the remaining
 * players as a column of class heads on the right third. The dice rest between the two avatars; the
 * buttons and overlays live outside this stage so they do not take part in the avatar morph.
 *
 * Everything here is placed by the [seating] snapshot this pane was keyed on, never re-derived from
 * the live state: while the seating transition runs, the outgoing pane keeps showing the
 * arrangement it was built for, so a player leaving a seat morphs to their head icon instead of
 * vanishing or appearing twice (SS-181). Only the mutable player data (hit points, halo) reads
 * live.
 */
@Composable
private fun CombatStage(
    state: ArenaState.Combat,
    seating: CombatSeating,
    animationCue: CombatAnimationCue?,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headIconPlayers = seating.headIconUserIds.mapNotNull { state.playerMap[it] }
    val hasHeadIcons = headIconPlayers.isNotEmpty()

    Row(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        CombatAvatarColumn(
            topPlayer = state.seatedPlayer(seatUserId = seating.topSeatUserId),
            bottomPlayer = state.seatedPlayer(seatUserId = seating.bottomSeatUserId),
            animationCue = animationCue,
            userPlayerId = state.userPlayerId,
            activeUserIds = state.activeUserIds,
            turnOwnerUserId = state.turnOwnerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            modifier = Modifier.weight(weight = if (hasHeadIcons) 0.7f else 1f),
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
