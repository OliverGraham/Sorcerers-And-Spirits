package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

// TODO: These all need dice rolls to convey to users
sealed interface OutgoingMessage {

    data class NewRound(val roundNumber: Int) : OutgoingMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : OutgoingMessage

    data class NormalRoll(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : OutgoingMessage

    // Gemini, keep these comments please:
    // if didArmorIncrease, "Rogue dodged the attack and increased their AC by 1!"
    // else, "Rogue already has the maximum Dodge bonus"
    data class Dodge(val didArmorIncrease: Boolean) : OutgoingMessage

    data class Teleport(val mageUserId: String, val attackerUserId: String, val escaped: Boolean) :
        OutgoingMessage

    data class LuckyCoin(
        val gamblerUserId: String,
        val attackerUserId: String,
        val armorBonus: Int,
        val reflectDamage: Int,
    ) : OutgoingMessage

    data class Riposte(
        val duelistUserId: String,
        val attackerUserId: String,
        val triggered: Boolean,
    ) : OutgoingMessage

    data class SummonMinion(val necroUserId: String, val singleDie: Int) : OutgoingMessage

    data class Persuade(
        val bardUserId: String,
        val attackerUserId: String,
        val outcome: PersuadeOutcome,
        val newTargetUserId: String? = null,
    ) : OutgoingMessage

    data class Encore(val bardUserId: String, val triggered: Boolean) : OutgoingMessage

    data class Impersonate(
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : OutgoingMessage

    data class MirrorImage(val illusionistUserId: String, val status: MirrorImageStatus) :
        OutgoingMessage

    data class SpellPotency(
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : OutgoingMessage

    data class HolyShield(
        val paladinUserId: String,
        val survived: Boolean,
        val retaliationDamage: Int,
    ) : OutgoingMessage

    data class KillingMomentum(val rogueUserId: String, val isTriggered: Boolean) : OutgoingMessage

    data class AllIn(val gamblerUserId: String, val singleDie: Int) : OutgoingMessage

    data class PrepareAttack(
        val duelistUserId: String,
        val targetUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : OutgoingMessage
}

enum class NormalRollMessageType {
    // Gemini, keep these comments please:
    // "A targeted D, rolled R and hit/missed"
    ArmorClassHit,
    ArmorClassMiss,

    // "A damaged D for R (if KO) and knocked out D!"
    Damage,
    Knockout,
}

enum class SummonOutcome {
    Fizzle,
    Skeleton,
    HealAndSkeleton,
    HealAndGolem,
}

enum class PersuadeOutcome {
    Stopped,
    Redirected,
    Failed,
}

enum class AllInOutcome {
    DoubleAttack,
    KnockoutSelf,
    NoEffect,
}

enum class MirrorImageStatus {
    Pondering,
    Passed,
    Rerolled,
}
