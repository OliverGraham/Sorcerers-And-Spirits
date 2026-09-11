package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.ability.AllInAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.DodgeAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.HolyShieldAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.ImpersonateAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.MirrorImageAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.SummonMinionAbility

@Immutable sealed interface AbilityState

@Immutable sealed interface ActivatedAbilityState : AbilityState

@Immutable
sealed interface DefensiveAbilityState : AbilityState {
    @Immutable data class Dodge(override val armorBonus: Int) :
        DefensiveAbilityState,
        DodgeAbility

    @Immutable data object LuckyCoin : DefensiveAbilityState

    @Immutable data object Riposte : DefensiveAbilityState

    @Immutable data object Persuade : DefensiveAbilityState

    @Immutable
    data class MirrorImage(override val usedThisAttack: Boolean) :
        DefensiveAbilityState,
        MirrorImageAbility

    @Immutable data object Teleport : DefensiveAbilityState

    @Immutable
    data class HolyShield(override val usedThisMatch: Boolean) :
        DefensiveAbilityState,
        HolyShieldAbility

    @Immutable data object None : DefensiveAbilityState
}

@Immutable
sealed interface OffensiveAbilityState : AbilityState {
    @Immutable data object KillingMomentum : OffensiveAbilityState

    @Immutable
    data class AllIn(override val hasAnotherAttack: Boolean, override val usedThisTurn: Boolean) :
        OffensiveAbilityState,
        ActivatedAbilityState,
        AllInAbility

    @Immutable data object PrepareAttack : OffensiveAbilityState, ActivatedAbilityState

    @Immutable
    data class SummonMinion(
        override val minions: List<MinionState>,
        override val usedThisTurn: Boolean,
    ) : OffensiveAbilityState,
        ActivatedAbilityState,
        SummonMinionAbility<MinionState>

    @Immutable data object Encore : OffensiveAbilityState

    @Immutable
    data class Impersonate(override val impersonatedAbility: OffensiveAbilityState = None) :
        OffensiveAbilityState,
        ActivatedAbilityState,
        ImpersonateAbility<OffensiveAbilityState>

    @Immutable data object SpellPotency : OffensiveAbilityState

    @Immutable data object None : OffensiveAbilityState
}

// TODO: Refactor these extension vals
internal val OffensiveAbilityState.effective: OffensiveAbilityState
    get() = if (this is OffensiveAbilityState.Impersonate) impersonatedAbility else this

internal val OffensiveAbilityState.isMinionAttackInProgress: Boolean
    get() {
        val summonMinion = effective as? OffensiveAbilityState.SummonMinion ?: return false
        return summonMinion.minions.any { it.hasAttackedThisRound }
    }

internal fun DefensiveAbility.toState(): DefensiveAbilityState = when (this) {
    is DefensiveAbility.Dodge -> DefensiveAbilityState.Dodge(armorBonus = armorBonus)

    is DefensiveAbility.LuckyCoin -> DefensiveAbilityState.LuckyCoin

    is DefensiveAbility.Riposte -> DefensiveAbilityState.Riposte

    is DefensiveAbility.Persuade -> DefensiveAbilityState.Persuade

    is DefensiveAbility.MirrorImage ->
        DefensiveAbilityState.MirrorImage(usedThisAttack = usedThisAttack)

    is DefensiveAbility.Teleport -> DefensiveAbilityState.Teleport

    is DefensiveAbility.HolyShield ->
        DefensiveAbilityState.HolyShield(usedThisMatch = usedThisMatch)

    is DefensiveAbility.None -> DefensiveAbilityState.None
}

internal fun OffensiveAbility.toState(): OffensiveAbilityState = when (this) {
    is OffensiveAbility.KillingMomentum -> OffensiveAbilityState.KillingMomentum

    is OffensiveAbility.AllIn ->
        OffensiveAbilityState.AllIn(
            hasAnotherAttack = hasAnotherAttack,
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbility.PrepareAttack -> OffensiveAbilityState.PrepareAttack

    is OffensiveAbility.SummonMinion ->
        OffensiveAbilityState.SummonMinion(
            minions = minions.map { minion -> minion.toState() },
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbility.Encore -> OffensiveAbilityState.Encore

    is OffensiveAbility.Impersonate ->
        OffensiveAbilityState.Impersonate(impersonatedAbility = impersonatedAbility.toState())

    is OffensiveAbility.SpellPotency -> OffensiveAbilityState.SpellPotency

    is OffensiveAbility.None -> OffensiveAbilityState.None
}

internal fun DefensiveAbilityState.toDomain(): DefensiveAbility = when (this) {
    is DefensiveAbilityState.Dodge -> DefensiveAbility.Dodge(armorBonus = armorBonus)

    is DefensiveAbilityState.LuckyCoin -> DefensiveAbility.LuckyCoin

    is DefensiveAbilityState.Riposte -> DefensiveAbility.Riposte

    is DefensiveAbilityState.Persuade -> DefensiveAbility.Persuade

    is DefensiveAbilityState.MirrorImage ->
        DefensiveAbility.MirrorImage(usedThisAttack = usedThisAttack)

    is DefensiveAbilityState.Teleport -> DefensiveAbility.Teleport

    is DefensiveAbilityState.HolyShield ->
        DefensiveAbility.HolyShield(usedThisMatch = usedThisMatch)

    is DefensiveAbilityState.None -> DefensiveAbility.None
}

internal fun OffensiveAbilityState.toDomain(): OffensiveAbility = when (this) {
    is OffensiveAbilityState.KillingMomentum -> OffensiveAbility.KillingMomentum

    is OffensiveAbilityState.AllIn ->
        OffensiveAbility.AllIn(hasAnotherAttack = hasAnotherAttack, usedThisTurn = usedThisTurn)

    is OffensiveAbilityState.PrepareAttack -> OffensiveAbility.PrepareAttack

    is OffensiveAbilityState.SummonMinion ->
        OffensiveAbility.SummonMinion(
            minions = minions.map { minion -> minion.toDomain() },
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbilityState.Encore -> OffensiveAbility.Encore

    is OffensiveAbilityState.Impersonate ->
        OffensiveAbility.Impersonate(impersonatedAbility = impersonatedAbility.toDomain())

    is OffensiveAbilityState.SpellPotency -> OffensiveAbility.SpellPotency

    is OffensiveAbilityState.None -> OffensiveAbility.None
}
