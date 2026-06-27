package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.ui.common.AnimatedSeconds
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.forAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.diceCount
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.flashText
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.headIconPlayersFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isImpersonateRedundant
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.resolveTargetUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.targetPlayerFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DIE_RESTING_FACE
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.Die3d
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.rememberDiceRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.ArenaMessageLog
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_attack_button
import sorcerersandspirits.composeapp.generated.resources.ic_defensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_messages
import sorcerersandspirits.composeapp.generated.resources.ic_offensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_speed
import sorcerersandspirits.composeapp.generated.resources.ic_teleport_button

private const val TARGET_TRANSITION_LABEL = "NewArenaCombatTargetTransition"

private const val TIMER_PULSE_TRANSITION_LABEL = "TurnTimerPulseTransition"
private const val TIMER_PULSE_SCALE_LABEL = "TurnTimerPulseScale"
private const val TIMER_PULSE_DURATION_MILLIS = 500
private const val TIMER_PULSE_MAX_SCALE = 1.3f
private val TIMER_FONT_SIZE = 28.sp
private val TIMER_CORNER_PADDING = 12.dp

private val MESSAGES_ICON_SIZE = 56.dp
private val UNREAD_COUNT_FONT_SIZE = 20.sp

private val PLACEHOLDER_PANEL_GOLD = Color(color = 0xFFB8860B)

private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f

private val ARENA_BUTTON_SIZE = 72.dp
private const val DISABLED_BUTTON_ALPHA = 0.4f

private const val DICE_HOME_DIE_COUNT = 2

private val DICE_HOME_HEIGHT = 96.dp
private val DICE_REST_SPACING = 76.dp

private val STAT_ROW_SHAPE = RoundedCornerShape(size = 8.dp)
private val STAT_ICON_SIZE = 28.dp
private val STAT_EXPLANATION_MAX_WIDTH = 150.dp

private const val FLASH_HOLD_MILLIS = 850L
private val FLASH_FONT_SIZE = 72.sp

private const val PI_FLOAT = 3.1415927f
private const val CONFETTI_COUNT = 24
private const val CONFETTI_MILLIS = 900
private const val CONFETTI_MIN_DISTANCE = 80f
private const val CONFETTI_MAX_DISTANCE = 220f
private const val CONFETTI_MIN_RADIUS = 4f
private const val CONFETTI_MAX_RADIUS = 9f

private val INSPECT_DRAWER_WIDTH = 280.dp
private val INSPECT_AVATAR_HEIGHT = 200.dp
private val INSPECT_DRAWER_COLOR = Color(color = 0xF21A1A1A)
private val INSPECT_SCRIM = Color(color = 0x99000000)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NewArenaCombat(
    state: ArenaState.Combat,
    messages: List<ArenaMessageState>,
    flashMessage: ArenaMessageState?,
    secondsRemaining: Int,
    isTimerVisible: Boolean,
    isTimerUrgent: Boolean,
    modifier: Modifier = Modifier,
) {
    val diceRoll = rememberDiceRoll()

    var selectedOpponentUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = "") }

    var inspectedUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf<String?>(value = null) }
    val inspectedPlayer = inspectedUserId?.let { state.playerMap[it] }

    var isTargetLockedOptimistically by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = false) }
    val isTargetable =
        state.isItThisUsersTurn && state.lockedTargetUserId == null && !isTargetLockedOptimistically
    val lockTarget = { isTargetLockedOptimistically = true }
    val targetedOpponentUserId =
        state.resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)
    val avatarPairing =
        AvatarPairing(activeUserId = state.turnOwnerUserId, targetUserId = targetedOpponentUserId)

    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = avatarPairing,
                    label = TARGET_TRANSITION_LABEL,
                    modifier = Modifier.weight(weight = 1f),
                ) { currentPairing ->
                    SharedTransitionProvider(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    ) {
                        TransitioningContent(
                            state = state,
                            activeUserId = currentPairing.activeUserId,
                            currentTargetUserId = currentPairing.targetUserId,
                            onSelectTarget = { selectedOpponentUserId = it },
                            onInspect = { inspectedUserId = it },
                            isTargetable = isTargetable,
                            messages = messages,
                        )
                    }
                }

                DiceHomeSlot(onBoundsChanged = { diceHomeBounds = it })

                BottomControls(
                    state = state,
                    targetedOpponentUserId = targetedOpponentUserId,
                    lockTarget = lockTarget,
                    diceRoll = diceRoll,
                )
            }

            TurnTimer(
                secondsRemaining = secondsRemaining,
                isVisible = isTimerVisible,
                isUrgent = isTimerUrgent,
                modifier = Modifier.align(alignment = Alignment.TopEnd),
            )

            DiceOverlay(
                diceRoll = diceRoll,
                homeBounds = diceHomeBounds,
                modifier = Modifier.fillMaxSize(),
            )

            FlashOverlay(flashMessage = flashMessage, modifier = Modifier.fillMaxSize())

            InspectDrawer(
                player = inspectedPlayer,
                onClose = { inspectedUserId = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The two players seated in the bottom-half avatars: the active turn owner on the left and the
 * targeted player on the right (SS-157). Keying the avatar [AnimatedContent] on this pairing lets
 * both slots morph when the turn passes or the target changes, not just the target.
 */
@Immutable private data class AvatarPairing(val activeUserId: String, val targetUserId: String)

/**
 * The combat turn clock, pinned in the top-end corner. It shows only in the final stretch of a turn
 * ([isVisible], the last ~15s) and pulses in the player's [error] color as it runs out ([isUrgent],
 * the last ~10s). Presentation only — the 45s turn timer itself ticks in the ViewModel (SS-155).
 */
@Composable
private fun TurnTimer(
    secondsRemaining: Int,
    isVisible: Boolean,
    isUrgent: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val pulseScale = if (isUrgent) timerPulseScale() else 1f
    val color =
        if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    AnimatedSeconds(
        seconds = secondsRemaining,
        modifier = modifier.padding(all = TIMER_CORNER_PADDING),
    ) { seconds ->
        Text(
            text = "$seconds",
            color = color,
            fontSize = TIMER_FONT_SIZE,
            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.SemiBold,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
        )
    }
}

/** The looping 1f→[TIMER_PULSE_MAX_SCALE] heartbeat the timer rides while it is urgent. */
@Composable
private fun timerPulseScale(): Float {
    val pulseTransition = rememberInfiniteTransition(label = TIMER_PULSE_TRANSITION_LABEL)
    return pulseTransition
        .animateFloat(
            initialValue = 1f,
            targetValue = TIMER_PULSE_MAX_SCALE,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = TIMER_PULSE_DURATION_MILLIS),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = TIMER_PULSE_SCALE_LABEL,
        )
        .value
}

@Composable
private fun TransitioningContent(
    state: ArenaState.Combat,
    activeUserId: String,
    currentTargetUserId: String,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    isTargetable: Boolean,
    messages: List<ArenaMessageState>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopHalfContent(
            headIconPlayers =
                state.headIconPlayersFor(
                    activeUserId = activeUserId,
                    targetUserId = currentTargetUserId,
                ),
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            onInspect = onInspect,
            messages = messages,
            modifier = Modifier.weight(weight = 1f),
        )
        AvatarsRow(
            activePlayer = state.playerMap.getValue(key = activeUserId),
            targetPlayer = state.targetPlayerFor(targetUserId = currentTargetUserId),
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

@Composable
private fun TopHalfContent(
    headIconPlayers: List<PlayerState.Selected>,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    messages: List<ArenaMessageState>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ClassIcons(
            headIconPlayers = headIconPlayers,
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            onInspect = onInspect,
            modifier = Modifier.padding(top = MESSAGES_ICON_SIZE),
        )
        Messages(messages = messages)
    }
}

@Composable
private fun ClassIcons(
    headIconPlayers: List<PlayerState.Selected>,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        headIconPlayers.forEach { player ->
            ClassIcon(
                player = player,
                isTargetable = isTargetable,
                onSelectTarget = onSelectTarget,
                onInspect = onInspect,
            )
        }
    }
}

@Composable
private fun ClassIcon(
    player: PlayerState.Selected,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConscious = player.characterClass.isConscious
    val isTargetingTap = isTargetable && isConscious

    ClassHeadIcon(
        characterClass = player.characterClass,
        isEnabled = isConscious,
        modifier =
            modifier.sharedBounds(key = player.userId).clickable {
                if (isTargetingTap) onSelectTarget(player.userId) else onInspect(player.userId)
            },
    )
}

/**
 * The top-left messages affordance: a tapping target that expands the message log downward over the
 * head icons (stopping at the bottom half), with the unread count drawn in the center of the icon.
 */
@Composable
private fun Messages(messages: List<ArenaMessageState>, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(value = false) }

    var lastSeenMessageCount by remember { mutableStateOf(value = messages.size) }
    LaunchedEffect(key1 = messages.size, key2 = isExpanded) {
        val hasLogShrunk = messages.size < lastSeenMessageCount
        if (isExpanded || hasLogShrunk) lastSeenMessageCount = messages.size
    }
    val unreadMessageCount = messages.size - lastSeenMessageCount

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            ArenaMessageLog(
                messages = messages,
                modifier = Modifier.fillMaxSize().background(color = PLACEHOLDER_PANEL_GOLD),
            )
        }

        MessagesIcon(
            unreadMessageCount = unreadMessageCount,
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.align(alignment = Alignment.TopStart),
        )
    }
}

@Composable
private fun MessagesIcon(
    unreadMessageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size = MESSAGES_ICON_SIZE).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource = Res.drawable.ic_messages),
            contentDescription = "Messages",
            modifier = Modifier.fillMaxSize(),
        )

        if (unreadMessageCount > 0) {
            UnreadCount(count = unreadMessageCount)
        }
    }
}

/**
 * The unread count, drawn so the four class colors continuously flow through its digits — a looping
 * horizontal gradient whose tiled colors are shifted across the text forever.
 */
@Composable
private fun UnreadCount(count: Int, modifier: Modifier = Modifier) {
    val colorTransition = rememberInfiniteTransition(label = UNREAD_COUNT_COLOR_TRANSITION_LABEL)
    val shift by
        colorTransition.animateFloat(
            initialValue = 0f,
            targetValue = UNREAD_COUNT_COLOR_TILE_WIDTH_PX,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = UNREAD_COUNT_COLOR_CYCLE_MILLIS,
                            easing = LinearEasing,
                        )
                ),
            label = UNREAD_COUNT_COLOR_SHIFT_LABEL,
        )

    val flowingColors = remember { classFamilyColors().let { it + it.first() } }
    val flowingBrush =
        Brush.linearGradient(
            colors = flowingColors,
            start = Offset(x = shift, y = 0f),
            end = Offset(x = shift + UNREAD_COUNT_COLOR_TILE_WIDTH_PX, y = 0f),
            tileMode = TileMode.Repeated,
        )

    Text(
        text = count.toString(),
        style =
            TextStyle(
                brush = flowingBrush,
                fontSize = UNREAD_COUNT_FONT_SIZE,
                fontWeight = FontWeight.Bold,
            ),
        modifier = modifier,
    )
}

@Composable
private fun AvatarsRow(
    activePlayer: PlayerState.Selected,
    targetPlayer: PlayerState.Selected?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ClassAvatar(
            characterClass = activePlayer.characterClass,
            modifier = Modifier.weight(weight = 1f).sharedBounds(key = activePlayer.userId),
        )

        if (targetPlayer != null) {
            ClassAvatar(
                characterClass = targetPlayer.characterClass,
                modifier = Modifier.weight(weight = 1f).sharedBounds(key = targetPlayer.userId),
            )
        }
    }
}

/**
 * The button bar flanked by the two stat columns. The buttons sit centered underneath while the
 * stat columns are pinned to the edges on top, so an expanded stat row opens over the buttons
 * rather than pushing them (SS-146).
 */
@Composable
private fun BottomControls(
    state: ArenaState.Combat,
    targetedOpponentUserId: String,
    lockTarget: () -> Unit,
    diceRoll: DiceRoll,
    modifier: Modifier = Modifier,
) {
    val targetPlayer = state.targetPlayerFor(targetUserId = targetedOpponentUserId)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Buttons(
            state = state,
            targetedOpponentUserId = targetedOpponentUserId,
            lockTarget = lockTarget,
            diceRoll = diceRoll,
        )

        ClassAvatarStats(
            characterClass = state.turnOwnerPlayer.characterClass,
            displayDirection = DisplayDirection.Left,
            modifier = Modifier.align(alignment = Alignment.CenterStart),
        )

        ClassAvatarStats(
            characterClass = targetPlayer?.characterClass,
            displayDirection = DisplayDirection.Right,
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
        )
    }
}

/**
 * One player's five stat rows stacked beside the buttons: Speed, Armor, Hit Points, and the
 * offensive/defensive ability markers. Each row shows an icon (plus a value for the numeric stats)
 * on the class color; tapping expands it toward the centre — rightward on the
 * [DisplayDirection.Left] side, leftward on the [DisplayDirection.Right] — to reveal a short
 * explanation (SS-146). Null while there is no target.
 */
@Composable
private fun ClassAvatarStats(
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

    Row(
        modifier =
            modifier
                .clip(shape = STAT_ROW_SHAPE)
                .background(color = classColor)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLeft) {
            StatIconAndValue(stat = stat, isLeft = true)
            StatExplanation(stat = stat, isVisible = isExpanded, isLeft = true)
        } else {
            StatExplanation(stat = stat, isVisible = isExpanded, isLeft = false)
            StatIconAndValue(stat = stat, isLeft = false)
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
private fun StatExplanation(stat: AvatarStat, isVisible: Boolean, isLeft: Boolean) {
    val edge = if (isLeft) Alignment.Start else Alignment.End

    AnimatedVisibility(
        visible = isVisible,
        enter = expandHorizontally(expandFrom = edge) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = edge) + fadeOut(),
    ) {
        Column(
            modifier = Modifier.widthIn(max = STAT_EXPLANATION_MAX_WIDTH).padding(horizontal = 4.dp)
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

private data class AvatarStat(
    val icon: DrawableResource,
    val value: String?,
    val name: String,
    val explanation: String,
)

private fun avatarStats(
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

@Composable
private fun ClassAvatar(characterClass: CharacterClassState, modifier: Modifier = Modifier) {
    // TODO(SS-068): combat animations will drive this static art.
    Image(
        painter = painterResource(resource = characterClass.type.toAvatarResource()),
        contentDescription = characterClass.className,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/**
 * The empty resting slot below the avatars that reserves space for the dice and reports its bounds.
 * The dice themselves are drawn in the screen-spanning [DiceOverlay] so they can roll over all the
 * content; at rest they sit centered in this slot (SS-149).
 */
@Composable
private fun DiceHomeSlot(onBoundsChanged: (Rect) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.fillMaxWidth().height(height = DICE_HOME_HEIGHT).onGloballyPositioned {
                onBoundsChanged(it.boundsInRoot())
            }
    )
}

/**
 * The dice, drawn in a screen-spanning overlay above all other content so a roll tumbles over
 * everything (SS-149). Each die's tray is the whole overlay, so it bounces across the screen while
 * rolling, then glides back to its resting spot in the [homeBounds] slot. Always shows both dice
 * for a steady footprint; on a single-die roll the second die is dimmed and still. The overlay is
 * non-interactive, so taps fall through to the content (the buttons) beneath it.
 */
@Composable
private fun DiceOverlay(diceRoll: DiceRoll, homeBounds: Rect?, modifier: Modifier = Modifier) {
    if (homeBounds == null) return

    var overlayOrigin by remember { mutableStateOf(value = Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier.onGloballyPositioned { overlayOrigin = it.localToRoot(Offset.Zero) }
    ) {
        val spacingPx = with(LocalDensity.current) { DICE_REST_SPACING.toPx() }
        val homeCenter = homeBounds.center - overlayOrigin

        repeat(times = DICE_HOME_DIE_COUNT) { index ->
            val isActive = index < diceRoll.displayedDiceCount
            val restCenter =
                Offset(
                    x = homeCenter.x + (index - (DICE_HOME_DIE_COUNT - 1) / 2f) * spacingPx,
                    y = homeCenter.y,
                )
            Die3d(
                value = diceRoll.dieFaces.getOrNull(index = index) ?: DIE_RESTING_FACE,
                isRolling = isActive && diceRoll.isRolling,
                enabled = isActive,
                shakeTick = diceRoll.shakeTick,
                trayWidth = maxWidth,
                trayHeight = maxHeight,
                restCenter = restCenter,
            )
        }
    }
}

/**
 * The full-screen call-out for a flash-worthy event (SS-148): a big "Hit!"/"Miss!"/"K.O.!" that
 * springs in with a bounce, holds, then pops out, over a one-shot confetti burst in the four class
 * colors. Driven by the latest [flashMessage] with [flashText]; re-fires whenever a newer one
 * lands.
 */
@Composable
private fun FlashOverlay(flashMessage: ArenaMessageState?, modifier: Modifier = Modifier) {
    var flash by remember { mutableStateOf<FlashState?>(value = null) }
    val visibleState = remember { MutableTransitionState(initialState = false) }

    LaunchedEffect(key1 = flashMessage?.id) {
        val text = flashMessage?.flashText ?: return@LaunchedEffect
        flash = FlashState(key = flashMessage.id, text = text)
        visibleState.targetState = true
        delay(timeMillis = FLASH_HOLD_MILLIS)
        visibleState.targetState = false
    }

    val current = flash

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (current == null) return@Box

        if (visibleState.targetState || visibleState.currentState) {
            ConfettiBurst(triggerKey = current.key, modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter =
                scaleIn(
                    initialScale = 0.3f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
        ) {
            FlashText(text = current.text)
        }
    }
}

@Composable
private fun FlashText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style =
            TextStyle(
                fontSize = FLASH_FONT_SIZE,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                shadow =
                    Shadow(color = Color.Black, offset = Offset(x = 0f, y = 4f), blurRadius = 8f),
            ),
        modifier = modifier,
    )
}

/** A one-shot burst: each particle flies out from the centre and fades, in a class-family color. */
@Composable
private fun ConfettiBurst(triggerKey: Any, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(initialValue = 0f) }
    val particles = remember(triggerKey) { confettiParticles() }

    LaunchedEffect(key1 = triggerKey) {
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = CONFETTI_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        val fade = (1f - t).coerceIn(minimumValue = 0f, maximumValue = 1f)
        particles.forEach { particle ->
            val travelPx = particle.distance.dp.toPx() * t
            val position =
                Offset(
                    x = center.x + cos(x = particle.angle) * travelPx,
                    y = center.y + sin(x = particle.angle) * travelPx,
                )
            drawCircle(
                color = particle.color.copy(alpha = fade),
                radius = particle.radius.dp.toPx() * (1f - 0.5f * t),
                center = position,
            )
        }
    }
}

private class FlashState(val key: String, val text: String)

private class ConfettiParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color,
)

private fun confettiParticles(): List<ConfettiParticle> {
    val colors = classFamilyColors()
    return List(size = CONFETTI_COUNT) { index ->
        ConfettiParticle(
            angle = Random.nextFloat() * 2f * PI_FLOAT,
            distance =
                CONFETTI_MIN_DISTANCE +
                    Random.nextFloat() * (CONFETTI_MAX_DISTANCE - CONFETTI_MIN_DISTANCE),
            radius =
                CONFETTI_MIN_RADIUS +
                    Random.nextFloat() * (CONFETTI_MAX_RADIUS - CONFETTI_MIN_RADIUS),
            color = colors[index % colors.size],
        )
    }
}

/**
 * A right-side drawer that slides in to inspect a class (SS-150): the full avatar plus every stat
 * with its explanation, opened by a non-targeting tap on a head icon. A scrim dims the combat
 * behind it; tapping the scrim (or back into a normal targeting tap next turn) dismisses it.
 */
@Composable
private fun InspectDrawer(
    player: PlayerState.Selected?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = player != null
    var shownPlayer by remember { mutableStateOf<PlayerState.Selected?>(value = null) }
    if (player != null) shownPlayer = player

    Box(modifier = modifier) {
        AnimatedVisibility(visible = isOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(color = INSPECT_SCRIM).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onClose()
                    }
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
        ) {
            val current = shownPlayer
            if (current != null) InspectContent(player = current)
        }
    }
}

@Composable
private fun InspectContent(player: PlayerState.Selected, modifier: Modifier = Modifier) {
    val classCopy = LocalCharacterClassCopy.current
    val stats = remember(player, classCopy) { avatarStats(player.characterClass, classCopy) }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(width = INSPECT_DRAWER_WIDTH)
                .background(color = INSPECT_DRAWER_COLOR)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .verticalScroll(state = rememberScrollState())
                .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = player.characterClass.className,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        ClassAvatar(
            characterClass = player.characterClass,
            modifier = Modifier.fillMaxWidth().height(height = INSPECT_AVATAR_HEIGHT),
        )
        stats.forEach { stat -> InspectStatRow(stat = stat) }
    }
}

@Composable
private fun InspectStatRow(stat: AvatarStat, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = stat.icon),
            contentDescription = stat.name,
            modifier = Modifier.size(size = STAT_ICON_SIZE),
        )
        Column {
            val title = if (stat.value != null) "${stat.name} · ${stat.value}" else stat.name
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
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
 * The action bar: the normal [AttackButton] on top, then a row holding the ability button(s) and
 * the Illusionist's [ImpersonateButton]. The controls available right now are derived from the
 * combat state (SS-101); a multi-choice ability (Mirror Image) yields several ability buttons in
 * the row. Each button rolls on hold and fires its action on release.
 */
@Composable
private fun Buttons(
    state: ArenaState.Combat,
    targetedOpponentUserId: String,
    lockTarget: () -> Unit,
    diceRoll: DiceRoll,
    modifier: Modifier = Modifier,
) {
    val onAttack = ArenaActionsProvider.current.onAttack
    val onUseAbility = ArenaActionsProvider.current.onUseAbility

    val controls = state.combatControls
    val attackControl = controls.firstOrNull { it is ArenaCombatControl.NormalAttack }
    val impersonateControl = controls.firstOrNull { it.isImpersonate() }
    val abilityControls = controls.filter { it != attackControl && it != impersonateControl }
    val shouldShowImpersonateButton = impersonateControl != null

    val buttonCount = abilityControls.size + if (shouldShowImpersonateButton) 1 else 0
    val shouldLabelAbilities = abilityControls.size > 1

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (attackControl != null) {
            AttackButton(
                isEnabled = state.isControlEnabled(attackControl, targetedOpponentUserId),
                diceRoll = diceRoll,
                diceCount = attackControl.diceCount,
                onRoll =
                    state.rollActionFor(
                        control = attackControl,
                        targetedOpponentUserId = targetedOpponentUserId,
                        onAttack = onAttack,
                        onUseAbility = onUseAbility,
                        lockTarget = lockTarget,
                    ),
            )
        }

        if (buttonCount > 0) {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally)
            ) {
                abilityControls.forEach { control ->
                    AbilityButton(
                        control = control,
                        isEnabled = state.isControlEnabled(control, targetedOpponentUserId),
                        diceRoll = diceRoll,
                        onRoll =
                            state.rollActionFor(
                                control = control,
                                targetedOpponentUserId = targetedOpponentUserId,
                                onAttack = onAttack,
                                onUseAbility = onUseAbility,
                                lockTarget = lockTarget,
                            ),
                        label = control.label.takeIf { shouldLabelAbilities },
                    )
                }

                ImpersonateButton(
                    shouldShowImpersonateButton = shouldShowImpersonateButton,
                    isEnabled =
                        impersonateControl != null &&
                            state.isControlEnabled(impersonateControl, targetedOpponentUserId),
                    label = state.impersonateLabel(),
                    diceRoll = diceRoll,
                    diceCount = impersonateControl?.diceCount,
                    onRoll =
                        impersonateControl?.let {
                            state.rollActionFor(
                                control = it,
                                targetedOpponentUserId = targetedOpponentUserId,
                                onAttack = onAttack,
                                onUseAbility = onUseAbility,
                                lockTarget = lockTarget,
                            )
                        } ?: {},
                )
            }
        }
    }
}

@Composable
private fun AttackButton(
    isEnabled: Boolean,
    diceRoll: DiceRoll,
    diceCount: Int?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArenaButton(
        icon = Res.drawable.ic_attack_button,
        contentDescription = "Attack",
        isEnabled = isEnabled,
        diceRoll = diceRoll,
        diceCount = diceCount,
        onRoll = onRoll,
        modifier = modifier,
    )
}

@Composable
private fun AbilityButton(
    control: ArenaCombatControl,
    isEnabled: Boolean,
    diceRoll: DiceRoll,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    ArenaButton(
        icon = control.toButtonIcon(),
        contentDescription = control.label,
        isEnabled = isEnabled,
        diceRoll = diceRoll,
        diceCount = control.diceCount,
        onRoll = onRoll,
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun ImpersonateButton(
    shouldShowImpersonateButton: Boolean,
    isEnabled: Boolean,
    label: String,
    diceRoll: DiceRoll,
    diceCount: Int?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = shouldShowImpersonateButton, modifier = modifier) {
        ArenaButton(
            icon = Res.drawable.ic_offensive_ability,
            contentDescription = "Impersonate",
            isEnabled = isEnabled,
            diceRoll = diceRoll,
            diceCount = diceCount,
            onRoll = onRoll,
            label = label,
        )
    }
}

/**
 * The shared button style: an icon held to roll, released to fire [onRoll]. When the control rolls
 * dice ([diceCount] non-null) the hold drives the shared [diceRoll] — shake or double-tap to
 * commit, like the dice tray; a choice control (no dice, e.g. Impersonate) commits immediately on
 * release.
 */
@Composable
private fun ArenaButton(
    icon: DrawableResource,
    contentDescription: String,
    isEnabled: Boolean,
    diceRoll: DiceRoll,
    diceCount: Int?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val rollsDice = diceCount != null

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Image(
            painter = painterResource(resource = icon),
            contentDescription = contentDescription,
            modifier =
                Modifier.size(size = ARENA_BUTTON_SIZE)
                    .alpha(alpha = if (isEnabled) 1f else DISABLED_BUTTON_ALPHA)
                    .holdToRollGesture(
                        enabled = isEnabled,
                        onHoldStart = { if (rollsDice) diceRoll.startHold(diceCount = diceCount) },
                        onRelease = { isDoubleTap ->
                            if (rollsDice) {
                                diceRoll.release(isDoubleTap = isDoubleTap) { onRoll() }
                            } else {
                                onRoll()
                            }
                        },
                    ),
        )

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Whether this control is the Illusionist's Impersonate (its own button, not a generic ability).
 */
private fun ArenaCombatControl.isImpersonate(): Boolean =
    this is ArenaCombatControl.ActivatedAbility &&
        action is AbilityActionState.Activated.Impersonate

/** The context icon for a control: attack, activated-offensive, teleport roll-off, or defensive. */
private fun ArenaCombatControl.toButtonIcon(): DrawableResource =
    when (this) {
        is ArenaCombatControl.NormalAttack -> Res.drawable.ic_attack_button

        is ArenaCombatControl.ActivatedAbility -> Res.drawable.ic_offensive_ability

        is ArenaCombatControl.AbilityRoll ->
            if (action is AbilityActionState.Passive.Teleport) {
                Res.drawable.ic_teleport_button
            } else {
                Res.drawable.ic_defensive_ability
            }
    }

/**
 * A control is rollable only when enabled, its target is chosen, and it is not a no-op Impersonate.
 */
private fun ArenaState.Combat.isControlEnabled(
    control: ArenaCombatControl,
    targetedOpponentUserId: String,
): Boolean {
    val hasRequiredTarget = !control.requiresTarget || targetedOpponentUserId.isNotEmpty()
    val isRedundantImpersonate =
        isImpersonateRedundant(control = control, targetedOpponentUserId = targetedOpponentUserId)
    return control.enabled && hasRequiredTarget && !isRedundantImpersonate
}

private fun ArenaState.Combat.impersonateLabel(): String {
    val ability = userPlayer.characterClass.offensiveAbility
    val isImpersonating =
        ability is OffensiveAbilityState.Impersonate &&
            ability.impersonatedAbility !is OffensiveAbilityState.None
    // TODO(SS-151): show the copied class name ("Impersonating: $className") via SS-029 string
    //  resources once the copied ability resolves to a class name.
    return if (isImpersonating) "Impersonating" else "Impersonate"
}

private enum class DisplayDirection {
    Left,
    Right,
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatMultiplayerPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                        previewCombatPlayer(
                            userId = "player3",
                            displayName = "Card Sharp",
                            type = CharacterClassType.Gambler,
                        ),
                        previewCombatPlayer(
                            userId = "player4",
                            displayName = "Holy Knight",
                            type = CharacterClassType.Paladin,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatTwoPlayerPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatKnockedOutOpponentPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                        previewCombatPlayer(
                            userId = "player3",
                            displayName = "Card Sharp",
                            type = CharacterClassType.Gambler,
                            hitPoints = 0,
                        ),
                        previewCombatPlayer(
                            userId = "player4",
                            displayName = "Holy Knight",
                            type = CharacterClassType.Paladin,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatUserTurnPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Trickster",
                            type = CharacterClassType.Illusionist,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    ),
                combatActionState = CombatActionState.Idle(activeUserIds = setOf("player1")),
            )
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatFlashPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    )
            ),
        flashMessage =
            ArenaMessageState.NormalRoll(
                id = "ko",
                attackerName = "Mage",
                defenderName = "Rogue",
                firstDie = 6,
                secondDie = 6,
                type = NormalRollMessageType.Knockout,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun InspectDrawerPreview() {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            InspectContent(
                player =
                    PlayerState.Selected(
                        userId = "player1",
                        displayName = "Sorcerer Supreme",
                        characterClass = previewCharacterClassState(type = CharacterClassType.Mage),
                    )
            )
        }
    }
}

@Composable
private fun PreviewArenaCombat(
    state: ArenaState.Combat,
    messages: List<ArenaMessageState> = previewMessages(),
    flashMessage: ArenaMessageState? = null,
    secondsRemaining: Int = 8,
) {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            NewArenaCombat(
                state = state,
                messages = messages,
                flashMessage = flashMessage,
                secondsRemaining = secondsRemaining,
                isTimerVisible = secondsRemaining in 1..15,
                isTimerUrgent = secondsRemaining in 1..10,
            )
        }
    }
}

private fun previewCombatPlayer(
    userId: String,
    displayName: String,
    type: CharacterClassType,
    hitPoints: Int? = null,
): PlayerState.Selected {
    val classState = previewCharacterClassState(type = type)
    return PlayerState.Selected(
        userId = userId,
        displayName = displayName,
        characterClass =
            if (hitPoints == null) classState else classState.copy(hitPoints = hitPoints),
    )
}

/**
 * Builds a combat state for previews. Defaults to the user (first player) mid-attack against the
 * second player, so the rest become head icons; pass [combatActionState] for other contexts (e.g.
 * an idle turn that surfaces the ability buttons).
 */
private fun previewCombatState(
    players: List<PlayerState.Selected>,
    combatActionState: CombatActionState? = null,
): ArenaState.Combat {
    val userId = players.first().userId
    return ArenaState.Combat(
        playerMap = players.associateBy { it.userId },
        userPlayerId = userId,
        combatActionState =
            combatActionState
                ?: CombatActionState.NormalAttack(
                    roll = NormalAttackType.ArmorClass,
                    attackerUserId = userId,
                    defenderUserId = players[1].userId,
                    originalTurnUserId = userId,
                    activeUserIds = setOf(userId),
                ),
        roundNumber = 1,
        isNewRound = true,
        doublesBonus = null,
    )
}

private fun previewMessages(): List<ArenaMessageState> =
    listOf(
        ArenaMessageState.ConnectedToArena(id = "1", displayName = "The battle begins!"),
        ArenaMessageState.NewRound(id = "2", roundNumber = "1"),
        ArenaMessageState.NewTurn(
            id = "3",
            characterClassInfo =
                CharacterClassMessageInfoState(className = "Mage", type = CharacterClassType.Mage),
        ),
    )
