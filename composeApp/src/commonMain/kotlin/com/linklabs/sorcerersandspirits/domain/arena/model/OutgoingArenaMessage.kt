package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

sealed interface OutgoingArenaMessage {

    data class ChoseCharacterClass(
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : OutgoingArenaMessage

    data class ConnectedToArena(val displayName: String) : OutgoingArenaMessage

    data class NewRound(val roundNumber: Int) : OutgoingArenaMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : OutgoingArenaMessage

    data class NormalRoll(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : OutgoingArenaMessage

    data object KillingMomentum : OutgoingArenaMessage

    data class Dodge(val didArmorIncrease: Boolean) : OutgoingArenaMessage

    sealed interface Teleport : OutgoingArenaMessage {
        data object Trigger : Teleport

        data class Roll(val userId: String, val singleDie: Int) : Teleport

        data class Result(val didDefenderEscape: Boolean) : Teleport
    }

    // TODO LuckyCoin is this the right approach for messages?
    sealed interface LuckyCoin : OutgoingArenaMessage {

        data object Trigger : LuckyCoin

        data class Flip(val singleDie: Int, val didStopAttack: Boolean) : LuckyCoin

        data class HighRollerTrigger(val targetUserId: String) : LuckyCoin

        data class HighRollerDamage(val singleDie: Int, val isKnockout: Boolean) : LuckyCoin
    }

    sealed interface Riposte : OutgoingArenaMessage {
        data object Trigger : Riposte

        data class EnGarde(val firstDie: Int, val secondDie: Int, val isSuccessful: Boolean) :
            Riposte
    }

    data class SummonMinion(val necroUserId: String, val singleDie: Int) : OutgoingArenaMessage

    sealed interface Persuade : OutgoingArenaMessage {

        data object Trigger : Persuade

        data class StrumLute(val diceRoll: Int, val isSuccessful: Boolean) : Persuade

        data object CharmActivation : Persuade

        data object Lull : Persuade

        data class Charm(val attackerUserId: String, val defenderUserId: String) : Persuade
    }

    sealed interface Encore : OutgoingArenaMessage {
        data object Trigger : Encore

        data class Roll(val encoreDie: Int, val isSuccessful: Boolean) : OutgoingArenaMessage
    }

    data class Impersonate(
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : OutgoingArenaMessage

    // TODO MirrorImage
    sealed interface MirrorImage : OutgoingArenaMessage {
        data object Trigger : MirrorImage

        data object Pass : MirrorImage

        data class Reroll(val firstDie: Int, val secondDie: Int) : MirrorImage

        data class TemptFate(
            val rerolledMirrorImageDie: AbilityAction.Passive.MirrorImage.MirrorImageDie
        ) : MirrorImage
    }

    data class SpellPotency(
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : OutgoingArenaMessage

    sealed interface HolyShield : OutgoingArenaMessage {

        data object Trigger : HolyShield

        data class Prayer(val firstDie: Int, val secondDie: Int, val didSurvive: Boolean) :
            HolyShield

        data class Wrath(
            val paladinUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
        ) : HolyShield
    }

    data class AllIn(val gamblerUserId: String, val singleDie: Int) : OutgoingArenaMessage

    data class PrepareAttack(
        val duelistUserId: String,
        val targetUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : OutgoingArenaMessage
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
