package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

sealed interface ArenaMessage {

    data class ChoseCharacterClass(
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : ArenaMessage

    data class ConnectedToArena(val displayName: String) : ArenaMessage

    data class NewRound(val roundNumber: Int) : ArenaMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : ArenaMessage

    // TODO: Maybe?
    // data class Doubles(val userId: String) : ArenaMessage

    data class NormalRoll(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : ArenaMessage

    data object KillingMomentum : ArenaMessage

    data class Dodge(val didArmorIncrease: Boolean) : ArenaMessage

    sealed interface Teleport : ArenaMessage {
        data object Trigger : Teleport

        data class Roll(val userId: String, val singleDie: Int) : Teleport

        data class Result(val didDefenderEscape: Boolean) : Teleport
    }

    // TODO LuckyCoin is this the right approach for messages?
    sealed interface LuckyCoin : ArenaMessage {

        data object Trigger : LuckyCoin

        data class Flip(val singleDie: Int, val didStopAttack: Boolean) : LuckyCoin

        data class HighRollerTrigger(val targetUserId: String) : LuckyCoin

        data class HighRollerDamage(val singleDie: Int, val isKnockout: Boolean) : LuckyCoin
    }

    sealed interface Riposte : ArenaMessage {
        data object Trigger : Riposte

        data class EnGarde(val firstDie: Int, val secondDie: Int, val isSuccessful: Boolean) :
            Riposte
    }

    data class SummonMinion(val necroUserId: String, val singleDie: Int) : ArenaMessage

    sealed interface Persuade : ArenaMessage {

        data object Trigger : Persuade

        data class StrumLute(val diceRoll: Int, val isSuccessful: Boolean) : Persuade

        data object CharmActivation : Persuade

        data object Lull : Persuade

        data class Charm(val attackerUserId: String, val defenderUserId: String) : Persuade
    }

    sealed interface Encore : ArenaMessage {
        data object Trigger : Encore

        data class Roll(val encoreDie: Int, val isSuccessful: Boolean) : ArenaMessage
    }

    data class Impersonate(
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : ArenaMessage

    sealed interface MirrorImage : ArenaMessage {
        data object Trigger : MirrorImage

        /** The Illusionist let the attacker's roll stand; [outcome] is how that roll resolved. */
        data class Pass(val attackerUserId: String, val outcome: NormalRollMessageType) :
            MirrorImage

        data class Reroll(val firstDie: Int, val secondDie: Int) : MirrorImage

        /**
         * The Illusionist rerolled a die ([rerolledMirrorImageDie]) against [attackerUserId]'s
         * attack; [outcome] is how the modified roll resolved.
         */
        data class TemptFate(
            val rerolledMirrorImageDie: AbilityAction.Passive.MirrorImage.MirrorImageDie,
            val attackerUserId: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage
    }

    data class SpellPotency(
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : ArenaMessage

    sealed interface HolyShield : ArenaMessage {

        data object Trigger : HolyShield

        data class Prayer(val firstDie: Int, val secondDie: Int, val didSurvive: Boolean) :
            HolyShield

        data class Wrath(
            val paladinUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
        ) : HolyShield
    }

    data class AllIn(val gamblerUserId: String, val singleDie: Int) : ArenaMessage

    data class PrepareAttack(
        val duelistUserId: String,
        val targetUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessage
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
