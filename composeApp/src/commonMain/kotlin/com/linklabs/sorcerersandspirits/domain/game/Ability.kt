package com.linklabs.sorcerersandspirits.domain.game

sealed interface Ability

sealed interface ActivatedAbility : Ability

sealed interface DefensiveAbility : Ability {
    data class Dodge(val armorBonus: Int = 0) : DefensiveAbility

    data object LuckyCoin : DefensiveAbility

    data object Riposte : DefensiveAbility

    data object Persuade : DefensiveAbility

    data class MirrorImage(val usedThisRound: Boolean = false) : DefensiveAbility

    data object Teleport : DefensiveAbility

    data class HolyShield(val usedThisMatch: Boolean = false) : DefensiveAbility

    data object None : DefensiveAbility
}

sealed interface OffensiveAbility : Ability {
    data object KillingMomentum : OffensiveAbility

    data class AllIn(val hasAnotherAttack: Boolean = false) : OffensiveAbility, ActivatedAbility

    data object PrepareAttack : OffensiveAbility, ActivatedAbility

    // TODO: Can Necro have more than 1 skeleton? Or Golem?
    data class SummonMinion(val skeletonCount: Int = 0, val golemCount: Int = 0) :
        OffensiveAbility, ActivatedAbility

    data object Encore : OffensiveAbility

    data class Impersonate(val impersonatedAbility: OffensiveAbility = None) :
        OffensiveAbility, ActivatedAbility

    data object SpellPotency : OffensiveAbility

    data object None : OffensiveAbility
}
