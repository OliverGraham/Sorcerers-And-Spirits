package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.isMinionAttackInProgress
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain

// TODO(SS-257): Refactor this file - move functions into the files they belong in. Also, think of
// reducing
//       the unnecessary mapping here. Rename it, if properties can't simply be used in
//       ArenaState.Combat
//

/**
 * A combat action the user can currently take, derived from [ArenaMatchState.Combat] by
 * [deriveCombatControls].
 */
@Immutable
sealed interface ArenaCombatControl {
    val label: String
    val requiresTarget: Boolean
    val enabled: Boolean
    val numberOfDice: NumberOfDice
    val rollingDiePositions: Set<DiePosition>

    @Immutable
    data class NormalAttack(
        override val requiresTarget: Boolean,
        override val numberOfDice: NumberOfDice = NumberOfDice.Two,
    ) : ArenaCombatControl {
        override val label: String = "Attack"
        override val enabled: Boolean = true
        override val rollingDiePositions = numberOfDice.toDiePositions()
    }

    @Immutable
    data class ActivatedAbility(
        val action: AbilityActionState.Activated,
        override val label: String,
        override val requiresTarget: Boolean,
        override val enabled: Boolean,
    ) : ArenaCombatControl {
        override val numberOfDice = action.getNumberOfDice()
        override val rollingDiePositions = numberOfDice.toDiePositions()
    }

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        override val label: String,
        override val requiresTarget: Boolean,
        override val rollingDiePositions: Set<DiePosition>,
        override val enabled: Boolean = true,
    ) : ArenaCombatControl {
        override val numberOfDice: NumberOfDice = action.getNumberOfDice()
    }
}

enum class NumberOfDice {
    One,
    Two,
    None,
}

private fun NumberOfDice.toDiePositions(): Set<DiePosition> = when (this) {
    NumberOfDice.None -> emptySet()
    NumberOfDice.One -> setOf(DiePosition.First)
    NumberOfDice.Two -> setOf(DiePosition.First, DiePosition.Second)
}

private fun AbilityActionState.resolveRollingDiePositions(userPlayerId: String): Set<DiePosition> = when (getNumberOfDice()) {
    NumberOfDice.None -> emptySet()
    NumberOfDice.Two -> setOf(DiePosition.First, DiePosition.Second)
    NumberOfDice.One -> setOf(singleRollDiePosition(userPlayerId = userPlayerId))
}

private fun AbilityActionState.singleRollDiePosition(userPlayerId: String): DiePosition = when (this) {
    is AbilityActionState.Passive.Teleport ->
        localRollerDiePosition(userPlayerId = userPlayerId)

    is AbilityActionState.Passive.MirrorImage ->
        if (mirrorImageRerollSelection == MirrorImageRerollSelection.Second) {
            DiePosition.Second
        } else {
            DiePosition.First
        }

    else -> DiePosition.First
}

private fun AbilityActionState.Passive.Teleport.localRollerDiePosition(
    userPlayerId: String,
): DiePosition = if (attackerRoll.userId == userPlayerId) attackerRoll.diePosition else defenderRoll.diePosition

private fun AbilityActionState.getNumberOfDice() = when (this) {
    is AbilityActionState.Activated.AllIn -> NumberOfDice.One

    is AbilityActionState.Activated.SummonMinion -> NumberOfDice.One

    is AbilityActionState.Activated.PrepareAttack -> NumberOfDice.Two

    is AbilityActionState.Activated.Impersonate -> NumberOfDice.None

    is AbilityActionState.Passive.LuckyCoin -> NumberOfDice.One

    is AbilityActionState.Passive.Riposte -> NumberOfDice.Two

    is AbilityActionState.Passive.Persuade.StrumLute -> NumberOfDice.One

    is AbilityActionState.Passive.Persuade.Charm -> NumberOfDice.None

    is AbilityActionState.Passive.Encore -> NumberOfDice.One

    is AbilityActionState.Passive.Teleport -> NumberOfDice.One

    is AbilityActionState.Passive.MirrorImage ->
        if (mirrorImageRerollSelection == MirrorImageRerollSelection.None) {
            NumberOfDice.None
        } else {
            NumberOfDice.One
        }

    is AbilityActionState.Passive.HolyShield.Prayer -> NumberOfDice.Two

    is AbilityActionState.Passive.HolyShield.Wrath -> NumberOfDice.One
}

/**
 * Determines which combat controls the user can act on right now. An empty list means the user
 * cannot act and no action buttons should be shown.
 */
internal fun ArenaMatchState.Combat.deriveCombatControls(): List<ArenaCombatControl> {
    if (isMatchOver) return emptyList()

    return when (val actionState = combatActionState) {
        is CombatActionState.Idle ->
            if (isItThisUsersTurn) {
                val offensiveAbility = userPlayer.characterClass.offensiveAbility
                buildList {
                    add(element = ArenaCombatControl.NormalAttack(requiresTarget = true))
                    if (!offensiveAbility.isMinionAttackInProgress) {
                        addAll(elements = offensiveAbility.toActivatedControls())
                    }
                }
            } else {
                emptyList()
            }

        is CombatActionState.NormalAttack ->
            if (actionState.attackTurn.attackerUserId == userPlayerId) {
                listOf(element = ArenaCombatControl.NormalAttack(requiresTarget = false))
            } else {
                emptyList()
            }

        is CombatActionState.AbilityRoll ->
            if (
                isItThisUsersTurn && actionState.action.isUserTheRoller(userPlayerId = userPlayerId)
            ) {
                actionState.action.toAbilityRollControls(userPlayerId = userPlayerId)
            } else {
                emptyList()
            }
    }
}

private fun AbilityActionState.toAbilityRollControls(
    userPlayerId: String,
): List<ArenaCombatControl> = when (this) {
    is AbilityActionState.Passive.MirrorImage ->
        mirrorImageControls(userPlayerId = userPlayerId)

    else ->
        listOf(
            element =
            ArenaCombatControl.AbilityRoll(
                action = this,
                label = label,
                requiresTarget = requiresTarget,
                rollingDiePositions =
                resolveRollingDiePositions(userPlayerId = userPlayerId),
            ),
        )
}

private fun AbilityActionState.Passive.MirrorImage.mirrorImageControls(
    userPlayerId: String,
): List<ArenaCombatControl> = listOf(
    MirrorImageRerollSelection.First,
    MirrorImageRerollSelection.Second,
    MirrorImageRerollSelection.None,
)
    .map { selection -> copy(mirrorImageRerollSelection = selection) }
    .map { action ->
        ArenaCombatControl.AbilityRoll(
            action = action,
            label = action.mirrorImageControlLabel(),
            requiresTarget = false,
            rollingDiePositions = action.resolveRollingDiePositions(userPlayerId = userPlayerId),
        )
    }

private fun AbilityActionState.Passive.MirrorImage.mirrorImageControlLabel(): String = when (mirrorImageRerollSelection) {
    MirrorImageRerollSelection.First -> "Reroll the ${firstDie.originalRoll}"
    MirrorImageRerollSelection.Second -> "Reroll the ${secondDie.originalRoll}"
    MirrorImageRerollSelection.None -> "Keep roll"
}

internal fun AbilityActionState.isUserTheRoller(userPlayerId: String): Boolean = when (this) {
    is AbilityActionState.Passive.Teleport ->
        (attackerRoll.userId == userPlayerId && attackerRoll.roll == null) ||
            (defenderRoll.userId == userPlayerId && defenderRoll.roll == null)

    else -> true
}

internal fun OffensiveAbilityState.toActivatedControls(): List<ArenaCombatControl.ActivatedAbility> = when (this) {
    is OffensiveAbilityState.AllIn ->
        listOf(
            element =
            AbilityActionState.Activated.AllIn(
                allInAbility =
                OffensiveAbility.AllIn(
                    hasAnotherAttack = hasAnotherAttack,
                    usedThisTurn = usedThisTurn,
                ),
            )
                .toControl(enabled = !usedThisTurn),
        )

    is OffensiveAbilityState.PrepareAttack ->
        listOf(element = AbilityActionState.Activated.PrepareAttack.toControl())

    is OffensiveAbilityState.SummonMinion ->
        listOf(
            element =
            AbilityActionState.Activated.SummonMinion(
                summonMinionAbility =
                OffensiveAbility.SummonMinion(minions = minions.map { minion -> minion.toDomain() }),
            )
                .toControl(),
        )

    is OffensiveAbilityState.Impersonate ->
        listOf(
            element =
            AbilityActionState.Activated.Impersonate(
                impersonateAbility =
                OffensiveAbility.Impersonate(
                    impersonatedAbility = impersonatedAbility.toDomain(),
                ),
            )
                .toControl(),
        ) + impersonatedAbility.toImpersonatedControls()

    is OffensiveAbilityState.KillingMomentum,
    is OffensiveAbilityState.Encore,
    is OffensiveAbilityState.SpellPotency,
    is OffensiveAbilityState.None,
    -> emptyList()
}

private fun OffensiveAbilityState.toImpersonatedControls(): List<ArenaCombatControl.ActivatedAbility> = when (this) {
    // Impersonate cannot copy itself
    is OffensiveAbilityState.Impersonate -> emptyList()

    else -> toActivatedControls()
}

private fun AbilityActionState.Activated.toControl(enabled: Boolean = true) = ArenaCombatControl.ActivatedAbility(
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
