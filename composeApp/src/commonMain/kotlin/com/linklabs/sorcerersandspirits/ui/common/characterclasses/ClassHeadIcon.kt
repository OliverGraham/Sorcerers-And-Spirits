package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_speed

private val DEFAULT_HEAD_ICON_WIDTH = 132.dp

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
private const val HEAD_AVATAR_DIAMETER_FRACTION = 0.54f
private const val STAT_ICON_DIAMETER_FRACTION = 0.27f
private const val VALUE_TEXT_FRACTION = 0.13f
private const val VALUE_SHADOW_FRACTION = 0.012f
private const val MEDALLION_RING_FRACTION = 0.02f

// TODO(SS-136): placeholder for the wooden medallion frame until the designer separates the head
// from the medallion so the real frame asset can sit around the avatar.
private val MEDALLION_RING_COLOR = Color(color = 0xFF5A4326)

/**
 * The collapsed, at-a-glance "class head" shown for a player in the Arena while they are not the
 * active roller. A cluster of class-colored bordered circles — one behind the head and one behind
 * each stat icon and value — overlaps to form the cloud silhouette. Speed sits on top (value to its
 * right), Armor at the start (value below), Hitpoints at the end (value below). The expanded detail
 * view is a separate concern (SS-141).
 */
@Composable
fun ClassHeadIcon(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
    width: Dp = DEFAULT_HEAD_ICON_WIDTH,
) {
    val height = width / ASPECT_RATIO
    val circleColors = classCircleColors(type = characterClass.type)

    Box(
        modifier = modifier.width(width = width).aspectRatio(ratio = ASPECT_RATIO),
        contentAlignment = Alignment.Center,
    ) {
        BackingCircles(colors = circleColors, modifier = Modifier.matchParentSize())

        // TODO(SS-136): swap this circular crop of the class art for the combined head + wooden
        // medallion asset once the designer separates those elements in Figma.
        OverlayAt(center = HEAD_CENTER, width = width, height = height) {
            Image(
                painter = painterResource(resource = classArtwork(type = characterClass.type)),
                contentDescription = characterClass.className,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier =
                    Modifier.size(size = width * HEAD_AVATAR_DIAMETER_FRACTION)
                        .clip(shape = CircleShape)
                        .border(
                            width = width * MEDALLION_RING_FRACTION,
                            color = MEDALLION_RING_COLOR,
                            shape = CircleShape,
                        ),
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
private fun BackingCircles(colors: ClassCircleColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Drawn back-to-front: the stat circles sit furthest back and the head circle is drawn last
        // so it overlaps them, tucking the stat circles behind the head to scallop the silhouette.
        val circles =
            listOf(
                SPEED_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
                ARMOR_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
                HIT_POINTS_ICON_CENTER to ICON_BACKING_RADIUS_FRACTION,
                SPEED_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                ARMOR_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                HIT_POINTS_VALUE_CENTER to VALUE_BACKING_RADIUS_FRACTION,
                HEAD_CENTER to HEAD_BACKING_RADIUS_FRACTION,
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

private class ClassCircleColors(val light: Color, val dark: Color)

// TODO(SS-137): centralize per-class colors (currently mirrored from
// CharacterClassWheel.spokeColor)
// so the head icon, wheel, and future Arena UI all tint from one source.
private fun classCircleColors(type: CharacterClassType): ClassCircleColors =
    when (type) {
        CharacterClassType.Rogue,
        CharacterClassType.Duelist -> ClassCircleColors(light = RED_LIGHT, dark = RED_DARK)

        CharacterClassType.Gambler,
        CharacterClassType.Bard -> ClassCircleColors(light = YELLOW_LIGHT, dark = YELLOW_DARK)

        CharacterClassType.Necromancer,
        CharacterClassType.Paladin -> ClassCircleColors(light = GREEN_LIGHT, dark = GREEN_DARK)

        CharacterClassType.Illusionist,
        CharacterClassType.Mage -> ClassCircleColors(light = BLUE_LIGHT, dark = BLUE_DARK)

        CharacterClassType.None -> ClassCircleColors(light = RED_LIGHT, dark = RED_DARK)
    }

private val RED_LIGHT = Color(color = 0xFF9D2920)
private val RED_DARK = Color(color = 0xFF581310)
private val YELLOW_LIGHT = Color(color = 0xFFDAA73B)
private val YELLOW_DARK = Color(color = 0xFF7A5E14)
private val GREEN_LIGHT = Color(color = 0xFF3C7A3C)
private val GREEN_DARK = Color(color = 0xFF1E3D1E)
private val BLUE_LIGHT = Color(color = 0xFF1F3A5D)
private val BLUE_DARK = Color(color = 0xFF0E1C2E)

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
