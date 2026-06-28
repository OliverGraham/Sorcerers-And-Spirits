package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.forAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_defensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_offensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_speed

private val STAT_ROW_SHAPE = RoundedCornerShape(size = 8.dp)
internal val STAT_ICON_SIZE = 28.dp
private val STAT_EXPLANATION_MAX_WIDTH = 150.dp
private val STAT_EXPLANATION_GAP = 4.dp

/**
 * One player's five stat rows stacked beside the buttons: Speed, Armor, Hit Points, and the
 * offensive/defensive ability markers. Each row shows an icon (plus a value for the numeric stats)
 * on the class color; tapping expands it toward the centre — rightward on the
 * [DisplayDirection.Left] side, leftward on the [DisplayDirection.Right] — to reveal a short
 * explanation (SS-146). Null while there is no target.
 */
@Composable
internal fun ClassAvatarStats(
    characterClass: CharacterClassState?,
    displayDirection: DisplayDirection,
    modifier: Modifier = Modifier,
) {
    if (characterClass == null) return

    val classCopy = LocalCharacterClassCopy.current
    val classColor = characterClass.type.toColor()
    val stats = remember(characterClass, classCopy) { avatarStats(characterClass, classCopy) }
    val rowAlignment =
        if (displayDirection == DisplayDirection.Left) Alignment.Start else Alignment.End

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = rowAlignment,
    ) {
        stats.forEach { stat ->
            StatRow(stat = stat, classColor = classColor, displayDirection = displayDirection)
        }
    }
}

@Composable
private fun StatRow(
    stat: AvatarStat,
    classColor: Color,
    displayDirection: DisplayDirection,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(value = false) }
    val isLeft = displayDirection == DisplayDirection.Left
    val explanationVisibility = remember { MutableTransitionState(initialState = false) }
    explanationVisibility.targetState = isExpanded

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier.clip(shape = STAT_ROW_SHAPE)
                    .background(color = classColor)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatIconAndValue(stat = stat, isLeft = isLeft)
        }

        val isExplanationMounted =
            explanationVisibility.currentState || explanationVisibility.targetState
        if (isExplanationMounted) {
            Popup(
                popupPositionProvider =
                    remember(isLeft) { StatExplanationPositionProvider(isLeft = isLeft) },
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = false),
            ) {
                StatExplanation(
                    stat = stat,
                    classColor = classColor,
                    visibility = explanationVisibility,
                    isLeft = isLeft,
                )
            }
        }
    }
}

@Composable
private fun StatIconAndValue(stat: AvatarStat, isLeft: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon: @Composable () -> Unit = {
            Image(
                painter = painterResource(resource = stat.icon),
                contentDescription = stat.name,
                modifier = Modifier.size(size = STAT_ICON_SIZE),
            )
        }
        val value: @Composable () -> Unit = {
            if (stat.value != null) {
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        if (isLeft) {
            icon()
            value()
        } else {
            value()
            icon()
        }
    }
}

@Composable
private fun StatExplanation(
    stat: AvatarStat,
    classColor: Color,
    visibility: MutableTransitionState<Boolean>,
    isLeft: Boolean,
) {
    val edge = if (isLeft) Alignment.Start else Alignment.End

    AnimatedVisibility(
        visibleState = visibility,
        enter = expandHorizontally(expandFrom = edge) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = edge) + fadeOut(),
    ) {
        Column(
            modifier =
                Modifier.padding(horizontal = STAT_EXPLANATION_GAP)
                    .clip(shape = STAT_ROW_SHAPE)
                    .background(color = classColor)
                    .widthIn(max = STAT_EXPLANATION_MAX_WIDTH)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = stat.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stat.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
    }
}

/**
 * Seats a stat row's explanation popup flush against the pill's inner edge and vertically centered
 * on it: rightward from the pill on the [DisplayDirection.Left] column, leftward on the right,
 * clamped to stay within the window. Because it is a popup it overlays the buttons without taking
 * part in the stat column's layout, so expanding a row never shifts its neighbors (SS-156).
 */
private class StatExplanationPositionProvider(private val isLeft: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (isLeft) anchorBounds.right else anchorBounds.left - popupContentSize.width
        val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(minimumValue = 0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(minimumValue = 0)
        return IntOffset(
            x = x.coerceIn(minimumValue = 0, maximumValue = maxX),
            y = y.coerceIn(minimumValue = 0, maximumValue = maxY),
        )
    }
}

internal data class AvatarStat(
    val icon: DrawableResource,
    val value: String?,
    val name: String,
    val explanation: String,
)

internal fun avatarStats(
    characterClass: CharacterClassState,
    classCopy: CharacterClassCopy,
): List<AvatarStat> {
    val speed = classCopy[ClassCopyKeys.SPEED]
    val armor = classCopy[ClassCopyKeys.ARMOR]
    val health = classCopy[ClassCopyKeys.HEALTH]
    val offensive = classCopy.forAbility(ability = characterClass.offensiveAbility)
    val defensive = classCopy.forAbility(ability = characterClass.defensiveAbility)

    return listOf(
        AvatarStat(
            icon = Res.drawable.ic_speed,
            value = characterClass.speed.toString(),
            name = speed.name,
            explanation = speed.short,
        ),
        AvatarStat(
            icon = Res.drawable.ic_armor_class,
            value = characterClass.armorClass.toString(),
            name = armor.name,
            explanation = armor.short,
        ),
        AvatarStat(
            icon = Res.drawable.ic_hit_points,
            value = characterClass.hitPointsDisplay,
            name = health.name,
            explanation = health.short,
        ),
        AvatarStat(
            icon = Res.drawable.ic_offensive_ability,
            value = null,
            name = offensive.name,
            explanation = offensive.short,
        ),
        AvatarStat(
            icon = Res.drawable.ic_defensive_ability,
            value = null,
            name = defensive.name,
            explanation = defensive.short,
        ),
    )
}

internal enum class DisplayDirection {
    Left,
    Right,
}
