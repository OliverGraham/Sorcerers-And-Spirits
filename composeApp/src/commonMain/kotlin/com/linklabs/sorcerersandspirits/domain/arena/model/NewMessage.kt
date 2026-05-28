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

    sealed interface Teleport : OutgoingMessage {
        data object Trigger : Teleport

        data class Roll(val userId: String, val singleDie: Int) : Teleport

        data class Result(val didDefenderEscape: Boolean) : Teleport
    }

    sealed interface LuckyCoin : OutgoingMessage {

        data object Trigger : LuckyCoin

        data class ArmorClassRoll(val singleDie: Int) : LuckyCoin

        data class ReflectRoll(val singleDie: Int) : LuckyCoin
    }

    sealed interface Riposte : OutgoingMessage {
        data object Trigger : Riposte

        data class EnGarde(val firstDie: Int, val secondDie: Int)
    }

    data class SummonMinion(val necroUserId: String, val singleDie: Int) : OutgoingMessage

    sealed interface Persuade : OutgoingMessage {

        data object Trigger : Persuade

        data object Lull : Persuade

        data class Charm(val charmedPlayerUserId: String) : Persuade
    }

    data class Encore(val bardUserId: String, val triggered: Boolean) : OutgoingMessage

    data class Impersonate(
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : OutgoingMessage

    sealed interface MirrorImage : OutgoingMessage {
        data object Trigger : MirrorImage

        data object Pass : MirrorImage

        data class Reroll(val firstDie: Int, val secondDie: Int) : MirrorImage
    }

    data class SpellPotency(
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : OutgoingMessage

    sealed interface HolyShield : OutgoingMessage {

        data object Trigger : HolyShield

        data class Survival(
            val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        data class Reflect(val paladinUserId: String, val singleDie: Int) : HolyShield
    }

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
