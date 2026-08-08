package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState

internal fun ArenaState.Combat.rollActionFor(
    control: ArenaCombatControl,
    targetedOpponentUserId: String?,
    onAttack: (AttackArguments) -> Unit,
    onUseAbility: (UseAbilityArguments) -> Unit,
): () -> Unit {
    // TODO: Refactor. Likely remove ArenaCombatControl
    val targetedOpponentUserId = targetedOpponentUserId ?: return {}

    return when (control) {
        is ArenaCombatControl.NormalAttack -> {
            {
                onAttack(
                    AttackArguments(
                        targetedOpponentUserId = targetedOpponentUserId,
                        userPlayerId = userPlayerId,
                        combatActionState = combatActionState,
                    )
                )
            }
        }

        is ArenaCombatControl.ActivatedAbility ->
            abilityAction(
                action = control.action,
                targetedOpponentUserId = targetedOpponentUserId,
                onUseAbility = onUseAbility,
            )

        is ArenaCombatControl.AbilityRoll ->
            abilityAction(
                action = control.action,
                targetedOpponentUserId = targetedOpponentUserId,
                onUseAbility = onUseAbility,
            )
    }
}

private fun ArenaState.Combat.abilityAction(
    action: AbilityActionState,
    targetedOpponentUserId: String,
    onUseAbility: (UseAbilityArguments) -> Unit,
): () -> Unit = {
    onUseAbility(
        UseAbilityArguments(
            abilityActionState = action,
            targetedOpponentUserId = targetedOpponentUserId,
            userPlayerId = userPlayerId,
            combatActionState = combatActionState,
        )
    )
}
