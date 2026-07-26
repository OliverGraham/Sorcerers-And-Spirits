package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatMoment
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatSeating
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.collapsingUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatedPlayer
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatingFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CombatAnimationOverlays
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CombatAvatarBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.EncoreFullScreenOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.SpellPotencyFullScreenOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceStateHolder

private const val AVATAR_SEATING_TRANSITION_LABEL = "ArenaCombatAvatarSeatingTransition"

// TODO: ArenaScreenState is causing a recomposition for the entire screen from the timer. Why?

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    state: ArenaState.Combat,
    modifier: Modifier = Modifier,
) {
    // TODO: Encapsulate all dice-rolling state DiceStateHolder
    val diceStateHolder = rememberDiceStateHolder()

    val flashPlayback =
        rememberFlashPlayback(
            messages = screenState.playbackMessages,
            diceRollState = state.diceRollState,
            diceBadgeState = state.diceBadgeState,
            isBadgeBonusSpent = state.isTeleportDoublesBonusSpent,
            diceStateHolder = diceStateHolder,
        )
    val isAnimating = flashPlayback.isAnimating(animationCount = screenState.animationCount)

    var selectedOpponentUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = "") }

    var inspectedUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf<String?>(value = null) }

    var isTargetLockedOptimistically by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = false) }

    var isActionInFlight by remember(key1 = state) { mutableStateOf(value = false) }

    val targetedOpponentUserId =
        state.resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)

    val liveMoment =
        CombatMoment(
            seating =
                state.seatingFor(
                    targetUserId = targetedOpponentUserId,
                    isMatchEnding = screenState.isMatchEnding,
                    collapsingTargetUserId =
                        screenState.collapsingSeatUserId(playback = flashPlayback),
                ),
            activeUserIds = state.activeUserIds,
            turnOwnerUserId = state.turnOwnerUserId,
        )
    val heldMoment =
        rememberHeldCombatMoment(
            liveMoment = liveMoment,
            latestAnimationMessageId = screenState.latestAnimationMessageId,
            isSequencing = isAnimating,
        )

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
                    diceHomeBounds = diceHomeBounds,
                    onToggleExpanded = {
                        isMessagesExpanded = !isMessagesExpanded
                        unreadMessageCount = 0
                    },
                    modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
                )

                Box(modifier = Modifier.weight(weight = 1f)) {
                    AnimatedContent(
                        targetState = heldMoment.seating,
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
                                activeUserIds = heldMoment.activeUserIds,
                                turnOwnerUserId = heldMoment.turnOwnerUserId,
                                animationCue = flashPlayback.combatAnimationCue,
                                onSelectTarget = { selectedOpponentUserId = it },
                                onInspect = { inspectedUserId = it },
                                isTargetable =
                                    state.isItThisUsersTurn &&
                                        !state.isMatchOver &&
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
                    )
                }

                Spacer(
                    modifier =
                        Modifier.windowInsetsPadding(insets = WindowInsets.navigationBars)
                            .height(height = BOTTOM_CONTROLS_HEIGHT)
                )
            }

            EncoreFullScreenOverlay(
                animationCue = flashPlayback.combatAnimationCue,
                presentClasses = presentClasses,
            )

            BottomControls(
                state = state,
                targetedOpponentUserId = targetedOpponentUserId,
                lockTarget = { isTargetLockedOptimistically = true },
                diceStateHolder = diceStateHolder,
                areControlsVisible = !screenState.isMatchEnding,
                isAnimating = isAnimating,
                isActionInFlight = isActionInFlight,
                onActionDispatched = {
                    isActionInFlight = true
                    flashPlayback.markActing()
                },
                modifier =
                    Modifier.align(alignment = Alignment.BottomCenter).navigationBarsPadding(),
            )

            SpellPotencyFullScreenOverlay(
                animationCue = flashPlayback.combatAnimationCue,
                avatarBounds = avatarBounds,
                presentClasses = presentClasses,
            )

            MessageLogOverlay(
                messages = screenState.logMessages,
                isExpanded = isMessagesExpanded,
                diceHomeBounds = diceHomeBounds,
                onDismiss = { isMessagesExpanded = false },
                modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
            )

            // TODO Yourself: These things truly go on top of the screen column, but perhaps
            //                Should be elsewhere
            val homeBounds = diceHomeBounds
            if (homeBounds != null) {
                DiceOverlay(diceStateHolder = diceStateHolder, homeBounds = homeBounds)
            }

            FlashOverlay(playback = flashPlayback, avatarBounds = avatarBounds)

            InspectDrawer(
                player = inspectedUserId?.let { state.playerMap[it] },
                onClose = { inspectedUserId = null },
            )
        }
    }
}

private fun ArenaScreenState.collapsingSeatUserId(playback: FlashPlaybackState): String? {
    val latestCollapse =
        playbackMessages.lastOrNull { it.cue?.collapsingUserId != null } ?: return null

    val isCollapseFinished = latestCollapse.id == playback.finishedCollapseMessageId
    val shouldHoldSeat = isMatchEnding || !isCollapseFinished

    return if (shouldHoldSeat) latestCollapse.cue?.collapsingUserId else null
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
    activeUserIds: Set<String>,
    turnOwnerUserId: String,
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
            activeUserIds = activeUserIds,
            turnOwnerUserId = turnOwnerUserId,
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
                animationCue = animationCue,
                modifier = Modifier.weight(weight = 0.3f),
            )
        }
    }
}

// TODO: This logic needs to be simplified and made correct, without confusing fallbacks that should
//       never happen
internal fun ArenaState.Combat.resolveTargetUserId(selectedOpponentUserId: String): String =
    when (val action = combatActionState) {
        is CombatActionState.NormalAttack -> action.defenderUserId

        is CombatActionState.AbilityRoll -> action.defenderUserId

        is CombatActionState.Idle ->
            idleTargetUserId(selectedOpponentUserId = selectedOpponentUserId)
    }

private fun ArenaState.Combat.idleTargetUserId(selectedOpponentUserId: String): String {
    val isUserTheTurnOwner = turnOwnerUserId == userPlayerId
    val userChosenTarget =
        if (isUserTheTurnOwner) lockedTargetUserId ?: selectedOpponentUserId else ""

    return userChosenTarget.ifEmpty { defaultTargetedOpponentUserId() }
}

/** Finds opponent user id if there is only a single conscious opponent. */
private fun ArenaState.Combat.defaultTargetedOpponentUserId(): String =
    playerMap.values
        .singleOrNull { it.userId != turnOwnerUserId && it.characterClass.isConscious }
        ?.userId
        .orEmpty()
