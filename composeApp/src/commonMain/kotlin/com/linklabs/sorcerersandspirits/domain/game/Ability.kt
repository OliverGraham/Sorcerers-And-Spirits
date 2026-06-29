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
    data class Dodge(override val armorBonus: Int = 0) : DefensiveAbility, DodgeAbility

    data object LuckyCoin : DefensiveAbility

    data object Riposte : DefensiveAbility

    data object Persuade : DefensiveAbility

    data class MirrorImage(override val usedThisAttack: Boolean = false) :
        DefensiveAbility, MirrorImageAbility

    data object Teleport : DefensiveAbility

    data class HolyShield(override val usedThisMatch: Boolean = false) :
        DefensiveAbility, HolyShieldAbility

    data object None : DefensiveAbility
}

interface AllInAbility {
    val hasAnotherAttack: Boolean
    val usedThisTurn: Boolean
}

interface SummonMinionAbility {
    val skeletonCount: Int
    val golemCount: Int
    val attacksTakenThisTurn: Int
}

/**
 * The shield total contributed by summoned minions, counted as effective hit points: each skeleton
 * is a +1 shield and each golem is a +2 shield.
 */
val SummonMinionAbility.shieldPoints: Int
    get() = skeletonCount + golemCount * 2

/**
 * The follow-up attacks a summoner gets on their turn: one per minion held. Counted off live, so a
 * minion consumed as a shield this turn no longer grants its attack.
 */
val SummonMinionAbility.minionAttacks: Int
    get() = skeletonCount + golemCount

interface ImpersonateAbility<out T> {
    val impersonatedAbility: T
}

sealed interface OffensiveAbility : Ability {
    data object KillingMomentum : OffensiveAbility

    data class AllIn(
        override val hasAnotherAttack: Boolean = false,
        override val usedThisTurn: Boolean = false,
    ) : OffensiveAbility, ActivatedAbility, AllInAbility

    data object PrepareAttack : OffensiveAbility, ActivatedAbility

    data class SummonMinion(
        override val skeletonCount: Int = 0,
        override val golemCount: Int = 0,
        override val attacksTakenThisTurn: Int = 0,
    ) : OffensiveAbility, ActivatedAbility, SummonMinionAbility

    data object Encore : OffensiveAbility

    data class Impersonate(override val impersonatedAbility: OffensiveAbility = None) :
        OffensiveAbility, ActivatedAbility, ImpersonateAbility<OffensiveAbility>

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
fun OffensiveAbility.withEffective(updated: OffensiveAbility): OffensiveAbility =
    if (this is OffensiveAbility.Impersonate) copy(impersonatedAbility = updated) else updated
