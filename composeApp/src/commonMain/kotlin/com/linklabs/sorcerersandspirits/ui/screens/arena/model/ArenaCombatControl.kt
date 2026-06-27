package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.MirrorImage.RerollSelection as MirrorImageReroll
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain

/**
 * A combat action the user can currently take, derived from [ArenaState.Combat] by
 * [deriveCombatControls]. Composables only render these controls; whether an action is currently
 * legal is decided here from state the Domain already exposes.
 */
@Immutable
sealed interface ArenaCombatControl {
    val label: String
    val requiresTarget: Boolean
    val enabled: Boolean

    @Immutable
    data class NormalAttack(override val requiresTarget: Boolean) : ArenaCombatControl {
        override val label: String = "Attack"
        override val enabled: Boolean = true
    }

    @Immutable
    data class ActivatedAbility(
        val action: AbilityActionState.Activated,
        override val label: String,
        override val requiresTarget: Boolean,
        override val enabled: Boolean,
    ) : ArenaCombatControl

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        override val label: String,
        override val requiresTarget: Boolean,
        override val enabled: Boolean = true,
    ) : ArenaCombatControl
}

/**
 * Determines which combat controls the user can act on right now. An empty list means the user
 * cannot act and no action buttons should be shown.
 */
internal fun ArenaState.Combat.deriveCombatControls(): List<ArenaCombatControl> =
    when (val actionState = combatActionState) {
        is CombatActionState.Idle ->
            if (isItThisUsersTurn) {
                buildList {
                    add(element = ArenaCombatControl.NormalAttack(requiresTarget = true))
                    addAll(
                        elements = userPlayer.characterClass.offensiveAbility.toActivatedControls()
                    )
                }
            } else {
                emptyList()
            }

        is CombatActionState.NormalAttack ->
            if (actionState.attackerUserId == userPlayerId) {
                listOf(element = ArenaCombatControl.NormalAttack(requiresTarget = false))
            } else {
                emptyList()
            }

        is CombatActionState.AbilityRoll ->
            if (
                isItThisUsersTurn && actionState.action.isUserTheRoller(userPlayerId = userPlayerId)
            ) {
                actionState.action.toAbilityRollControls()
            } else {
                emptyList()
            }
    }

/**
 * Whether [control] is an Impersonate the Illusionist cannot meaningfully use against the chosen
 * target. Re-copying a class whose offensive ability has already been impersonated is a no-op, so
 * its control is disabled while that target is selected.
 */
internal fun ArenaState.Combat.isImpersonateRedundant(
    control: ArenaCombatControl,
    targetedOpponentUserId: String,
): Boolean {
    val isImpersonateControl =
        control is ArenaCombatControl.ActivatedAbility &&
            control.action is AbilityActionState.Activated.Impersonate
    if (!isImpersonateControl) return false

    val impersonateAbility = userPlayer.characterClass.offensiveAbility
    val copiedAbility =
        if (impersonateAbility is OffensiveAbilityState.Impersonate) {
            impersonateAbility.impersonatedAbility
        } else {
            OffensiveAbilityState.None
        }
    if (copiedAbility is OffensiveAbilityState.None) return false

    val target = playerMap[targetedOpponentUserId] ?: return false
    return target.characterClass.offensiveAbility::class == copiedAbility::class
}

/**
 * The controls for an ability roll the user owns. Most abilities are a single roll, but Mirror
 * Image is a choice: reroll either of the attacker's two dice, or keep the roll as-is.
 */
private fun AbilityActionState.toAbilityRollControls(): List<ArenaCombatControl> =
    when (this) {
        is AbilityActionState.Passive.MirrorImage -> mirrorImageControls()

        else ->
            listOf(
                element =
                    ArenaCombatControl.AbilityRoll(
                        action = this,
                        label = label,
                        requiresTarget = requiresTarget,
                    )
            )
    }

private fun AbilityActionState.Passive.MirrorImage.mirrorImageControls(): List<ArenaCombatControl> =
    listOf(
        ArenaCombatControl.AbilityRoll(
            action = copy(rerollSelection = MirrorImageReroll.First),
            label = "Reroll the ${firstDie.originalRoll}",
            requiresTarget = false,
        ),
        ArenaCombatControl.AbilityRoll(
            action = copy(rerollSelection = MirrorImageReroll.Second),
            label = "Reroll the ${secondDie.originalRoll}",
            requiresTarget = false,
        ),
        ArenaCombatControl.AbilityRoll(
            action = copy(rerollSelection = MirrorImageReroll.None),
            label = "Keep roll",
            requiresTarget = false,
        ),
    )

/**
 * The user is already in [CombatActionState.activeUserIds] when this is checked. The Teleport
 * roll-off is handed to one combatant at a time (the owner of the next unrolled die, SS-162), so
 * confirm the active user still owes their die before showing the roll control.
 */
private fun AbilityActionState.isUserTheRoller(userPlayerId: String): Boolean =
    when (this) {
        is AbilityActionState.Passive.Teleport ->
            (attackerRoll.userId == userPlayerId && attackerRoll.roll == null) ||
                (defenderRoll.userId == userPlayerId && defenderRoll.roll == null)

        else -> true
    }

private fun OffensiveAbilityState.toActivatedControls(): List<ArenaCombatControl.ActivatedAbility> =
    when (this) {
        is OffensiveAbilityState.AllIn ->
            listOf(
                element =
                    AbilityActionState.Activated.AllIn(
                            allInAbility =
                                OffensiveAbility.AllIn(
                                    hasAnotherAttack = hasAnotherAttack,
                                    usedThisTurn = usedThisTurn,
                                )
                        )
                        // All-In can only be activated once per turn
                        .toControl(enabled = !usedThisTurn)
            )

        is OffensiveAbilityState.PrepareAttack ->
            listOf(element = AbilityActionState.Activated.PrepareAttack.toControl())

        is OffensiveAbilityState.SummonMinion ->
            listOf(
                element =
                    AbilityActionState.Activated.SummonMinion(
                            summonMinionAbility =
                                OffensiveAbility.SummonMinion(
                                    skeletonCount = skeletonCount,
                                    golemCount = golemCount,
                                )
                        )
                        .toControl()
            )

        is OffensiveAbilityState.Impersonate ->
            listOf(
                element =
                    AbilityActionState.Activated.Impersonate(
                            impersonateAbility =
                                OffensiveAbility.Impersonate(
                                    impersonatedAbility = impersonatedAbility.toDomain()
                                )
                        )
                        .toControl()
            ) + impersonatedAbility.toImpersonatedControls()

        is OffensiveAbilityState.KillingMomentum,
        is OffensiveAbilityState.Encore,
        is OffensiveAbilityState.SpellPotency,
        is OffensiveAbilityState.None -> emptyList()
    }

private fun OffensiveAbilityState.toImpersonatedControls():
    List<ArenaCombatControl.ActivatedAbility> =
    when (this) {
        // Impersonate cannot copy itself
        is OffensiveAbilityState.Impersonate -> emptyList()

        else -> toActivatedControls()
    }

private fun AbilityActionState.Activated.toControl(enabled: Boolean = true) =
    ArenaCombatControl.ActivatedAbility(
        action = this,
        label = label,
        requiresTarget = requiresTarget,
        enabled = enabled,
    )

// TODO(SS-029): Always StringResources
private val AbilityActionState.label: String
    get() =
        when (this) {
            is AbilityActionState.Activated.AllIn -> "All In"
            is AbilityActionState.Activated.PrepareAttack -> "Prepare Attack"
            is AbilityActionState.Activated.SummonMinion -> "Summon Minion"
            is AbilityActionState.Activated.Impersonate -> "Impersonate"
            is AbilityActionState.Passive.LuckyCoin -> "Lucky Coin"
            is AbilityActionState.Passive.Riposte -> "Riposte"
            is AbilityActionState.Passive.Persuade -> "Persuade"
            is AbilityActionState.Passive.Encore -> "Encore"
            is AbilityActionState.Passive.Teleport -> "Teleport"
            is AbilityActionState.Passive.MirrorImage -> "Mirror Image"
            is AbilityActionState.Passive.HolyShield -> "Holy Shield"
        }

private val AbilityActionState.requiresTarget: Boolean
    get() =
        when (this) {
            is AbilityActionState.Activated.PrepareAttack,
            is AbilityActionState.Activated.Impersonate,
            is AbilityActionState.Passive.Persuade.Charm -> true

            else -> false
        }
