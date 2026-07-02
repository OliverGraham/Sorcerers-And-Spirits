package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.diceSlots
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isImpersonateRedundant
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRoll
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
private val arenaButtonShape = CircleShape
private val arenaButtonFaceColor = Color(color = 0xFF323232)
private val arenaButtonNeutralBorder = Color(color = 0xFF555555)
private const val ARENA_BUTTON_PRESS_LABEL = "ArenaButtonPress"

/**
 * The single action row across the bottom of the screen: every control the user can take right now,
 * evenly spaced with the normal attack between the ability buttons (left) and the Illusionist's
 * Impersonate button (right). [Arrangement.SpaceEvenly] keeps the attack dead-centered, however
 * many abilities there are. Stats moved out of this row into the inspect drawer, so the buttons
 * span the full width on their own (SS-157).
 *
 * The row always reserves its full height and fades the buttons in and out with
 * [AnimatedVisibility] rather than appearing and disappearing, so the avatars above do not reflow
 * as turns pass (there are no controls on an opponent's turn) or while a finish is pending
 * ([areControlsVisible]).
 */
@Composable
internal fun BottomControls(
    state: ArenaState.Combat,
    targetedOpponentUserId: String,
    lockTarget: () -> Unit,
    diceRoll: DiceRoll,
    areControlsVisible: Boolean,
    isActionInFlight: Boolean,
    onActionDispatched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onAttack = ArenaActionsProvider.current.onAttack
    val onUseAbility = ArenaActionsProvider.current.onUseAbility
    val classColor = state.userPlayer.characterClass.type.toColor()

    val controls = state.combatControls
    val attackControl = controls.firstOrNull { it is ArenaCombatControl.NormalAttack }
    val impersonateControl = controls.firstOrNull { it.isImpersonate() }
    val abilityControls = controls.filter { it != attackControl && it != impersonateControl }
    val shouldLabelAbilities = abilityControls.size > 1
    val areButtonsShown = areControlsVisible && controls.isNotEmpty()

    fun isEnabled(control: ArenaCombatControl): Boolean =
        !isActionInFlight &&
            state.isControlEnabled(
                control = control,
                targetedOpponentUserId = targetedOpponentUserId,
            )

    fun rollAction(control: ArenaCombatControl): () -> Unit =
        state.rollActionFor(
            control = control,
            targetedOpponentUserId = targetedOpponentUserId,
            onAttack = onAttack,
            onUseAbility = onUseAbility,
            lockTarget = lockTarget,
            onActionDispatched = onActionDispatched,
        )

    Box(
        modifier =
            modifier.fillMaxWidth().height(height = arenaButtonSize + arenaButtonLift + 24.dp)
    ) {
        AnimatedVisibility(visible = areButtonsShown, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                abilityControls.forEach { control ->
                    AbilityButton(
                        control = control,
                        isEnabled = isEnabled(control = control),
                        classColor = classColor,
                        diceRoll = diceRoll,
                        onRoll = rollAction(control = control),
                        label = control.label.takeIf { shouldLabelAbilities },
                    )
                }

                if (attackControl != null) {
                    AttackButton(
                        isEnabled = isEnabled(control = attackControl),
                        classColor = classColor,
                        diceRoll = diceRoll,
                        diceSlots = attackControl.diceSlots,
                        onRoll = rollAction(control = attackControl),
                    )
                }

                if (impersonateControl != null) {
                    ImpersonateButton(
                        isEnabled = isEnabled(control = impersonateControl),
                        classColor = classColor,
                        label = state.impersonateLabel(),
                        diceRoll = diceRoll,
                        diceSlots = impersonateControl.diceSlots,
                        onRoll = rollAction(control = impersonateControl),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttackButton(
    isEnabled: Boolean,
    classColor: Color,
    diceRoll: DiceRoll,
    diceSlots: Set<Int>?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArenaButton(
        icon = Res.drawable.ic_attack_button,
        contentDescription = "Attack",
        isEnabled = isEnabled,
        classColor = classColor,
        showClassBorder = false,
        diceRoll = diceRoll,
        diceSlots = diceSlots,
        onRoll = onRoll,
        modifier = modifier,
    )
}

@Composable
private fun AbilityButton(
    control: ArenaCombatControl,
    isEnabled: Boolean,
    classColor: Color,
    diceRoll: DiceRoll,
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
        diceRoll = diceRoll,
        diceSlots = control.diceSlots,
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
    diceRoll: DiceRoll,
    diceSlots: Set<Int>?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArenaButton(
        icon = Res.drawable.ic_offensive_ability,
        contentDescription = "Impersonate",
        isEnabled = isEnabled,
        classColor = classColor,
        showClassBorder = true,
        diceRoll = diceRoll,
        diceSlots = diceSlots,
        onRoll = onRoll,
        label = label,
        modifier = modifier,
    )
}

/**
 * The shared button style: a raised icon face over a darker base, held to roll and released to fire
 * [onRoll]. The face sits lifted above the base and drops onto it while pressed, so the button
 * reads as physically pushed in (SS-160); ability buttons carry the player's [classColor] as a
 * border, while the base depth is a darkened [classColor] for every button. When the control rolls
 * dice ([diceCount] non-null) the hold drives the shared [diceRoll] — shake or double-tap to
 * commit, like the dice tray; a choice control (no dice, e.g. Impersonate) commits immediately on
 * release.
 */
@Composable
private fun ArenaButton(
    icon: DrawableResource,
    contentDescription: String,
    isEnabled: Boolean,
    classColor: Color,
    showClassBorder: Boolean,
    diceRoll: DiceRoll,
    diceSlots: Set<Int>?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val rollsDice = diceSlots != null
    var isPressed by remember { mutableStateOf(value = false) }
    val pressDrop by
        animateDpAsState(
            targetValue = if (isPressed) arenaButtonLift else 0.dp,
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
                                isPressed = true
                                if (rollsDice) diceRoll.startHold(slots = diceSlots)
                            },
                            onRelease = { isDoubleTap ->
                                isPressed = false
                                if (rollsDice) {
                                    diceRoll.release(isDoubleTap = isDoubleTap) { onRoll() }
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
