package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState

/**
 * Builds the action a control fires when rolled: a normal attack, or using an ability. Every action
 * reports itself through [onActionDispatched] before dispatching, so the controls can lock while
 * the shared state's network round trip is in flight — a second tap in that window would otherwise
 * re-dispatch against a step that has already resolved (SS-181).
 */
internal fun ArenaState.Combat.rollActionFor(
    control: ArenaCombatControl,
    targetedOpponentUserId: String,
    onAttack: (AttackArguments) -> Unit,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
    onActionDispatched: () -> Unit,
): () -> Unit =
    when (control) {
        is ArenaCombatControl.NormalAttack -> {
            {
                lockTarget()
                onActionDispatched()
                onAttack(
                    AttackArguments(
                        targetedOpponentUserId = targetedOpponentUserId,
                        userPlayerId = userPlayerId,
                    )
                )
            }
        }

        is ArenaCombatControl.ActivatedAbility ->
            abilityAction(
                action = control.action,
                requiresTarget = control.requiresTarget,
                targetedOpponentUserId = targetedOpponentUserId,
                onUseAbility = onUseAbility,
                lockTarget = lockTarget,
                onActionDispatched = onActionDispatched,
            )

        is ArenaCombatControl.AbilityRoll ->
            abilityAction(
                action = control.action,
                requiresTarget = control.requiresTarget,
                targetedOpponentUserId = targetedOpponentUserId,
                onUseAbility = onUseAbility,
                lockTarget = lockTarget,
                onActionDispatched = onActionDispatched,
            )
    }

private fun ArenaState.Combat.abilityAction(
    action: AbilityActionState,
    requiresTarget: Boolean,
    targetedOpponentUserId: String,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
    onActionDispatched: () -> Unit,
): () -> Unit = {
    val isImpersonateCopyPick = action is AbilityActionState.Activated.Impersonate
    val shouldLockTarget = requiresTarget && !isImpersonateCopyPick
    if (shouldLockTarget) lockTarget()
    onActionDispatched()
    onUseAbility(
        UseAbilityArguments(
            abilityActionState = action,
            targetedOpponentUserId = targetedOpponentUserId,
            userPlayerId = userPlayerId,
        )
    )
}
