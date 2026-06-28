package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.diceCount
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isImpersonateRedundant
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.targetPlayerFor
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

private val ARENA_BUTTON_SIZE = 72.dp
private const val DISABLED_BUTTON_ALPHA = 0.4f

/**
 * The button bar flanked by the two stat columns. The buttons sit centered underneath while the
 * stat columns are pinned to the edges on top, so an expanded stat row opens over the buttons
 * rather than pushing them (SS-146).
 */
@Composable
internal fun BottomControls(
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
