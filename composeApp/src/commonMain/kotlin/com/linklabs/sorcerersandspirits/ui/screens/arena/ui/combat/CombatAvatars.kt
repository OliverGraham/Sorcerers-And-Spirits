package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_attack_button
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
private const val ATTACKS_COUNT_LABEL = "attacksCount"

/**
 * The two combatants as full-size avatars stacked vertically with the resting [DiceHomeSlot]
 * between them (SS-157). Either seat is empty when it has no combatant so the dice stay centered.
 * The seats are assigned by identity — the user's player always sits at the bottom (SS-181) — and
 * the [ActivePlayerHalo] marks whoever is rolling, so the seats themselves carry no meaning.
 *
 * The [WeaponStrikeOverlay] spans both seats so a class weapon can fly from one avatar to the other
 * (SS-192); each seat reports its measured bounds so the strike lands on the defender art.
 */
@Composable
internal fun CombatAvatarColumn(
    topPlayer: PlayerState.Selected?,
    bottomPlayer: PlayerState.Selected?,
    animationCue: CombatAnimationCue?,
    userPlayerId: String,
    activeUserIds: Set<String>,
    turnOwnerUserId: String,
    onInspect: (String) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarBoundsByUserId = remember { mutableStateMapOf<String, Rect>() }
    var stageBounds by remember { mutableStateOf(value = Rect.Zero) }

    Box(
        modifier = modifier.fillMaxSize().onGloballyPositioned { stageBounds = it.boundsInRoot() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SeatedAvatar(
                player = topPlayer,
                animationCue = animationCue,
                isTopSeat = true,
                userPlayerId = userPlayerId,
                activeUserIds = activeUserIds,
                turnOwnerUserId = turnOwnerUserId,
                onInspect = onInspect,
                onAvatarBoundsChanged = { userId, bounds -> avatarBoundsByUserId[userId] = bounds },
            )

            DiceHomeSlot(onBoundsChanged = onDiceHomeBoundsChanged)

            SeatedAvatar(
                player = bottomPlayer,
                animationCue = animationCue,
                isTopSeat = false,
                userPlayerId = userPlayerId,
                activeUserIds = activeUserIds,
                turnOwnerUserId = turnOwnerUserId,
                onInspect = onInspect,
                onAvatarBoundsChanged = { userId, bounds -> avatarBoundsByUserId[userId] = bounds },
            )
        }

        val avatarBoundsInStage: (String) -> Rect? = { userId ->
            val bounds = avatarBoundsByUserId[userId]
            bounds?.translate(offset = -stageBounds.topLeft)
        }

        WeaponStrikeOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBoundsInStage,
            modifier = Modifier.fillMaxSize(),
        )

        AllInOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBoundsInStage,
            modifier = Modifier.fillMaxSize(),
        )

        LuckyCoinOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBoundsInStage,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ColumnScope.SeatedAvatar(
    player: PlayerState.Selected?,
    animationCue: CombatAnimationCue?,
    isTopSeat: Boolean,
    userPlayerId: String,
    activeUserIds: Set<String>,
    turnOwnerUserId: String,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
) {
    if (player != null) {
        LargeAvatar(
            player = player,
            animationCue = animationCue,
            isTopSeat = isTopSeat,
            isUserPlayer = player.userId == userPlayerId,
            isActiveRoller = player.userId in activeUserIds,
            isTurnOwner = player.userId == turnOwnerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
            modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
        )
    } else {
        Spacer(modifier = Modifier.weight(weight = 1f))
    }
}

@Composable
private fun LargeAvatar(
    player: PlayerState.Selected,
    animationCue: CombatAnimationCue?,
    isTopSeat: Boolean,
    isUserPlayer: Boolean,
    isActiveRoller: Boolean,
    isTurnOwner: Boolean,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classColor = player.characterClass.type.toColor()
    val gestureState =
        rememberAvatarGesture(
            animationCue = animationCue,
            userId = player.userId,
            isTopSeat = isTopSeat,
        )

    Row(
        modifier = modifier.clickable { onInspect(player.userId) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarStatsBar(
            characterClass = player.characterClass,
            isUserPlayer = isUserPlayer,
            isTurnOwner = isTurnOwner,
            classColor = classColor,
        )

        Box(
            modifier =
                Modifier.weight(weight = 1f).fillMaxWidth().onGloballyPositioned {
                    onAvatarBoundsChanged(player.userId, it.boundsInRoot())
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (isActiveRoller) {
                ActivePlayerHalo()
            }

            GestureClassAvatar(
                characterClass = player.characterClass,
                gestureState = gestureState,
                modifier = Modifier.fillMaxSize().sharedBounds(key = player.userId),
            )
        }
    }
}

/**
 * The Speed / Armor / Hit-points pills shown beside an avatar, mirroring the inspect drawer's stat
 * markers (SS-157). The local player's identity marker sits atop the pills so the column reads as
 * "you", with each pill stacked vertically down the side of the avatar. An attacks-remaining pill
 * joins them only while this avatar's player owns the turn (SS-200).
 */
@Composable
private fun AvatarStatsBar(
    characterClass: CharacterClassState,
    isUserPlayer: Boolean,
    isTurnOwner: Boolean,
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
            value = characterClass.armorClassDisplay,
            classColor = classColor,
        )
        StatPill(
            icon = Res.drawable.ic_hit_points,
            value = characterClass.hitPointsDisplay,
            classColor = classColor,
        )

        AnimatedVisibility(visible = isTurnOwner) {
            AttacksStatPill(count = characterClass.attacksRemaining, classColor = classColor)
        }
    }
}

@Composable
private fun StatPill(
    icon: DrawableResource,
    value: String,
    classColor: Color,
    modifier: Modifier = Modifier,
) {
    StatPillFrame(icon = icon, classColor = classColor, modifier = modifier) {
        StatPillValue(value = value)
    }
}

/**
 * The turn owner's attacks-remaining pill, shown only while it is their turn. The number swaps with
 * an [AnimatedContent] so a Necromancer's follow-ups tick down (or a fresh count fades in) rather
 * than snapping.
 */
@Composable
private fun AttacksStatPill(count: Int, classColor: Color, modifier: Modifier = Modifier) {
    StatPillFrame(
        icon = Res.drawable.ic_attack_button,
        classColor = classColor,
        modifier = modifier,
    ) {
        AnimatedContent(targetState = count, label = ATTACKS_COUNT_LABEL) { attacks ->
            StatPillValue(value = attacks.toString())
        }
    }
}

@Composable
private fun StatPillFrame(
    icon: DrawableResource,
    classColor: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
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
        value()
    }
}

@Composable
private fun StatPillValue(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
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
    colorFilter: ColorFilter? = null,
) {
    Image(
        painter = painterResource(resource = characterClass.type.toAvatarResource()),
        contentDescription = characterClass.className,
        contentScale = ContentScale.Fit,
        alignment = alignment,
        colorFilter = colorFilter,
        modifier = modifier,
    )
}
