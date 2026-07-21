package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import kotlin.math.roundToInt

private const val BADGE_WOBBLE_TRANSITION_LABEL = "diceBadgeWobble"
private const val BADGE_ROCK_LABEL = "diceBadgeRock"
private const val BADGE_TILT_LABEL = "diceBadgeTilt"
private const val BADGE_GROW_LABEL = "diceBadgeGrow"
private const val BADGE_ROCK_DEGREES = 27f
private const val BADGE_TILT_DEGREES = 20f
private const val BADGE_MIN_SCALE = 1f
private const val BADGE_MAX_SCALE = 1.12f
private const val BADGE_ROCK_PERIOD_MILLIS = 700
private const val BADGE_TILT_PERIOD_MILLIS = 1300
private const val BADGE_GROW_PERIOD_MILLIS = 950
private const val BADGE_CAMERA_DISTANCE = 10f

private val badgeUpOffset = 34.dp

private val doublesGold = Color(color = 0xFFFFC947)

/** The +1 / -1 chip floating above the resting dice. */
@Composable
internal fun DiceBadgeOverlay(
    diceBadgeState: DiceBadgeState?,
    homeCenter: Offset,
    modifier: Modifier = Modifier,
) {
    var badgeSize by remember { mutableStateOf(value = IntSize.Zero) }

    val upPx = with(receiver = LocalDensity.current) { badgeUpOffset.toPx() }
    val spacingPx = with(receiver = LocalDensity.current) { diceSpacing.toPx() }
    val targetX =
        dieRestCenterX(
            homeCenterX = homeCenter.x,
            diePosition = diceBadgeState?.diePosition ?: DiePosition.First,
            spacingPx = spacingPx,
        )

    val targetY = homeCenter.y - upPx

    AnimatedContent(
        targetState = diceBadgeState,
        modifier =
            modifier
                .offset {
                    IntOffset(
                        x = (targetX - badgeSize.width / 2f).roundToInt(),
                        y = (targetY - badgeSize.height / 2f).roundToInt(),
                    )
                }
                .onGloballyPositioned { badgeSize = it.size },
    ) {
        if (diceBadgeState != null) {
            DiceChip(diceBadgeState = diceBadgeState)
        }
    }
}

@Composable
private fun DiceChip(diceBadgeState: DiceBadgeState, modifier: Modifier = Modifier) {
    val isBonus = diceBadgeState is DiceBadgeState.DoublesBonus
    val chipColor = if (isBonus) doublesGold else CharacterClassType.Illusionist.toColor()
    val label = if (isBonus) "+1" else "−1"

    Box(
        modifier =
            modifier
                .badgeWobble()
                .background(color = chipColor, shape = CircleShape)
                .border(width = 1.5.dp, color = Color.White, shape = CircleShape)
                .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Modifier.badgeWobble(): Modifier {
    val transition = rememberInfiniteTransition(label = BADGE_WOBBLE_TRANSITION_LABEL)
    val rock by
        transition.animateFloat(
            initialValue = -BADGE_ROCK_DEGREES,
            targetValue = BADGE_ROCK_DEGREES,
            animationSpec = wobbleSpec(periodMillis = BADGE_ROCK_PERIOD_MILLIS),
            label = BADGE_ROCK_LABEL,
        )
    val tilt by
        transition.animateFloat(
            initialValue = -BADGE_TILT_DEGREES,
            targetValue = BADGE_TILT_DEGREES,
            animationSpec = wobbleSpec(periodMillis = BADGE_TILT_PERIOD_MILLIS),
            label = BADGE_TILT_LABEL,
        )
    val scale by
        transition.animateFloat(
            initialValue = BADGE_MIN_SCALE,
            targetValue = BADGE_MAX_SCALE,
            animationSpec = wobbleSpec(periodMillis = BADGE_GROW_PERIOD_MILLIS),
            label = BADGE_GROW_LABEL,
        )

    return graphicsLayer {
        rotationZ = rock
        rotationY = tilt
        scaleX = scale
        scaleY = scale
        cameraDistance = BADGE_CAMERA_DISTANCE * density
    }
}

private fun wobbleSpec(periodMillis: Int) =
    infiniteRepeatable<Float>(
        animation = tween(durationMillis = periodMillis, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
    )

@Preview @Composable private fun DiceModifierChipDoublesPreview() {}

@Preview @Composable private fun DiceModifierChipMirrorPreview() {}
