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

interface SummonMinionAbility {
    val minions: List<Minion>
    val attacksTakenThisTurn: Int
}

val SummonMinionAbility.shieldPoints: Int
    get() = minions.sumOf { minion -> if (minion == Minion.Golem) 2 else 1 }

val SummonMinionAbility.minionAttacks: Int
    get() = minions.size

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
        SummonMinionAbility

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

/**
 * The offensive ability whose effect actually applies. An Illusionist that has Impersonated an
 * opponent attacks with the copied ability, so [OffensiveAbility.Impersonate] resolves to the
 * ability it copied; every other ability is itself.
 */
val OffensiveAbility.effective: OffensiveAbility
    get() = if (this is OffensiveAbility.Impersonate) impersonatedAbility else this

/**
 * The class an offensive ability belongs to. Drives the Illusionist's borrowed normal attack, which
 * renders the weapon of the class whose offensive ability it copied (SS-197). Impersonate itself
 * and None are the Illusionist's own, with no opponent class to borrow from.
 */
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

/**
 * The ability stripped of any state it has accumulated this match, leaving only its behaviour. An
 * Illusionist copies an opponent's ability, not the progress the opponent has made with it, so the
 * Necromancer's already-summoned minions or the Gambler's spent All-In must not come across with
 * the copy.
 */
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

/**
 * Writes back an [updated] effective ability, keeping an Impersonate wrapper intact. An Illusionist
 * keeps a copied ability until end of match or until another is chosen, so a copied ability that
 * carries state across a turn (All-In's [OffensiveAbility.AllIn.usedThisTurn]/
 * [OffensiveAbility.AllIn.hasAnotherAttack], Summon Minion's minion counts) must be re-wrapped
 * after it mutates rather than replacing the [OffensiveAbility.Impersonate]. For every other
 * ability this is simply [updated].
 */
fun OffensiveAbility.withEffective(updated: OffensiveAbility): OffensiveAbility = if (this is OffensiveAbility.Impersonate) copy(impersonatedAbility = updated) else updated
