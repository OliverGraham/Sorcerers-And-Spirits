package com.linklabs.sorcerersandspirits.domain.game.ability

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.Minion
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.SummonMinionAbility

sealed interface Ability

sealed interface ActivatedAbility : Ability

interface DodgeAbility {
    val armorBonus: Int
}

interface MirrorImageAbility {
    val usedThisAttack: Boolean
}

interface HolyShieldAbility {
    val usedThisMatch: Boolean
}

sealed interface DefensiveAbility : Ability {
    data class Dodge(override val armorBonus: Int = 0) :
        DefensiveAbility,
        DodgeAbility

    data object LuckyCoin : DefensiveAbility

    data object Riposte : DefensiveAbility

    data object Persuade : DefensiveAbility

    data class MirrorImage(override val usedThisAttack: Boolean = false) :
        DefensiveAbility,
        MirrorImageAbility

    data object Teleport : DefensiveAbility

    data class HolyShield(override val usedThisMatch: Boolean = false) :
        DefensiveAbility,
        HolyShieldAbility

    data object None : DefensiveAbility
}

interface AllInAbility {
    val hasAnotherAttack: Boolean
    val usedThisTurn: Boolean
}

interface ImpersonateAbility<out T> {
    val impersonatedAbility: T
}

sealed interface OffensiveAbility : Ability {
    data object KillingMomentum : OffensiveAbility

    data class AllIn(
        override val hasAnotherAttack: Boolean = false,
        override val usedThisTurn: Boolean = false,
    ) : OffensiveAbility,
        ActivatedAbility,
        AllInAbility

    data object PrepareAttack : OffensiveAbility, ActivatedAbility

    data class SummonMinion(
        override val minions: List<Minion> = emptyList(),
        override val usedThisTurn: Boolean = false,
    ) : OffensiveAbility,
        ActivatedAbility,
        SummonMinionAbility<Minion>

    data object Encore : OffensiveAbility

    data class Impersonate(override val impersonatedAbility: OffensiveAbility = None) :
        OffensiveAbility,
        ActivatedAbility,
        ImpersonateAbility<OffensiveAbility> {
        val impersonatedClassType = impersonatedAbility.effective.owningClassType()
    }

    data object SpellPotency : OffensiveAbility

    data object None : OffensiveAbility
}

val OffensiveAbility.effective: OffensiveAbility
    get() = if (this is OffensiveAbility.Impersonate) impersonatedAbility else this

private fun OffensiveAbility.owningClassType(): CharacterClassType = when (this) {
    is OffensiveAbility.KillingMomentum -> CharacterClassType.Rogue

    is OffensiveAbility.AllIn -> CharacterClassType.Gambler

    is OffensiveAbility.PrepareAttack -> CharacterClassType.Duelist

    is OffensiveAbility.SummonMinion -> CharacterClassType.Necromancer

    is OffensiveAbility.Encore -> CharacterClassType.Bard

    is OffensiveAbility.SpellPotency -> CharacterClassType.Mage

    is OffensiveAbility.Impersonate,
    is OffensiveAbility.None,
    -> CharacterClassType.Illusionist
}

val OffensiveAbility.defaultState: OffensiveAbility
    get() =
        when (this) {
            is OffensiveAbility.AllIn -> OffensiveAbility.AllIn()
            is OffensiveAbility.SummonMinion -> OffensiveAbility.SummonMinion()
            is OffensiveAbility.Impersonate -> OffensiveAbility.Impersonate()
            is OffensiveAbility.KillingMomentum -> this
            is OffensiveAbility.PrepareAttack -> this
            is OffensiveAbility.Encore -> this
            is OffensiveAbility.SpellPotency -> this
            is OffensiveAbility.None -> this
        }

fun OffensiveAbility.withEffective(updated: OffensiveAbility) = if (this is OffensiveAbility.Impersonate) copy(impersonatedAbility = updated) else updated
