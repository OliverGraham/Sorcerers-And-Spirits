package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.InspectDrawer
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.CombatAnimationOverlays
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flashtext.FlashMessageOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.ExpandingMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBarHeaderTarget
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceStateHolder

@Composable
internal fun ArenaCombat(
    screenState: ArenaScreenState,
    arenaMatchState: ArenaMatchState.Combat,
    timerState: () -> ArenaTimerState,
    selectedOpponent: () -> String?,
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
    var isLogExpanded by remember { mutableStateOf(value = false) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val avatarBoundsStateHolder = remember { AvatarBoundsStateHolder() }

            ScreenContent(
                arenaScreenState = screenState,
                arenaMatchState = arenaMatchState,
                timerState = timerState,
                selectedOpponent = selectedOpponent,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                isLogExpanded = isLogExpanded,
                onLogExpandedChange = { isLogExpanded = it },
                onDiceHomeBoundsChanged = { diceHomeBounds = it },
                onInspect = { inspectedUserId.value = it },
            )

            OverlayContent(
                arenaScreenState = screenState,
                arenaMatchState = arenaMatchState,
                selectedOpponent = selectedOpponent,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                diceStateHolder = diceStateHolder,
                diceHomeBounds = diceHomeBounds,
                inspectedUserId = inspectedUserId.value,
                isLogExpanded = isLogExpanded,
                onDismissMessageLog = { isLogExpanded = false },
                onCloseInspectCharacterDrawer = { inspectedUserId.value = null },
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ScreenContent(
    arenaScreenState: ArenaScreenState,
    arenaMatchState: ArenaMatchState.Combat,
    timerState: () -> ArenaTimerState,
    selectedOpponent: () -> String?,
    isLogExpanded: Boolean,
    onLogExpandedChange: (Boolean) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MessagesBar(
            screenState = arenaScreenState,
            header =
            MessagesBarHeaderTarget.CombatTurn(
                roundNumber = arenaMatchState.roundNumber,
                classType = arenaMatchState.turnOwnerClassType,
            ),
            latestMessage = arenaScreenState.latestCombatHeaderMessage,
            timerState = timerState,
            isExpanded = isLogExpanded,
            onExpandedChange = onLogExpandedChange,
        )

        CombatContent(
            animatingMessage = arenaScreenState.animatingMessage,
            arenaMatchState = arenaMatchState,
            selectedOpponent = selectedOpponent,
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
    selectedOpponent: () -> String?,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    diceStateHolder: DiceStateHolder,
    diceHomeBounds: Rect?,
    inspectedUserId: String?,
    isLogExpanded: Boolean,
    onDismissMessageLog: () -> Unit,
    onCloseInspectCharacterDrawer: () -> Unit,
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
        selectedOpponent = selectedOpponent,
        diceStateHolder = diceStateHolder,
        areControlsVisible = !arenaScreenState.isMatchEnding,
        isPlayingBack = isPlayingBack,
        modifier = Modifier.align(alignment = Alignment.BottomCenter).navigationBarsPadding(),
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

    ExpandingMessageLog(
        isExpanded = isLogExpanded,
        messageGroups = arenaScreenState.messageGroups,
        onDismiss = onDismissMessageLog,
        isCombatOngoing = true,
    )

    InspectDrawer(
        characterClass = inspectedUserId?.let { arenaMatchState.playerMap[it]?.characterClass },
        onClose = onCloseInspectCharacterDrawer,
    )
}

@Composable
private fun SharedTransitionScope.CombatContent(
    arenaMatchState: ArenaMatchState.Combat,
    animatingMessage: ArenaMessageState?,
    selectedOpponent: () -> String?,
    onInspect: (String) -> Unit,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetUserId =
        arenaMatchState.resolveTargetUserId(selectedOpponentUserId = selectedOpponent())

    Box(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        CombatAvatarColumn(
            topPlayer = arenaMatchState.topPlayerFor(targetUserId = targetUserId),
            bottomPlayer = arenaMatchState.bottomPlayer,
            animatingMessage = animatingMessage,
            userPlayerId = arenaMatchState.userPlayerId,
            attackerUserId = arenaMatchState.attackerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = avatarBoundsStateHolder::reportAvatarBounds,
            onDiceHomeBoundsChanged = onDiceHomeBoundsChanged,
            modifier = Modifier.fillMaxWidth(),
        )

        if (arenaMatchState.playerMap.size > 2) {
            val standsUserIds =
                arenaMatchState.playersInTheStandsFor(targetUserId = targetUserId)
                    .map { it.userId }
                    .toSet()

            ClassIcons(
                players = arenaMatchState.playerMap.values.toList(),
                standsUserIds = standsUserIds,
                userPlayerId = arenaMatchState.userPlayerId,
                isTargetable = arenaMatchState.canTurnOwnerTargetAnOpponent(
                    isAnAnimationActive = animatingMessage != null && !animatingMessage.isContinuous(),
                ),
                charmTargetableUserIds = arenaMatchState.charmTargetableUserIds,
                onInspect = onInspect,
                onAvatarBoundsChanged = avatarBoundsStateHolder::reportAvatarBounds,
                animatingMessage = animatingMessage,
                modifier = Modifier.fillMaxWidth(fraction = 0.3f).align(alignment = Alignment.CenterEnd),
            )
        }
    }
}
