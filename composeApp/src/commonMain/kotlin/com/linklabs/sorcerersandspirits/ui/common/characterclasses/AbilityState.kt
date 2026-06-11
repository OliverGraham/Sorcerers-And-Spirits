package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

@Immutable sealed interface AbilityState

@Immutable sealed interface ActivatedAbilityState : AbilityState

@Immutable
sealed interface DefensiveAbilityState : AbilityState {
    @Immutable data class Dodge(val armorBonus: Int) : DefensiveAbilityState

    @Immutable data object LuckyCoin : DefensiveAbilityState

    @Immutable data object Riposte : DefensiveAbilityState

    @Immutable data object Persuade : DefensiveAbilityState

    @Immutable data class MirrorImage(val usedThisAttack: Boolean) : DefensiveAbilityState

    @Immutable data object Teleport : DefensiveAbilityState

    @Immutable data class HolyShield(val usedThisMatch: Boolean) : DefensiveAbilityState

    @Immutable data object None : DefensiveAbilityState
}

@Immutable
sealed interface OffensiveAbilityState : AbilityState {
    @Immutable data object KillingMomentum : OffensiveAbilityState

    @Immutable
    data class AllIn(val hasAnotherAttack: Boolean, val usedThisTurn: Boolean) :
        OffensiveAbilityState, ActivatedAbilityState

    @Immutable data object PrepareAttack : OffensiveAbilityState, ActivatedAbilityState

    @Immutable
    data class SummonMinion(val skeletonCount: Int = 0, val golemCount: Int = 0) :
        OffensiveAbilityState, ActivatedAbilityState

    @Immutable data object Encore : OffensiveAbilityState

    @Immutable
    data class Impersonate(val impersonatedAbility: OffensiveAbilityState = None) :
        OffensiveAbilityState, ActivatedAbilityState

    @Immutable data object SpellPotency : OffensiveAbilityState

    @Immutable data object None : OffensiveAbilityState
}

internal fun DefensiveAbility.toState(): DefensiveAbilityState =
    when (this) {
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

internal fun OffensiveAbility.toState(): OffensiveAbilityState =
    when (this) {
        is OffensiveAbility.KillingMomentum -> OffensiveAbilityState.KillingMomentum

        is OffensiveAbility.AllIn ->
            OffensiveAbilityState.AllIn(
                hasAnotherAttack = hasAnotherAttack,
                usedThisTurn = usedThisTurn,
            )

        is OffensiveAbility.PrepareAttack -> OffensiveAbilityState.PrepareAttack

        is OffensiveAbility.SummonMinion ->
            OffensiveAbilityState.SummonMinion(
                skeletonCount = skeletonCount,
                golemCount = golemCount,
            )

        is OffensiveAbility.Encore -> OffensiveAbilityState.Encore

        is OffensiveAbility.Impersonate ->
            OffensiveAbilityState.Impersonate(impersonatedAbility = impersonatedAbility.toState())

        is OffensiveAbility.SpellPotency -> OffensiveAbilityState.SpellPotency

        is OffensiveAbility.None -> OffensiveAbilityState.None
    }

internal fun DefensiveAbilityState.toDomain(): DefensiveAbility =
    when (this) {
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

internal fun OffensiveAbilityState.toDomain(): OffensiveAbility =
    when (this) {
        is OffensiveAbilityState.KillingMomentum -> OffensiveAbility.KillingMomentum

        is OffensiveAbilityState.AllIn ->
            OffensiveAbility.AllIn(hasAnotherAttack = hasAnotherAttack, usedThisTurn = usedThisTurn)

        is OffensiveAbilityState.PrepareAttack -> OffensiveAbility.PrepareAttack

        is OffensiveAbilityState.SummonMinion ->
            OffensiveAbility.SummonMinion(skeletonCount = skeletonCount, golemCount = golemCount)

        is OffensiveAbilityState.Encore -> OffensiveAbility.Encore

        is OffensiveAbilityState.Impersonate ->
            OffensiveAbility.Impersonate(impersonatedAbility = impersonatedAbility.toDomain())

        is OffensiveAbilityState.SpellPotency -> OffensiveAbility.SpellPotency

        is OffensiveAbilityState.None -> OffensiveAbility.None
    }
