package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.linklabs.sorcerersandspirits.domain.game.MinionType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.play
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion
import kotlin.math.roundToInt

private const val PREVIEW_MINION_HEIGHT_FRACTION = 0.45f

@Composable
internal fun MinionAttackOverlay(
    minionType: MinionType,
    minionBounds: Rect,
    isTopSeat: Boolean,
    gesture: AvatarGesture,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        MinionGestureAvatar(
            minionType = minionType,
            gesture = gesture,
            isTopSeat = isTopSeat,
            onFinished = onFinished,
            modifier =
            Modifier.offset {
                IntOffset(x = minionBounds.left.roundToInt(), y = minionBounds.top.roundToInt())
            }.size(
                width = with(receiver = density) { minionBounds.width.toDp() },
                height = with(receiver = density) { minionBounds.height.toDp() },
            ),
        )
    }
}

@Composable
private fun MinionGestureAvatar(
    minionType: MinionType,
    gesture: AvatarGesture,
    isTopSeat: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureState = remember { AvatarGestureState() }
    val density = LocalDensity.current

    LaunchedEffect(key1 = gesture) {
        gestureState.play(
            gesture = gesture,
            strikeDirection = if (isTopSeat) 1f else -1f,
            density = density,
            reactionDelayMillis = REACTION_DELAY_MILLIS,
        )
        onFinished()
    }

    MinionImage(
        minionType = minionType,
        modifier = modifier.graphicsLayer { applyGesture(gestureState = gestureState) },
    )
}

@Composable
private fun MinionImage(minionType: MinionType, modifier: Modifier = Modifier) {
    val resource = when (minionType) {
        MinionType.Skeleton -> Res.drawable.skeleton_minion
        MinionType.Golem -> Res.drawable.golem_minion
    }

    Image(
        painter = painterResource(resource = resource),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
        modifier = modifier,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MinionAttackSkeletonPreview() {
    MinionAttackOverlayPreview(minionType = MinionType.Skeleton)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MinionAttackGolemPreview() {
    MinionAttackOverlayPreview(minionType = MinionType.Golem)
}

@Composable
private fun MinionAttackOverlayPreview(minionType: MinionType) {
    val avatars = rememberDuoAvatars()
    var replay by remember { mutableIntStateOf(value = 0) }

    SorcerersAndSpiritsTheme {
        Box(
            modifier =
            Modifier.fillMaxSize().paint(
                painter = painterResource(resource = Res.drawable.arena_background),
                contentScale = ContentScale.Crop,
            ),
        ) {
            val actorBounds = avatars.boundsFor(userId = PREVIEW_ACTOR_ID)
            val minionHeight = actorBounds.height * PREVIEW_MINION_HEIGHT_FRACTION
            val minionBounds = Rect(
                offset = Offset(x = actorBounds.center.x - minionHeight / 2f, y = actorBounds.bottom - minionHeight),
                size = Size(width = minionHeight, height = minionHeight),
            )

            key(replay) {
                MinionAttackOverlay(
                    minionType = minionType,
                    minionBounds = minionBounds,
                    isTopSeat = false,
                    gesture = AvatarGesture.Lunge,
                    onFinished = { replay++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
