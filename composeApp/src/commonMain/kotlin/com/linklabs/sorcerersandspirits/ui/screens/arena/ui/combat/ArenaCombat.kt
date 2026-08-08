package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.CombatAnimationOverlays
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flashtext.FlashMessageOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceStateHolder
import kotlinx.coroutines.flow.first

private const val AVATAR_SEATING_TRANSITION_LABEL = "ArenaCombatAvatarSeatingTransition"

// TODO: ArenaScreenState is causing a recomposition for the entire screen from the timer. Why?

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    arenaState: ArenaState.Combat,
    modifier: Modifier = Modifier,
) {
    // TODO: Encapsulate all dice-rolling state DiceStateHolder
    val diceStateHolder = rememberDiceStateHolder()

    // TODO: Yep, refactor this BS
    CommitDiceRolls(state = arenaState, diceStateHolder = diceStateHolder)

    val inspectedUserId =
        remember(key1 = arenaState.activeUserIds) { mutableStateOf<String?>(value = null) }

    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }

    // TODO: At least half of this can be contained in ScreenContent
    val displayedMessageCount = screenState.messages.size
    var isMessageLogExpanded by remember { mutableStateOf(value = false) }
    var seenLogMessageCount by remember { mutableStateOf(value = displayedMessageCount) }
    LaunchedEffect(key1 = isMessageLogExpanded, key2 = displayedMessageCount) {
        val shouldSyncSeenCount =
            isMessageLogExpanded || displayedMessageCount < seenLogMessageCount
        if (shouldSyncSeenCount) seenLogMessageCount = displayedMessageCount
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val avatarBoundsStateHolder = remember { AvatarBoundsStateHolder() }

            ScreenContent(
                arenaScreenState = screenState,
                arenaState = arenaState,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                unreadMessageCount =
                    (displayedMessageCount - seenLogMessageCount).coerceAtLeast(minimumValue = 0),
                diceHomeBoundsTop = diceHomeBounds?.top,
                onDiceHomeBoundsChanged = { diceHomeBounds = it },
                onToggleExpanded = {
                    isMessageLogExpanded = !isMessageLogExpanded
                    if (isMessageLogExpanded) seenLogMessageCount = displayedMessageCount
                },
                onInspect = { inspectedUserId.value = it },
            )

            OverlayContent(
                arenaScreenState = screenState,
                arenaState = arenaState,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                diceStateHolder = diceStateHolder,
                isMessageLogExpanded = isMessageLogExpanded,
                diceHomeBounds = diceHomeBounds,
                inspectedUserId = inspectedUserId.value,
                onCloseInspectCharacterDrawer = { inspectedUserId.value = null },
                onCloseMessageLog = { isMessageLogExpanded = false },
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ScreenContent(
    arenaScreenState: ArenaScreenState,
    arenaState: ArenaState.Combat,
    unreadMessageCount: Int,
    diceHomeBoundsTop: Float?,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    onInspect: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MessagesBar(
            roundNumber = arenaState.roundNumber,
            turnOwnerCharacterClass = arenaState.turnOwnerCharacterClass,
            latestMessage = arenaScreenState.latestCombatHeaderMessage,
            secondsRemaining = arenaScreenState.secondsRemaining,
            isTimerVisible = arenaScreenState.isTimerVisible,
            isTimerUrgent = arenaScreenState.isTimerUrgent,
            unreadMessageCount = unreadMessageCount,
            diceHomeBoundsTop = diceHomeBoundsTop,
            onToggleExpanded = onToggleExpanded,
            modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
        )

        CombatContent(
            animatingMessage = arenaScreenState.animatingMessage,
            arenaState = arenaState,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            onInspect = onInspect,
            modifier = Modifier.weight(weight = 1f),
        )
    }

    Spacer(
        modifier =
            Modifier.windowInsetsPadding(insets = WindowInsets.navigationBars)
                .height(height = BOTTOM_CONTROLS_HEIGHT)
    )
}

@Composable
private fun BoxScope.OverlayContent(
    arenaScreenState: ArenaScreenState,
    arenaState: ArenaState.Combat,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    diceStateHolder: DiceStateHolder,
    isMessageLogExpanded: Boolean,
    diceHomeBounds: Rect?,
    inspectedUserId: String?,
    onCloseInspectCharacterDrawer: () -> Unit,
    onCloseMessageLog: () -> Unit,
) {
    val animatingMessage = arenaScreenState.animatingMessage

    // TODO: is this it?
    val isPlayingBack = animatingMessage != null
    CombatButtons(
        state = arenaState,
        diceStateHolder = diceStateHolder,
        areControlsVisible = !arenaScreenState.isMatchEnding,
        isPlayingBack = isPlayingBack,
        modifier = Modifier.align(alignment = Alignment.BottomCenter).navigationBarsPadding(),
    )


    MessageLogOverlay(
        messageGroups = arenaScreenState.messageGroups,
        isExpanded = isMessageLogExpanded,
        diceHomeBounds = diceHomeBounds,
        onDismiss = onCloseMessageLog,
        modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
    )

    if (diceHomeBounds != null) {
        DiceOverlay(diceStateHolder = diceStateHolder, homeBounds = diceHomeBounds)
    }

    if (animatingMessage?.isFlash() == true) {
        FlashMessageOverlay(message = animatingMessage, avatarBoundsStateHolder = avatarBoundsStateHolder)
    }

    InspectDrawer(
        player = inspectedUserId?.let { arenaState.playerMap[it] },
        onClose = onCloseInspectCharacterDrawer,
    )
}

@Composable
private fun SharedTransitionScope.CombatContent(
    arenaState: ArenaState.Combat,
    animatingMessage: ArenaMessageState?,
    onInspect: (String) -> Unit,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = arenaState,
            label = AVATAR_SEATING_TRANSITION_LABEL,
            modifier = Modifier.fillMaxSize(),
        ) { animatingState ->
            SharedTransitionProvider(
                sharedTransitionScope = this@CombatContent,
                animatedVisibilityScope = this@AnimatedContent,
            ) {
                CombatStage(
                    arenaState = animatingState,
                    animatingMessage = animatingMessage,
                    onInspect = onInspect,
                    isTargetable =
                        arenaState.canTurnOwnerTargetAnOpponent(
                            isAnAnimationActive = animatingMessage != null
                        ),
                    onAvatarBoundsChanged = avatarBoundsStateHolder::reportAvatarBounds,
                    onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
                )
            }
        }

        if (animatingMessage != null && animatingMessage.isAvatar()) {
            CombatAnimationOverlays(
                animatingMessage = animatingMessage,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
            )
        }
    }
}

// TODO: Should be DiceStateHolder. Also, see if it's correct
@Composable
private fun CommitDiceRolls(state: ArenaState.Combat, diceStateHolder: DiceStateHolder) {
    val onDiceSettled = ArenaActionsProvider.current.onDiceSettled
    val latestState by rememberUpdatedState(newValue = state)

    LaunchedEffect(key1 = Unit) {
        var lastRoll: DiceRollState? = latestState.diceRollState
        snapshotFlow { latestState.diceRollState }
            .collect { roll ->
                val isNewRoll = roll != null && roll != lastRoll
                if (!isNewRoll) return@collect
                lastRoll = roll

                diceStateHolder.commitRoll(
                    diceRollState = roll,
                    diceBadge = latestState.diceBadgeState,
                    isBadgeBonusSpent = latestState.isTeleportDoublesBonusSpent,
                )
                snapshotFlow { diceStateHolder.isSettled }.first { it }
                onDiceSettled()
            }
    }
}

@Composable
private fun CombatStage(
    arenaState: ArenaState.Combat,
    animatingMessage: ArenaMessageState?,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val areTherePlayersInTheStands = arenaState.areTherePlayersInTheStands

    Row(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        CombatAvatarColumn(
            topPlayer = arenaState.targetedPlayer,
            bottomPlayer = arenaState.turnOwner,
            animatingMessage = animatingMessage,
            userPlayerId = arenaState.userPlayerId,
            activeUserIds = arenaState.activeUserIds,
            turnOwnerUserId = arenaState.turnOwnerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            modifier = Modifier.weight(weight = if (areTherePlayersInTheStands) 0.7f else 1f),
        )

        if (areTherePlayersInTheStands) {
            ClassIcons(
                headIconPlayers = arenaState.playersInTheStands,
                userPlayerId = arenaState.userPlayerId,
                isTargetable = isTargetable,
                onInspect = onInspect,
                animatingMessage = animatingMessage,
                modifier = Modifier.weight(weight = 0.3f),
            )
        }
    }
}
