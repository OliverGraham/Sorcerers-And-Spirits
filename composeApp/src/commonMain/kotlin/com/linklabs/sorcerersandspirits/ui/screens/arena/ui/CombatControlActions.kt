package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState

/** Builds the action a control fires when rolled: a normal attack, or using an ability. */
internal fun ArenaState.Combat.rollActionFor(
    control: ArenaCombatControl,
    targetedOpponentUserId: String,
    onAttack: (AttackArguments) -> Unit,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
): () -> Unit =
    when (control) {
        is ArenaCombatControl.NormalAttack -> {
            {
                lockTarget()
                onAttack(
                    AttackArguments(
                        combatActionState = combatActionState,
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
            )

        is ArenaCombatControl.AbilityRoll ->
            abilityAction(
                action = control.action,
                requiresTarget = control.requiresTarget,
                targetedOpponentUserId = targetedOpponentUserId,
                onUseAbility = onUseAbility,
                lockTarget = lockTarget,
            )
    }

private fun ArenaState.Combat.abilityAction(
    action: AbilityActionState,
    requiresTarget: Boolean,
    targetedOpponentUserId: String,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
): () -> Unit = {
    if (requiresTarget) lockTarget()
    onUseAbility(
        UseAbilityArguments(
            abilityActionState = action,
            targetedOpponentUserId = targetedOpponentUserId,
            userPlayerId = userPlayerId,
        )
    )
}
