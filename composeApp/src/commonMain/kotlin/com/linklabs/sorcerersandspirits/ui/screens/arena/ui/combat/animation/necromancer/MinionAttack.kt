package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.MinionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.play
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion
import kotlin.math.roundToInt

@Composable
internal fun MinionGestureAvatar(
    minion: MinionState,
    displayIndex: Int,
    gestureState: AvatarGestureState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val area =
            Rect(
                offset = Offset.Zero,
                size =
                Size(
                    width = constraints.maxWidth.toFloat(),
                    height = constraints.maxHeight.toFloat(),
                ),
            )
        val slot =
            minionRestingSlot(displayIndex = displayIndex, minionType = minion.type, area = area)
                .toRect()
        val centeringOffsetX = area.center.x - slot.center.x

        MinionImage(
            minionType = minion.type,
            modifier =
            Modifier
                .offset {
                    IntOffset(
                        x = (slot.left + centeringOffsetX * gestureState.minionCharge.value).roundToInt(),
                        y = slot.top.roundToInt(),
                    )
                }
                .size(
                    width = with(receiver = density) { slot.width.toDp() },
                    height = with(receiver = density) { slot.height.toDp() },
                )
                .graphicsLayer { applyGesture(gestureState = gestureState) },
        )
    }
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
    MinionGestureAvatarPreview(minionType = MinionType.Skeleton)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MinionAttackGolemPreview() {
    MinionGestureAvatarPreview(minionType = MinionType.Golem)
}

@Composable
private fun MinionGestureAvatarPreview(minionType: MinionType) {
    val gestureState = remember { AvatarGestureState() }
    val density = LocalDensity.current
    var replay by remember { mutableIntStateOf(value = 0) }

    LaunchedEffect(key1 = replay) {
        gestureState.play(
            gesture = AvatarGesture.Lunge,
            strikeDirection = -1f,
            density = density,
            reactionDelayMillis = REACTION_DELAY_MILLIS,
        )
        replay++
    }

    SorcerersAndSpiritsTheme {
        Box(
            modifier =
            Modifier.fillMaxSize().paint(
                painter = painterResource(resource = Res.drawable.arena_background),
                contentScale = ContentScale.Crop,
            ),
        ) {
            MinionGestureAvatar(
                minion = MinionState(type = minionType, name = "Kebab", hasAttackedThisRound = false, hitPoints = 1),
                displayIndex = 0,
                gestureState = gestureState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
