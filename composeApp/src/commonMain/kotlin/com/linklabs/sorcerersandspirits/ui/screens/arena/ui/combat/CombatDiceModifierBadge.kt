package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Rect
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadge
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
private const val BADGE_ENTER_SCALE = 0.4f

private val badgeRightOffset = 46.dp
private val badgeUpOffset = 34.dp

private val doublesGold = Color(color = 0xFFFFC947)

/**
 * The +1 / -1 modifier chip floating above the resting dice, wobbling in a lively 3D loop while it
 * is shown. The banked doubles +1 rides at the upper-right of the pair and folds away as the next
 * roll consumes it; the Mirror Image -1 anchors above the specific die it penalized ([dieSlot]) and
 * rides along while that reroll is the dice' last roll. Drawn in a screen-spanning overlay so it
 * can be anchored to the shared [homeBounds] dice slot.
 */
@Composable
internal fun DiceModifierBadgeOverlay(
    badge: DiceBadge?,
    dieSlot: Int?,
    homeBounds: Rect,
    modifier: Modifier = Modifier,
) {
    var overlayOrigin by remember { mutableStateOf(value = Offset.Zero) }
    var badgeSize by remember { mutableStateOf(value = IntSize.Zero) }
    var shownBadge by remember { mutableStateOf<DiceBadge?>(value = null) }
    if (badge != null) shownBadge = badge

    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                overlayOrigin = it.localToRoot(relativeToLocal = Offset.Zero)
            }
    ) {
        val rightPx = with(receiver = LocalDensity.current) { badgeRightOffset.toPx() }
        val upPx = with(receiver = LocalDensity.current) { badgeUpOffset.toPx() }
        val spacingPx = with(receiver = LocalDensity.current) { diceSpacing.toPx() }
        val homeCenter = homeBounds.center - overlayOrigin
        val targetX =
            if (dieSlot != null) {
                dieRestCenterX(homeCenterX = homeCenter.x, slot = dieSlot, spacingPx = spacingPx)
            } else {
                homeCenter.x + rightPx
            }
        val targetY = homeCenter.y - upPx

        AnimatedVisibility(
            visible = badge != null,
            enter = fadeIn() + scaleIn(initialScale = BADGE_ENTER_SCALE),
            exit = fadeOut() + scaleOut(targetScale = BADGE_ENTER_SCALE),
            modifier =
                Modifier.offset {
                        IntOffset(
                            x = (targetX - badgeSize.width / 2f).roundToInt(),
                            y = (targetY - badgeSize.height / 2f).roundToInt(),
                        )
                    }
                    .onGloballyPositioned { badgeSize = it.size },
        ) {
            val renderedBadge = shownBadge
            if (renderedBadge != null) DiceModifierChip(badge = renderedBadge)
        }
    }
}

@Composable
private fun DiceModifierChip(badge: DiceBadge, modifier: Modifier = Modifier) {
    val isBonus = badge is DiceBadge.DoublesBonus
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

@Preview
@Composable
private fun DiceModifierChipDoublesPreview() {
    Box(modifier = Modifier.background(color = Color(color = 0xFF1A1A1A)).padding(all = 24.dp)) {
        DiceModifierChip(badge = DiceBadge.DoublesBonus)
    }
}

@Preview
@Composable
private fun DiceModifierChipMirrorPreview() {
    Box(modifier = Modifier.background(color = Color(color = 0xFF1A1A1A)).padding(all = 24.dp)) {
        DiceModifierChip(badge = DiceBadge.MirrorPenalty)
    }
}
