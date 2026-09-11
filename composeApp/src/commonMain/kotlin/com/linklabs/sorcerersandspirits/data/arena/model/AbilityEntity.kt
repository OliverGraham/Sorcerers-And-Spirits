package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.ability.Ability
import com.linklabs.sorcerersandspirits.domain.game.ability.AllInAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.DodgeAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.HolyShieldAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.ImpersonateAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.MirrorImageAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.SummonMinionAbility
import kotlinx.serialization.Serializable

@Serializable sealed interface AbilityEntity

@Serializable sealed interface ActivatedAbilityEntity : AbilityEntity

@Serializable
sealed interface DefensiveAbilityEntity : AbilityEntity {
    @Serializable
    data class Dodge(override val armorBonus: Int = 0) :
        DefensiveAbilityEntity,
        DodgeAbility

    @Serializable data object LuckyCoin : DefensiveAbilityEntity

    @Serializable data object Riposte : DefensiveAbilityEntity

    @Serializable data object Persuade : DefensiveAbilityEntity

    @Serializable
    data class MirrorImage(override val usedThisAttack: Boolean = false) :
        DefensiveAbilityEntity,
        MirrorImageAbility

    @Serializable data object Teleport : DefensiveAbilityEntity

    @Serializable
    data class HolyShield(override val usedThisMatch: Boolean = false) :
        DefensiveAbilityEntity,
        HolyShieldAbility

    @Serializable data object None : DefensiveAbilityEntity
}

@Serializable
sealed interface OffensiveAbilityEntity : AbilityEntity {
    @Serializable data object KillingMomentum : OffensiveAbilityEntity

    @Serializable
    data class AllIn(
        override val hasAnotherAttack: Boolean = false,
        override val usedThisTurn: Boolean = false,
    ) : OffensiveAbilityEntity,
        ActivatedAbilityEntity,
        AllInAbility

    @Serializable data object PrepareAttack : OffensiveAbilityEntity, ActivatedAbilityEntity

    @Serializable
    data class SummonMinion(
        override val minions: List<MinionEntity>,
        override val usedThisTurn: Boolean,
    ) : OffensiveAbilityEntity,
        ActivatedAbilityEntity,
        SummonMinionAbility<MinionEntity>

    @Serializable data object Encore : OffensiveAbilityEntity

    @Serializable
    data class Impersonate(override val impersonatedAbility: OffensiveAbilityEntity = None) :
        OffensiveAbilityEntity,
        ActivatedAbilityEntity,
        ImpersonateAbility<OffensiveAbilityEntity>

    @Serializable data object SpellPotency : OffensiveAbilityEntity

    @Serializable data object None : OffensiveAbilityEntity
}

internal fun OffensiveAbilityEntity.resetForNewMatch(): OffensiveAbilityEntity = when (this) {
    is OffensiveAbilityEntity.AllIn -> OffensiveAbilityEntity.AllIn()

    is OffensiveAbilityEntity.SummonMinion -> OffensiveAbilityEntity.SummonMinion(
        minions = emptyList(),
        usedThisTurn = false,
    )

    is OffensiveAbilityEntity.Impersonate -> OffensiveAbilityEntity.Impersonate()

    is OffensiveAbilityEntity.KillingMomentum,
    is OffensiveAbilityEntity.PrepareAttack,
    is OffensiveAbilityEntity.Encore,
    is OffensiveAbilityEntity.SpellPotency,
    is OffensiveAbilityEntity.None,
    -> this
}

internal fun DefensiveAbilityEntity.resetForNewMatch(): DefensiveAbilityEntity = when (this) {
    is DefensiveAbilityEntity.Dodge -> DefensiveAbilityEntity.Dodge()

    is DefensiveAbilityEntity.MirrorImage -> DefensiveAbilityEntity.MirrorImage()

    is DefensiveAbilityEntity.HolyShield -> DefensiveAbilityEntity.HolyShield()

    is DefensiveAbilityEntity.LuckyCoin,
    is DefensiveAbilityEntity.Riposte,
    is DefensiveAbilityEntity.Persuade,
    is DefensiveAbilityEntity.Teleport,
    is DefensiveAbilityEntity.None,
    -> this
}

internal fun Ability.toEntity(): AbilityEntity = when (this) {
    is DefensiveAbility -> toEntity()
    is OffensiveAbility -> toEntity()
}

internal fun AbilityEntity.toDomain(): Ability = when (this) {
    is DefensiveAbilityEntity -> toDomain()
    is OffensiveAbilityEntity -> toDomain()
}

internal fun DefensiveAbility.toEntity(): DefensiveAbilityEntity = when (this) {
    is DefensiveAbility.Dodge -> DefensiveAbilityEntity.Dodge(armorBonus = armorBonus)

    is DefensiveAbility.LuckyCoin -> DefensiveAbilityEntity.LuckyCoin

    is DefensiveAbility.Riposte -> DefensiveAbilityEntity.Riposte

    is DefensiveAbility.Persuade -> DefensiveAbilityEntity.Persuade

    is DefensiveAbility.MirrorImage ->
        DefensiveAbilityEntity.MirrorImage(usedThisAttack = usedThisAttack)

    is DefensiveAbility.Teleport -> DefensiveAbilityEntity.Teleport

    is DefensiveAbility.HolyShield ->
        DefensiveAbilityEntity.HolyShield(usedThisMatch = usedThisMatch)

    is DefensiveAbility.None -> DefensiveAbilityEntity.None
}

internal fun OffensiveAbility.toEntity(): OffensiveAbilityEntity = when (this) {
    is OffensiveAbility.KillingMomentum -> OffensiveAbilityEntity.KillingMomentum

    is OffensiveAbility.AllIn ->
        OffensiveAbilityEntity.AllIn(
            hasAnotherAttack = hasAnotherAttack,
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbility.PrepareAttack -> OffensiveAbilityEntity.PrepareAttack

    is OffensiveAbility.SummonMinion ->
        OffensiveAbilityEntity.SummonMinion(
            minions = minions.map { minion -> minion.toEntity() },
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbility.Encore -> OffensiveAbilityEntity.Encore

    is OffensiveAbility.Impersonate ->
        OffensiveAbilityEntity.Impersonate(impersonatedAbility = impersonatedAbility.toEntity())

    is OffensiveAbility.SpellPotency -> OffensiveAbilityEntity.SpellPotency

    is OffensiveAbility.None -> OffensiveAbilityEntity.None
}

internal fun DefensiveAbilityEntity.toDomain(): DefensiveAbility = when (this) {
    is DefensiveAbilityEntity.Dodge -> DefensiveAbility.Dodge(armorBonus = armorBonus)

    is DefensiveAbilityEntity.LuckyCoin -> DefensiveAbility.LuckyCoin

    is DefensiveAbilityEntity.Riposte -> DefensiveAbility.Riposte

    is DefensiveAbilityEntity.Persuade -> DefensiveAbility.Persuade

    is DefensiveAbilityEntity.MirrorImage ->
        DefensiveAbility.MirrorImage(usedThisAttack = usedThisAttack)

    is DefensiveAbilityEntity.Teleport -> DefensiveAbility.Teleport

    is DefensiveAbilityEntity.HolyShield ->
        DefensiveAbility.HolyShield(usedThisMatch = usedThisMatch)

    is DefensiveAbilityEntity.None -> DefensiveAbility.None
}

internal fun OffensiveAbilityEntity.toDomain(): OffensiveAbility = when (this) {
    is OffensiveAbilityEntity.KillingMomentum -> OffensiveAbility.KillingMomentum

    is OffensiveAbilityEntity.AllIn ->
        OffensiveAbility.AllIn(hasAnotherAttack = hasAnotherAttack, usedThisTurn = usedThisTurn)

    is OffensiveAbilityEntity.PrepareAttack -> OffensiveAbility.PrepareAttack

    is OffensiveAbilityEntity.SummonMinion ->
        OffensiveAbility.SummonMinion(
            minions = minions.map { minion -> minion.toDomain() },
            usedThisTurn = usedThisTurn,
        )

    is OffensiveAbilityEntity.Encore -> OffensiveAbility.Encore

    is OffensiveAbilityEntity.Impersonate ->
        OffensiveAbility.Impersonate(impersonatedAbility = impersonatedAbility.toDomain())

    is OffensiveAbilityEntity.SpellPotency -> OffensiveAbility.SpellPotency

    is OffensiveAbilityEntity.None -> OffensiveAbility.None
}
