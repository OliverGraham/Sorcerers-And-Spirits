package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.LocalArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_user_player

private val USER_MARKER_ICON_SIZE = 24.dp
private val USER_MARKER_ICON_INSET = 4.dp

@Composable
internal fun SharedTransitionScope.ClassIcons(
    players: List<PlayerState.Selected>,
    standsUserIds: Set<String>,
    userPlayerId: String,
    isTargetable: Boolean,
    charmTargetableUserIds: Set<String>,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    animatingMessage: ArenaMessageState?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        players.forEach { player ->
            Box(
                modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                this@Column.AnimatedVisibility(
                    visible = player.userId in standsUserIds,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SharedTransitionProvider(
                        sharedTransitionScope = this@ClassIcons,
                        animatedVisibilityScope = this@AnimatedVisibility,
                    ) {
                        ClassIcon(
                            player = player,
                            isUserPlayer = player.userId == userPlayerId,
                            isTargetable = isTargetable,
                            isCharmTarget = player.userId in charmTargetableUserIds,
                            onInspect = onInspect,
                            onAvatarBoundsChanged = onAvatarBoundsChanged,
                            animatingMessage = animatingMessage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassIcon(
    player: PlayerState.Selected,
    isUserPlayer: Boolean,
    isTargetable: Boolean,
    isCharmTarget: Boolean,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    animatingMessage: ArenaMessageState?,
    modifier: Modifier = Modifier,
) {
    val isConscious = player.characterClass.isConscious
    val isTargetingTap = isTargetable && isConscious

    // TODO: only call this function if there's a conscious character with SpellPotency
    val splash = rememberSpellPotencySplash(animatingMessage = animatingMessage, player = player)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val headWidth = minOf(a = maxWidth, b = maxHeight)

        val onTargetOpponent = LocalArenaActions.current.onTargetOpponent
        val onChooseCharmTarget = LocalArenaActions.current.onChooseCharmTarget
        ClassHeadIcon(
            characterClass = player.characterClass,
            isEnabled = isConscious,
            width = headWidth,
            hitPointsEmphasis = splash.hitPointsEmphasis,
            modifier =
            Modifier.sharedBounds(key = player.userId)
                .onGloballyPositioned {
                    onAvatarBoundsChanged(player.userId, it.boundsInRoot())
                }
                .clickable {
                    // TODO: Make this better
                    if (isCharmTarget) {
                        onChooseCharmTarget(player.userId)
                    } else if (isTargetingTap) {
                        onTargetOpponent(player.userId)
                    } else {
                        onInspect(player.userId)
                    }
                },
        )

        SpellPotencySplashIndicator(
            splash = splash,
            headWidth = headWidth,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
        )

        if (isUserPlayer) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_user_player),
                contentDescription = null,
                modifier =
                Modifier.align(alignment = Alignment.BottomCenter)
                    .padding(bottom = USER_MARKER_ICON_INSET)
                    .size(size = USER_MARKER_ICON_SIZE),
            )
        }
    }
}
