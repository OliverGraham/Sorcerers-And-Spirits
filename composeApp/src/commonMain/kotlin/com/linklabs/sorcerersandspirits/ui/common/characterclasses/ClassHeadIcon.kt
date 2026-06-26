package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_bust_background
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_speed

private val DEFAULT_HEAD_ICON_WIDTH = 132.dp

// A knocked-out player's icon is shown faded and drained of class color so it reads as out of the
// fight (SS-152). Placeholder treatment — the finalized disabled design lands later.
private const val DISABLED_ALPHA = 0.45f
private val DISABLED_GRADIENT =
    ClassGradient(light = Color(color = 0xFF8A8A8A), dark = Color(color = 0xFF4A4A4A))

// The Figma frame is wider than it is tall; keep that footprint so the laid-out circles match.
private const val ASPECT_RATIO = 176.627f / 154.811f

// Normalised element centres (fraction of width / height) lifted straight from the Figma node so
// the overlapping circles, icons and numbers land where the designer placed them.
private val HEAD_CENTER = Offset(x = 0.500f, y = 0.526f)
private val SPEED_ICON_CENTER = Offset(x = 0.476f, y = 0.207f)
private val SPEED_VALUE_CENTER = Offset(x = 0.667f, y = 0.165f)
private val ARMOR_ICON_CENTER = Offset(x = 0.196f, y = 0.504f)
private val ARMOR_VALUE_CENTER = Offset(x = 0.203f, y = 0.791f)
private val HIT_POINTS_ICON_CENTER = Offset(x = 0.804f, y = 0.504f)
private val HIT_POINTS_VALUE_CENTER = Offset(x = 0.837f, y = 0.772f)

// Radii / sizes as a fraction of the icon width.
private const val HEAD_BACKING_RADIUS_FRACTION = 0.30f
private const val ICON_BACKING_RADIUS_FRACTION = 0.155f
private const val VALUE_BACKING_RADIUS_FRACTION = 0.11f
private const val HEAD_FRAME_SIZE_FRACTION = 0.62f
private const val HEAD_AVATAR_DIAMETER_FRACTION = 0.40f
private const val STAT_ICON_DIAMETER_FRACTION = 0.27f
private const val VALUE_TEXT_FRACTION = 0.13f
private const val VALUE_SHADOW_FRACTION = 0.012f

/**
 * The collapsed, at-a-glance "class head" shown for a player in the Arena while they are not the
 * active roller. A cluster of class-colored gradient circles — one behind the head and one behind
 * each stat icon and value — overlaps to form the cloud silhouette, with the class avatar mounted
 * in a wooden frame at the centre. Speed sits on top (value to its right), Armor at the start
 * (value below), Hitpoints at the end (value below). The expanded detail view is a separate concern
 * (SS-150). A knocked-out player is shown with [isEnabled] false: faded and gray rather than
 * class-colored, so it reads as out of the fight (SS-152).
 */
@Composable
fun ClassHeadIcon(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
    width: Dp = DEFAULT_HEAD_ICON_WIDTH,
    isEnabled: Boolean = true,
) {
    val height = width / ASPECT_RATIO
    val circleColors =
        if (isEnabled) classGradient(type = characterClass.type) else DISABLED_GRADIENT
    val disabledModifier = if (isEnabled) Modifier else Modifier.alpha(alpha = DISABLED_ALPHA)

    Box(
        modifier =
            modifier.width(width = width).aspectRatio(ratio = ASPECT_RATIO).then(disabledModifier),
        contentAlignment = Alignment.Center,
    ) {
        BackingCircles(colors = circleColors, modifier = Modifier.matchParentSize())

        OverlayAt(center = HEAD_CENTER, width = width, height = height) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_bust_background),
                contentDescription = null,
                modifier = Modifier.size(size = width * HEAD_FRAME_SIZE_FRACTION),
            )
        }

        // TODO(SS-136): the class avatar art doubles as the head bust; swap for dedicated head art
        // if the designer provides it.
        OverlayAt(center = HEAD_CENTER, width = width, height = height) {
            Image(
                painter = painterResource(resource = characterClass.type.toBustResource()),
                contentDescription = characterClass.className,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size = width * HEAD_AVATAR_DIAMETER_FRACTION),
            )
        }

        StatIcon(
            icon = Res.drawable.ic_speed,
            center = SPEED_ICON_CENTER,
            width = width,
            height = height,
        )
        StatIcon(
            icon = Res.drawable.ic_armor_class,
            center = ARMOR_ICON_CENTER,
            width = width,
            height = height,
        )
        StatIcon(
            icon = Res.drawable.ic_hit_points,
            center = HIT_POINTS_ICON_CENTER,
            width = width,
            height = height,
        )

        StatValue(
            value = characterClass.speed,
            center = SPEED_VALUE_CENTER,
            width = width,
            height = height,
        )
        StatValue(
            value = characterClass.armorClass,
            center = ARMOR_VALUE_CENTER,
            width = width,
            height = height,
        )
        StatValue(
            value = characterClass.hitPoints,
            center = HIT_POINTS_VALUE_CENTER,
            width = width,
            height = height,
        )
    }
}

@Composable
private fun BackingCircles(colors: ClassGradient, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Drawn back-to-front: the stat-value circles sit furthest back, then the head's centre
        // circle, then the icon circles in front — so each stat's value circle tucks behind the
        // circle holding its icon.
        val circles =
            listOf(
                SPEED_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                ARMOR_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                HIT_POINTS_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                HEAD_CENTER to HEAD_BACKING_RADIUS_FRACTION,
                SPEED_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
                ARMOR_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
                HIT_POINTS_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
            )

        circles.forEach { (center, radiusFraction) ->
            val pixelCenter = Offset(x = center.x * size.width, y = center.y * size.height)
            val radius = size.width * radiusFraction
            drawCircle(
                brush =
                    Brush.linearGradient(
                        colors = listOf(colors.light, colors.dark),
                        start = Offset(x = pixelCenter.x, y = pixelCenter.y - radius),
                        end = Offset(x = pixelCenter.x, y = pixelCenter.y + radius),
                    ),
                radius = radius,
                center = pixelCenter,
            )
        }
    }
}

@Composable
private fun BoxScope.StatIcon(icon: DrawableResource, center: Offset, width: Dp, height: Dp) {
    OverlayAt(center = center, width = width, height = height) {
        Image(
            painter = painterResource(resource = icon),
            contentDescription = null,
            modifier = Modifier.size(size = width * STAT_ICON_DIAMETER_FRACTION),
        )
    }
}

@Composable
private fun BoxScope.StatValue(value: Int, center: Offset, width: Dp, height: Dp) {
    OverlayAt(center = center, width = width, height = height) {
        Text(
            text = value.toString(),
            style =
                TextStyle(
                    color = Color.White,
                    fontSize = (width.value * VALUE_TEXT_FRACTION).sp,
                    fontWeight = FontWeight.Bold,
                    shadow =
                        Shadow(
                            color = Color.Black,
                            offset = Offset(x = 0f, y = width.value * VALUE_SHADOW_FRACTION),
                            blurRadius = width.value * VALUE_SHADOW_FRACTION,
                        ),
                ),
        )
    }
}

/** Places [content] so its centre lands on the normalised [center] within the icon box. */
@Composable
private fun BoxScope.OverlayAt(
    center: Offset,
    width: Dp,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.align(alignment = Alignment.Center).offset {
                IntOffset(
                    x = (width.toPx() * (center.x - 0.5f)).roundToInt(),
                    y = (height.toPx() * (center.y - 0.5f)).roundToInt(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview
@Composable
private fun ClassHeadIconPreview() {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                ClassHeadIcon(
                    characterClass = previewCharacterClassState(type = CharacterClassType.Rogue)
                )
                ClassHeadIcon(
                    characterClass = previewCharacterClassState(type = CharacterClassType.Mage)
                )
            }
        }
    }
}
