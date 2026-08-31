package com.linklabs.sorcerersandspirits.domain.game

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

interface SummonMinionAbility<out M : MinionAttributes> {
    val minions: List<M>
    val attacksTakenThisTurn: Int
}

val SummonMinionAbility<*>.shieldPoints: Int
    get() = minions.sumOf { minion -> if (minion.type == MinionType.Golem) 2 else 1 }

val SummonMinionAbility<*>.minionAttacks: Int
    get() = minions.size

fun OffensiveAbility.SummonMinion.recordMinionAttack(): OffensiveAbility.SummonMinion {
    val nextMinionIndex = minions.indexOfFirst { minion -> !minion.hasAttackedThisRound }
    val updatedMinions = minions.mapIndexed { index, minion ->
        if (index == nextMinionIndex) {
            minion.copy(hasAttackedThisRound = true)
        } else {
            minion
        }
    }
    return copy(minions = updatedMinions, attacksTakenThisTurn = attacksTakenThisTurn + 1)
}

fun OffensiveAbility.SummonMinion.resetMinionsForNewRound(): OffensiveAbility.SummonMinion = copy(minions = minions.map { minion -> minion.copy(hasAttackedThisRound = false) })

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
        override val attacksTakenThisTurn: Int = 0,
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
