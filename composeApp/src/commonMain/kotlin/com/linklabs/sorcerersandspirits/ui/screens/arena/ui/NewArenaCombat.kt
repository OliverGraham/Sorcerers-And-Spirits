package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_attack_button
import sorcerersandspirits.composeapp.generated.resources.ic_defensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_messages
import sorcerersandspirits.composeapp.generated.resources.ic_offensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_teleport_button

private const val TARGET_TRANSITION_LABEL = "NewArenaCombatTargetTransition"

private val MESSAGES_ICON_SIZE = 56.dp
private val UNREAD_COUNT_FONT_SIZE = 20.sp

// Placeholder gold for the expanded message panel — eyeballed until the final Figma value lands.
private val PLACEHOLDER_PANEL_GOLD = Color(color = 0xFFB8860B)

private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f

private val ARENA_BUTTON_SIZE = 72.dp
private const val DISABLED_BUTTON_ALPHA = 0.4f

// The dice home always shows both dice so the footprint is steady; a single-die roll dims one.
private const val DICE_HOME_DIE_COUNT = 2

// The reserved height of the resting dice slot, and the gap between the two dice at rest.
private val DICE_HOME_HEIGHT = 96.dp
private val DICE_REST_SPACING = 76.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NewArenaCombat(
    state: ArenaState.Combat,
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
) {
    // TODO(SS-150): Tapping on ClassIcon of ClassIcons when it's not the user's turn or when
    // they're
    //  locked into a target and to just want to see more info about another class (maybe they're
    //  waiting for their opponent to roll and they want to tap around see things). Two options:
    //       1. Expand head to reveal full Avatar, covering all content nearby?
    //       2. Have Drawer on right that opens with Full Avatar and stats?

    // TODO(SS-148): Need to add animations for full-screen messages.
    //          short messages like "Hit!" or "Miss!" should be shown in center of entire screen.
    //          Brief, stylized animation (bounce, rotate, etc.). Full message would be in log.
    //          Could be a property of the ArenaMessage - short text?

    val diceRoll = rememberDiceRoll()

    var selectedOpponentUserId by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = "") }

    // The instant the user commits a roll, lock the target so a stray tap can't retarget before the
    // Domain pins the defender (mirrors the old ArenaCombat optimistic lock).
    var isTargetLockedOptimistically by
        remember(key1 = state.activeUserIds) { mutableStateOf(value = false) }
    val isTargetable =
        state.isItThisUsersTurn && state.lockedTargetUserId == null && !isTargetLockedOptimistically
    val lockTarget = { isTargetLockedOptimistically = true }
    val targetedOpponentUserId =
        state.resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)

    // Where the dice rest, measured from the empty DiceHome slot in the layout. The dice themselves
    // live in a screen-spanning overlay so a roll tumbles over everything, then glides back here.
    var diceHomeBounds by remember { mutableStateOf<Rect?>(value = null) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Only the morphing head icons and avatars transition on a target change; the dice
                // and buttons below stay put rather than crossfading (SS-149, hoisted out of
                // SS-144).
                AnimatedContent(
                    targetState = targetedOpponentUserId,
                    label = TARGET_TRANSITION_LABEL,
                    modifier = Modifier.weight(weight = 1f),
                ) { currentTargetUserId ->
                    SharedTransitionProvider(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    ) {
                        TransitioningContent(
                            state = state,
                            currentTargetUserId = currentTargetUserId,
                            onSelectTarget = { selectedOpponentUserId = it },
                            isTargetable = isTargetable,
                            messages = messages,
                            unreadMessageCount = unreadMessageCount,
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

            DiceOverlay(
                diceRoll = diceRoll,
                homeBounds = diceHomeBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TransitioningContent(
    state: ArenaState.Combat,
    currentTargetUserId: String,
    onSelectTarget: (String) -> Unit,
    isTargetable: Boolean,
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopHalfContent(
            headIconPlayers = state.headIconPlayersFor(targetUserId = currentTargetUserId),
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            messages = messages,
            unreadMessageCount = unreadMessageCount,
            modifier = Modifier.weight(weight = 1f),
        )
        AvatarsRow(
            userPlayer = state.userPlayer,
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
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ClassIcons(
            headIconPlayers = headIconPlayers,
            isTargetable = isTargetable,
            onSelectTarget = onSelectTarget,
            modifier = Modifier.padding(top = MESSAGES_ICON_SIZE),
        )
        Messages(messages = messages, unreadMessageCount = unreadMessageCount)
    }
}

@Composable
private fun ClassIcons(
    headIconPlayers: List<PlayerState.Selected>,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        headIconPlayers.forEach { player ->
            ClassIcon(player = player, isTargetable = isTargetable, onSelectTarget = onSelectTarget)
        }
    }
}

@Composable
private fun ClassIcon(
    player: PlayerState.Selected,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConscious = player.characterClass.isConscious
    val isClickable = isTargetable && isConscious

    ClassHeadIcon(
        characterClass = player.characterClass,
        isEnabled = isConscious,
        modifier =
            modifier.sharedBounds(key = player.userId).clickable(enabled = isClickable) {
                onSelectTarget(player.userId)
            },
    )
}

/**
 * The top-left messages affordance: a tapping target that expands the message log downward over the
 * head icons (stopping at the bottom half), with the unread count drawn in the center of the icon.
 */
@Composable
private fun Messages(
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(value = false) }

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
    userPlayer: PlayerState.Selected,
    targetPlayer: PlayerState.Selected?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ClassAvatar(
            characterClass = userPlayer.characterClass,
            modifier = Modifier.weight(weight = 1f).sharedBounds(key = userPlayer.userId),
        )

        if (targetPlayer != null) {
            ClassAvatar(
                characterClass = targetPlayer.characterClass,
                modifier = Modifier.weight(weight = 1f).sharedBounds(key = targetPlayer.userId),
            )
        }
    }
}

@Composable
private fun BottomControls(
    state: ArenaState.Combat,
    targetedOpponentUserId: String,
    lockTarget: () -> Unit,
    diceRoll: DiceRoll,
    modifier: Modifier = Modifier,
) {
    // TODO(SS-146): Stats need to open on top of all content in this Row
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            // TODO(SS-146): 5 stats - Active Player
            //       Speed
            //       ArmorClass
            //       HitPoints
            //       OffensiveAbility
            //       DefensiveAbility
        }

        Buttons(
            state = state,
            targetedOpponentUserId = targetedOpponentUserId,
            lockTarget = lockTarget,
            diceRoll = diceRoll,
        )

        Column(modifier = Modifier.weight(weight = 1f)) {
            // TODO(SS-146): 5 stats - Target Opponent
            //       Speed
            //       ArmorClass
            //       HitPoints
            //       OffensiveAbility
            //       DefensiveAbility
        }
    }
}

@Composable
private fun ClassAvatarStats(displayDirection: DisplayDirection, modifier: Modifier = Modifier) {
    when (displayDirection) {
        DisplayDirection.Left -> {
            // TODO(SS-146): Row of Icon with stat, both with class color background.
            //       onTap is expand to the right, revealing a short explanation of the stat.
            //       For OffensiveAbility and DefensiveAbility, there's no stat, but the rest is the
            //  same.
        }

        DisplayDirection.Right -> {
            // TODO(SS-146): Opposite directions of above. Row stat and then Icon, and expands to
            //  left.
        }
    }
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

    // The slot's bounds are in root coordinates; subtract the overlay's own root position so the
    // resting centre is expressed in the overlay's local space, where the dice are drawn.
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
            ),
        unreadMessageCount = 3,
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
            ),
        unreadMessageCount = 0,
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
            ),
        unreadMessageCount = 1,
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NewArenaCombatUserTurnPreview() {
    // An Illusionist on their turn against a lone opponent: the opponent is auto-targeted, so the
    // Attack and Impersonate buttons are enabled.
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
            ),
        unreadMessageCount = 0,
    )
}

@Composable
private fun PreviewArenaCombat(
    state: ArenaState.Combat,
    unreadMessageCount: Int,
    messages: List<ArenaMessageState> = previewMessages(),
) {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            NewArenaCombat(
                state = state,
                messages = messages,
                unreadMessageCount = unreadMessageCount,
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
