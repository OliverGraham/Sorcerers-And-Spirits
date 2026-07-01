package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_speed
import sorcerersandspirits.composeapp.generated.resources.ic_user_player

private val haloWidth = 132.dp
private val haloHeight = 30.dp
private val haloShape = RoundedCornerShape(percent = 50)
private val haloInnerColor = Color(color = 0xFFCB9C6A)
private val haloOuterColor = Color(color = 0xFF654E35)
private val haloSweepColors = listOf(haloOuterColor, haloInnerColor, haloOuterColor)
private const val HALO_SWEEP_WIDTH_PX = 220f
private const val HALO_SWEEP_DURATION_MILLIS = 3000
private const val HALO_SWEEP_LABEL = "haloSweep"
private const val HALO_SWEEP_TRANSLATE_LABEL = "haloSweepTranslate"

private val statPillShape = RoundedCornerShape(size = 8.dp)
private val statPillIconSize = 24.dp

/**
 * The two combatants as full-size avatars stacked vertically with the resting [DiceHomeSlot]
 * between them (SS-157). The lower slot is empty when there is no second combatant so the dice stay
 * centered. The [ActivePlayerHalo] marks whoever is rolling, so the slots themselves carry no
 * meaning.
 */
@Composable
internal fun CombatAvatarColumn(
    topPlayer: PlayerState.Selected,
    bottomPlayer: PlayerState.Selected?,
    userPlayerId: String,
    activeUserIds: Set<String>,
    onInspect: (String) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LargeAvatar(
            player = topPlayer,
            isUserPlayer = topPlayer.userId == userPlayerId,
            isActiveRoller = topPlayer.userId in activeUserIds,
            onInspect = onInspect,
            modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
        )

        DiceHomeSlot(onBoundsChanged = onDiceHomeBoundsChanged)

        if (bottomPlayer != null) {
            LargeAvatar(
                player = bottomPlayer,
                isUserPlayer = bottomPlayer.userId == userPlayerId,
                isActiveRoller = bottomPlayer.userId in activeUserIds,
                onInspect = onInspect,
                modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
            )
        } else {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
    }
}

@Composable
private fun LargeAvatar(
    player: PlayerState.Selected,
    isUserPlayer: Boolean,
    isActiveRoller: Boolean,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classColor = player.characterClass.type.toColor()

    Row(
        modifier = modifier.clickable { onInspect(player.userId) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarStatsBar(
            characterClass = player.characterClass,
            isUserPlayer = isUserPlayer,
            classColor = classColor,
        )

        Box(
            modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (isActiveRoller) {
                ActivePlayerHalo()
            }

            ClassAvatar(
                characterClass = player.characterClass,
                modifier = Modifier.fillMaxSize().sharedBounds(key = player.userId),
            )
        }
    }
}

/**
 * The Speed / Armor / Hit-points pills shown beside an avatar, mirroring the inspect drawer's stat
 * markers (SS-157). The local player's identity marker sits atop the pills so the column reads as
 * "you", with each pill stacked vertically down the side of the avatar.
 */
@Composable
private fun AvatarStatsBar(
    characterClass: CharacterClassState,
    isUserPlayer: Boolean,
    classColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        if (isUserPlayer) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_user_player),
                contentDescription = null,
                modifier = Modifier.size(size = 44.dp),
            )
        }

        StatPill(
            icon = Res.drawable.ic_speed,
            value = characterClass.speed.toString(),
            classColor = classColor,
        )
        StatPill(
            icon = Res.drawable.ic_armor_class,
            value = characterClass.armorClass.toString(),
            classColor = classColor,
        )
        StatPill(
            icon = Res.drawable.ic_hit_points,
            value = characterClass.hitPointsDisplay,
            classColor = classColor,
        )
    }
}

@Composable
private fun StatPill(
    icon: DrawableResource,
    value: String,
    classColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(shape = statPillShape)
                .background(color = classColor)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = icon),
            contentDescription = null,
            modifier = Modifier.size(size = statPillIconSize),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/**
 * The glowing oval the active roller stands on (SS-157): the attacker, a defender mid-defensive
 * ability, or both combatants during a Teleport roll-off. Seated at the bottom of the avatar box so
 * the bottom-aligned art's feet rest on it. Drawn behind the art so the transparent base reads as a
 * halo on the ground.
 */
@Composable
private fun ActivePlayerHalo(modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = HALO_SWEEP_LABEL)
    val translate by
        sweep.animateFloat(
            initialValue = 0f,
            targetValue = HALO_SWEEP_WIDTH_PX,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(durationMillis = HALO_SWEEP_DURATION_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = HALO_SWEEP_TRANSLATE_LABEL,
        )

    Box(
        modifier =
            modifier
                .size(width = haloWidth, height = haloHeight)
                .clip(shape = haloShape)
                .background(
                    brush =
                        Brush.linearGradient(
                            colors = haloSweepColors,
                            start = Offset(x = translate, y = 0f),
                            end = Offset(x = translate + HALO_SWEEP_WIDTH_PX, y = 0f),
                            tileMode = TileMode.Repeated,
                        )
                )
    )
}

@Composable
internal fun ClassAvatar(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    // TODO(SS-068): combat animations will drive this static art.
    Image(
        painter = painterResource(resource = characterClass.type.toAvatarResource()),
        contentDescription = characterClass.className,
        contentScale = ContentScale.Fit,
        alignment = alignment,
        modifier = modifier,
    )
}
