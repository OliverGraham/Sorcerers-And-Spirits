package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.holdToRollGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.rollActionFor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_attack_button
import sorcerersandspirits.composeapp.generated.resources.ic_defensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_offensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_teleport_button

private val arenaButtonSize = 72.dp
private val arenaButtonLift = 6.dp
internal val BOTTOM_CONTROLS_HEIGHT = arenaButtonSize + arenaButtonLift + 24.dp

private val arenaButtonShape = CircleShape
private val arenaButtonFaceColor = Color(color = 0xFF323232)
private val arenaButtonNeutralBorder = Color(color = 0xFF555555)
private const val ARENA_BUTTON_PRESS_LABEL = "ArenaButtonPress"

// TODO: Move state to view model layer. Goddamn you Claude
@Composable
internal fun CombatButtons(
    state: ArenaState.Combat,
    diceStateHolder: DiceStateHolder,
    areControlsVisible: Boolean,
    isPlayingBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val onAttack = ArenaActionsProvider.current.onAttack
    val onUseAbility = ArenaActionsProvider.current.onUseAbility

    val classColor = state.userPlayer.characterClass.type.toColor()
    val controls = state.combatControls

    val isActionInFlight = state.isActionInFlight
    val isBusy = isPlayingBack || isActionInFlight

    // TODO: What is this Teleport nonsense? Looks like the cause of the bug I've been seeing.
    //       Need to hide the button per user, so a user can't roll twice (for teleport here).
    val hasPendingTeleportRoll = state.isAwaitingTeleportRoll
    var hasReachedTeleportRollBeat by
        remember(key1 = hasPendingTeleportRoll) { mutableStateOf(value = false) }
    val hasReachedRollBeatNow = hasPendingTeleportRoll && !isBusy
    if (hasReachedRollBeatNow) hasReachedTeleportRollBeat = true

    val isSuppressedByAnimation = isBusy && !hasReachedTeleportRollBeat

    // TODO: Very, very rarely are nested functions allowable (these are not)
    fun isEnabled(control: ArenaCombatControl): Boolean =
        !isActionInFlight &&
            state.isControlEnabled(control = control, targetedOpponentUserId = state.targetedUserId)

    fun rollAction(control: ArenaCombatControl): () -> Unit =
        state.rollActionFor(
            control = control,
            targetedOpponentUserId = state.targetedUserId,
            onAttack = onAttack,
            onUseAbility = onUseAbility,
        )

    val areButtonsShown = areControlsVisible && !isSuppressedByAnimation && controls.isNotEmpty()
    Box(modifier = modifier.fillMaxWidth().height(height = BOTTOM_CONTROLS_HEIGHT)) {
        AnimatedVisibility(
            visible = areButtonsShown,
            enter = fadeIn(),
            exit = ExitTransition.None,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                val abilityControls = state.abilityControls
                abilityControls.forEach { control ->
                    AbilityButton(
                        control = control,
                        isEnabled = isEnabled(control = control),
                        classColor = classColor,
                        diceStateHolder = diceStateHolder,
                        onRoll = rollAction(control = control),
                        label = control.label.takeIf { abilityControls.size > 1 },
                    )
                }

                val normalAttackControl = state.arenaCombatControl
                if (normalAttackControl != null) {
                    AttackButton(
                        isEnabled = isEnabled(control = normalAttackControl),
                        classColor = classColor,
                        diceStateHolder = diceStateHolder,
                        onRoll = rollAction(control = normalAttackControl),
                    )
                }

                val impersonateControl = state.impersonateControl
                if (impersonateControl != null) {
                    ImpersonateButton(
                        isEnabled = isEnabled(control = impersonateControl),
                        classColor = classColor,
                        label = state.impersonateLabel(),
                        diceStateHolder = diceStateHolder,
                        onRoll = rollAction(control = impersonateControl),
                    )
                }
            }
        }
    }
}

// TODO: refactor rollingDiePositions
@Composable
private fun AttackButton(
    isEnabled: Boolean,
    classColor: Color,
    diceStateHolder: DiceStateHolder,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArenaButton(
        icon = Res.drawable.ic_attack_button,
        contentDescription = "Attack",
        isEnabled = isEnabled,
        classColor = classColor,
        showClassBorder = false,
        diceStateHolder = diceStateHolder,
        rollingDiePositions = setOf(DiePosition.First, DiePosition.Second),
        onRoll = onRoll,
        modifier = modifier,
    )
}

@Composable
private fun AbilityButton(
    control: ArenaCombatControl,
    isEnabled: Boolean,
    classColor: Color,
    diceStateHolder: DiceStateHolder,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    ArenaButton(
        icon = control.toButtonIcon(),
        contentDescription = control.label,
        isEnabled = isEnabled,
        classColor = classColor,
        showClassBorder = true,
        diceStateHolder = diceStateHolder,
        rollingDiePositions = control.rollingDiePositions,
        onRoll = onRoll,
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun ImpersonateButton(
    isEnabled: Boolean,
    classColor: Color,
    label: String,
    diceStateHolder: DiceStateHolder,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArenaButton(
        icon = Res.drawable.ic_offensive_ability,
        contentDescription = "Impersonate",
        isEnabled = isEnabled,
        classColor = classColor,
        showClassBorder = true,
        diceStateHolder = diceStateHolder,
        rollingDiePositions = emptySet(),
        onRoll = onRoll,
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun ArenaButton(
    icon: DrawableResource,
    contentDescription: String,
    isEnabled: Boolean,
    classColor: Color,
    showClassBorder: Boolean,
    diceStateHolder: DiceStateHolder,
    rollingDiePositions: Set<DiePosition>,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val isPressed = remember { mutableStateOf(value = false) }
    val pressDrop by
        animateDpAsState(
            targetValue = if (isPressed.value) arenaButtonLift else 0.dp,
            label = ARENA_BUTTON_PRESS_LABEL,
        )
    val baseColor = lerp(start = classColor, stop = Color.Black, fraction = 0.5f)
    val borderColor = if (showClassBorder) classColor else arenaButtonNeutralBorder

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Box(
            modifier =
                Modifier.size(width = arenaButtonSize, height = arenaButtonSize + arenaButtonLift)
                    .alpha(alpha = if (isEnabled) 1f else 0.4f)
        ) {
            Box(
                modifier =
                    Modifier.align(alignment = Alignment.BottomCenter)
                        .size(size = arenaButtonSize)
                        .clip(shape = arenaButtonShape)
                        .background(color = baseColor)
            )

            Box(
                modifier =
                    Modifier.align(alignment = Alignment.TopCenter)
                        .offset(y = pressDrop)
                        .size(size = arenaButtonSize)
                        .clip(shape = arenaButtonShape)
                        .background(color = arenaButtonFaceColor)
                        .border(width = 2.dp, color = borderColor, shape = arenaButtonShape)
                        .holdToRollGesture(
                            enabled = isEnabled,
                            onHoldStart = {
                                isPressed.value = true
                                if (rollingDiePositions.isNotEmpty()) {
                                    diceStateHolder.startHold(
                                        rollingPositions = rollingDiePositions
                                    )
                                }
                            },
                            onRelease = { isDoubleTap ->
                                isPressed.value = false
                                if (rollingDiePositions.isNotEmpty()) {
                                    diceStateHolder.release(isDoubleTap = isDoubleTap) { onRoll() }
                                } else {
                                    onRoll()
                                }
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(resource = icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

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

// TODO: Move to view model layer - in ArenaState.Combat itself
private fun ArenaState.Combat.isControlEnabled(
    control: ArenaCombatControl,
    targetedOpponentUserId: String?,
): Boolean {
    val hasRequiredTarget = !control.requiresTarget || !targetedOpponentUserId.isNullOrEmpty()
    val isRedundantImpersonate =
        isImpersonateRedundant(control = control, targetedOpponentUserId = targetedOpponentUserId)

    return control.enabled && hasRequiredTarget && !isRedundantImpersonate
}

// TODO: Can likely be GREATLY simplified (is it the Illusionist's turn? have they impersonated?
//       can they? done.)
internal fun ArenaState.Combat.isImpersonateRedundant(
    control: ArenaCombatControl,
    targetedOpponentUserId: String?,
): Boolean {
    val isImpersonateControl =
        control is ArenaCombatControl.ActivatedAbility &&
            control.action is AbilityActionState.Activated.Impersonate
    if (!isImpersonateControl) return false

    val target = playerMap[targetedOpponentUserId] ?: return false
    val targetOffensiveAbility = target.characterClass.offensiveAbility
    if (targetOffensiveAbility is OffensiveAbilityState.None) return true

    val impersonateAbility = userPlayer.characterClass.offensiveAbility
    val copiedAbility =
        if (impersonateAbility is OffensiveAbilityState.Impersonate) {
            impersonateAbility.impersonatedAbility
        } else {
            OffensiveAbilityState.None
        }
    if (copiedAbility is OffensiveAbilityState.None) return false

    return targetOffensiveAbility::class == copiedAbility::class
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
