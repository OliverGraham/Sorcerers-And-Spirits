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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.CombatAnimationOverlays
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flashtext.FlashMessageOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessageLogOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceStateHolder

private const val AVATAR_SEATING_TRANSITION_LABEL = "ArenaCombatAvatarSeatingTransition"

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    arenaMatchState: ArenaMatchState.Combat,
    timerState: () -> ArenaTimerState,
    modifier: Modifier = Modifier,
) {
    val diceStateHolder =
        rememberDiceStateHolder(
            diceRollState = arenaMatchState.diceRollState,
            userId = arenaMatchState.userPlayerId,
        )

    val inspectedUserId =
        remember(key1 = arenaMatchState.activeUserIds) { mutableStateOf<String?>(value = null) }

    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }

    val displayedMessageCount = screenState.displayedMessageCount
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
                arenaMatchState = arenaMatchState,
                timerState = timerState,
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
                arenaMatchState = arenaMatchState,
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
    arenaMatchState: ArenaMatchState.Combat,
    timerState: () -> ArenaTimerState,
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
            roundNumber = arenaMatchState.roundNumber,
            turnOwnerClassType = arenaMatchState.turnOwnerClassType,
            latestMessage = arenaScreenState.latestCombatHeaderMessage,
            timerState = timerState,
            unreadMessageCount = unreadMessageCount,
            diceHomeBoundsTop = diceHomeBoundsTop,
            onToggleExpanded = onToggleExpanded,
            modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
        )

        CombatContent(
            animatingMessage = arenaScreenState.animatingMessage,
            arenaMatchState = arenaMatchState,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            onInspect = onInspect,
            modifier = Modifier.weight(weight = 1f),
        )
    }

    Spacer(
        modifier =
        Modifier.windowInsetsPadding(insets = WindowInsets.navigationBars)
            .height(height = BOTTOM_CONTROLS_HEIGHT),
    )
}

@Composable
private fun BoxScope.OverlayContent(
    arenaScreenState: ArenaScreenState,
    arenaMatchState: ArenaMatchState.Combat,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    diceStateHolder: DiceStateHolder,
    isMessageLogExpanded: Boolean,
    diceHomeBounds: Rect?,
    inspectedUserId: String?,
    onCloseInspectCharacterDrawer: () -> Unit,
    onCloseMessageLog: () -> Unit,
) {
    val animatingMessage = arenaScreenState.animatingMessage

    if (animatingMessage != null && animatingMessage.isAvatar()) {
        CombatAnimationOverlays(
            animatingMessage = animatingMessage,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            modifier = Modifier.fillMaxSize(),
        )
    }

    val isPlayingBack = animatingMessage != null && !animatingMessage.isContinuous()
    CombatButtons(
        state = arenaMatchState,
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
        DiceOverlay(
            diceStateHolder = diceStateHolder,
            homeBounds = diceHomeBounds,
            userId = arenaMatchState.userPlayerId,
        )
    }

    FlashMessageOverlay(
        animatingMessage = animatingMessage,
        avatarBoundsStateHolder = avatarBoundsStateHolder,
    )

    InspectDrawer(
        player = inspectedUserId?.let { arenaMatchState.playerMap[it] },
        onClose = onCloseInspectCharacterDrawer,
    )
}

@Composable
private fun SharedTransitionScope.CombatContent(
    arenaMatchState: ArenaMatchState.Combat,
    animatingMessage: ArenaMessageState?,
    onInspect: (String) -> Unit,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = arenaMatchState,
            contentKey = { "${it.attackerUserId}|${it.targetedUserId}" },
            label = AVATAR_SEATING_TRANSITION_LABEL,
            modifier = Modifier.fillMaxSize(),
        ) { animatingState ->
            SharedTransitionProvider(
                sharedTransitionScope = this@CombatContent,
                animatedVisibilityScope = this@AnimatedContent,
            ) {
                CombatStage(
                    arenaMatchState = animatingState,
                    animatingMessage = animatingMessage,
                    onInspect = onInspect,
                    isTargetable =
                    arenaMatchState.canTurnOwnerTargetAnOpponent(
                        isAnAnimationActive =
                        animatingMessage != null && !animatingMessage.isContinuous(),
                    ),
                    onAvatarBoundsChanged = avatarBoundsStateHolder::reportAvatarBounds,
                    onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
                )
            }
        }
    }
}

@Composable
private fun CombatStage(
    arenaMatchState: ArenaMatchState.Combat,
    animatingMessage: ArenaMessageState?,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val areTherePlayersInTheStands = arenaMatchState.areTherePlayersInTheStands

    Row(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        CombatAvatarColumn(
            topPlayer = arenaMatchState.targetedPlayer,
            bottomPlayer = arenaMatchState.attacker,
            animatingMessage = animatingMessage,
            userPlayerId = arenaMatchState.userPlayerId,
            activeUserIds = arenaMatchState.activeUserIds,
            attackerUserId = arenaMatchState.attackerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            modifier = Modifier.weight(weight = if (areTherePlayersInTheStands) 0.7f else 1f),
        )

        if (areTherePlayersInTheStands) {
            ClassIcons(
                headIconPlayers = arenaMatchState.playersInTheStands,
                userPlayerId = arenaMatchState.userPlayerId,
                isTargetable = isTargetable,
                onInspect = onInspect,
                animatingMessage = animatingMessage,
                modifier = Modifier.weight(weight = 0.3f),
            )
        }
    }
}
