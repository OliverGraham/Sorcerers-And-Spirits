package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.headIconPlayersFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.resolveTargetUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.targetPlayerFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceRoll

private const val TARGET_TRANSITION_LABEL = "NewArenaCombatTargetTransition"

@Composable
internal fun NewArenaCombat(
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
                Row(modifier = Modifier.fillMaxSize()) {
                    // TODO: This Avatar column should take up 2/3 of the width of the screen. There
                    //       Should be a good amount of space on the left, though. We'll figure it out
                    Column {
                        // TODO Large Avatar
                        // TODO DiceHome
                        // TODO Large Avatar

                        // TODO: The vertical ordering of the Avatars doesn't matter - it can be random
                        //       The user will have the ic_user_icon in the bottom left of the contianer that contains their Avatar -
                        //       the icon will not touch the Avatar.
                        //       Whoever's turn it is will have a halo icon underneath their feet,
                        //       a selected type of icon to indicate that it's their turn to roll.
                        //       This can be when they're the attacker or if they're using a
                        //       DefensiveAbility, etc.
                        //       The activePlayerHaloIcon will be this color gradient drawn 3d-like as an oval
                        //         background: radial-gradient(50% 50% at 50% 50%, #CB9C6A 42.31%, #654E35 94.23%);
                    }

                    // TODO: The ClassIcons will now be a Column that occupies the right 1/3 or 1/4 of the screen.
                    //       Whomever the user will have the ic_user_icon (smaller) at the bottom part of their ClassIcon circle
                    Column {

                    }
                }

                // TODO: This Row will be the Row of Buttons. Let me know if I need to describe how
                //       the buttons should be laid out, but all buttons should be in this row
                Row {

                }

                // TODO: The existing StatColumns on both sides of the screen are done way with.
                //       We'll use the Drawer in order to tell the user about stats, so even large Avatars
                //       must be clickable to bring up the drawer.
            }

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState =
                        AvatarPairing(
                            activeUserId = state.attackingUserId,
                            targetUserId = targetedOpponentUserId,
                        ),
                    label = TARGET_TRANSITION_LABEL,
                    modifier = Modifier.weight(weight = 1f),
                ) { currentPairing ->
                    SharedTransitionProvider(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    ) {
                        TransitioningContent(
                            state = state,
                            activeUserId = currentPairing.activeUserId,
                            currentTargetUserId = currentPairing.targetUserId,
                            onSelectTarget = { selectedOpponentUserId = it },
                            onInspect = { inspectedUserId = it },
                            isTargetable =
                                state.isItThisUsersTurn &&
                                    state.lockedTargetUserId == null &&
                                    !isTargetLockedOptimistically,
                            messages = screenState.messages,
                            unreadMessageCount = unreadMessageCount,
                            isMessagesExpanded = isMessagesExpanded,
                            onToggleMessages = {
                                isMessagesExpanded = !isMessagesExpanded
                                unreadMessageCount = 0
                            },
                        )
                    }
                }

                DiceHomeSlot(onBoundsChanged = { diceHomeBounds = it })

                BottomControls(
                    state = state,
                    targetedOpponentUserId = targetedOpponentUserId,
                    lockTarget = { isTargetLockedOptimistically = true },
                    diceRoll = diceRoll,
                )
            }

            TurnTimer(
                secondsRemaining = screenState.secondsRemaining,
                isVisible = screenState.isTimerVisible,
                isUrgent = screenState.isTimerUrgent,
                modifier = Modifier.align(alignment = Alignment.TopEnd),
            )

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
 * The two players seated in the bottom-half avatars: the active turn owner on the left and the
 * targeted player on the right (SS-157). Keying the avatar [AnimatedContent] on this pairing lets
 * both slots morph when the turn passes or the target changes, not just the target.
 */
@Immutable private data class AvatarPairing(val activeUserId: String, val targetUserId: String)

@Composable
private fun TransitioningContent(
    state: ArenaState.Combat,
    activeUserId: String,
    currentTargetUserId: String,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    isMessagesExpanded: Boolean,
    onToggleMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopHalfContent(
            headIconPlayers =
                state.headIconPlayersFor(
                    activeUserId = activeUserId,
                    targetUserId = currentTargetUserId,
                ),
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            onInspect = onInspect,
            messages = messages,
            unreadMessageCount = unreadMessageCount,
            isMessagesExpanded = isMessagesExpanded,
            onToggleMessages = onToggleMessages,
            modifier = Modifier.weight(weight = 1f),
        )
        AvatarsRow(
            activePlayer = state.playerMap.getValue(key = activeUserId),
            targetPlayer = state.targetPlayerFor(targetUserId = currentTargetUserId),
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

@Composable
private fun TopHalfContent(
    headIconPlayers: List<PlayerState.Selected>,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    isMessagesExpanded: Boolean,
    onToggleMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ClassIcons(
            headIconPlayers = headIconPlayers,
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            onInspect = onInspect,
            modifier = Modifier.padding(top = MESSAGES_ICON_SIZE),
        )
        Messages(
            messages = messages,
            unreadMessageCount = unreadMessageCount,
            isExpanded = isMessagesExpanded,
            onToggleExpanded = onToggleMessages,
        )
    }
}
