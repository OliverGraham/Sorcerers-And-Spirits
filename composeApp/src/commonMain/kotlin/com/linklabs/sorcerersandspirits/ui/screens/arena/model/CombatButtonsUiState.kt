package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState

@Immutable
data class CombatButtonsUiState(
    val abilityButtons: List<CombatButton>,
    val attackButton: CombatButton?,
    val impersonateButton: CombatButton?,
    val userClassType: CharacterClassType,
    val userPlayerId: String,
    val targetedOpponentUserId: String?,
    val combatActionState: CombatActionState,
    val isActionInFlight: Boolean,
    val isAwaitingTeleportRoll: Boolean,
    val teleportRollOffKey: Int?,
) {
    val hasAnyButton: Boolean =
        abilityButtons.isNotEmpty() || attackButton != null || impersonateButton != null
}

@Immutable
data class CombatButton(
    val control: ArenaCombatControl,
    val isEnabled: Boolean,
    val label: String?,
    val roll: CombatButtonRoll,
)

@Immutable
sealed interface CombatButtonRoll {
    @Immutable data object None : CombatButtonRoll

    @Immutable data object Attack : CombatButtonRoll

    @Immutable data class UseAbility(val action: AbilityActionState) : CombatButtonRoll
}

internal fun ArenaMatchState.Combat.deriveCombatButtonsUiState(
    selectedOpponentUserId: String?,
): CombatButtonsUiState {
    val target = resolveTargetUserId(selectedOpponentUserId = selectedOpponentUserId)
    val hasMultipleAbilities = abilityControls.size > 1

    return CombatButtonsUiState(
        abilityButtons =
        abilityControls.map { control ->
            control.toCombatButton(
                state = this,
                target = target,
                label = control.label.takeIf { hasMultipleAbilities },
            )
        },
        attackButton = arenaCombatControl?.toCombatButton(state = this, target = target),
        impersonateButton =
        impersonateControl?.toCombatButton(state = this, target = target, label = impersonateLabel()),
        userClassType = userPlayer.characterClass.type,
        userPlayerId = userPlayerId,
        targetedOpponentUserId = target,
        combatActionState = combatActionState,
        isActionInFlight = isActionInFlight,
        isAwaitingTeleportRoll = isAwaitingTeleportRoll,
        teleportRollOffKey = teleportRollOffKey,
    )
}

private fun ArenaCombatControl.toCombatButton(
    state: ArenaMatchState.Combat,
    target: String?,
    label: String? = null,
): CombatButton = CombatButton(
    control = this,
    isEnabled = !state.isActionInFlight &&
        state.isControlEnabled(control = this, targetedOpponentUserId = target),
    label = label,
    roll = toCombatButtonRoll(targetedOpponentUserId = target),
)

private fun ArenaCombatControl.toCombatButtonRoll(targetedOpponentUserId: String?): CombatButtonRoll {
    val hasUnmetTargetRequirement = requiresTarget && targetedOpponentUserId == null
    if (hasUnmetTargetRequirement) return CombatButtonRoll.None

    return when (this) {
        is ArenaCombatControl.NormalAttack -> CombatButtonRoll.Attack
        is ArenaCombatControl.ActivatedAbility -> CombatButtonRoll.UseAbility(action = action)
        is ArenaCombatControl.AbilityRoll -> CombatButtonRoll.UseAbility(action = action)
    }
}

private fun ArenaMatchState.Combat.isControlEnabled(
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
private fun ArenaMatchState.Combat.isImpersonateRedundant(
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

private fun ArenaMatchState.Combat.impersonateLabel(): String {
    val ability = userPlayer.characterClass.offensiveAbility
    val isImpersonating =
        ability is OffensiveAbilityState.Impersonate &&
            ability.impersonatedAbility !is OffensiveAbilityState.None
    // TODO(SS-151): show the copied class name ("Impersonating: $className") via SS-029 string
    //  resources once the copied ability resolves to a class name.
    return if (isImpersonating) "Impersonating" else "Impersonate"
}
