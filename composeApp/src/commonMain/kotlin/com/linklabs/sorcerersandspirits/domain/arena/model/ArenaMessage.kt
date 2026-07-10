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

    data class Doubles(val userId: String) : ArenaMessage

    data class NormalRoll(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val bonus: Int,
        val type: NormalRollMessageType,
    ) : ArenaMessage

    data class KillingMomentum(val attackerUserId: String) : ArenaMessage

    data class Dodge(val didArmorIncrease: Boolean, val dodgerUserId: String) : ArenaMessage

    sealed interface Teleport : ArenaMessage {
        val mageUserId: String

        data class Trigger(override val mageUserId: String) : Teleport

        data class Roll(override val mageUserId: String, val userId: String, val singleDie: Int) :
            Teleport

        data class Result(override val mageUserId: String, val didDefenderEscape: Boolean) :
            Teleport
    }

    sealed interface LuckyCoin : ArenaMessage {

        data object Trigger : LuckyCoin

        data class Flip(val singleDie: Int, val didStopAttack: Boolean) : LuckyCoin

        data class HighRollerTrigger(val targetUserId: String) : LuckyCoin

        data class HighRollerDamage(val singleDie: Int, val isKnockout: Boolean) : LuckyCoin
    }

    sealed interface Riposte : ArenaMessage {
        data class Trigger(val duelistUserId: String, val attackerUserId: String) : Riposte

        data class EnGarde(
            val duelistUserId: String,
            val attackerUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
    }

    data class SummonMinion(val necroUserId: String, val singleDie: Int) : ArenaMessage

    sealed interface Persuade : ArenaMessage {

        data class Trigger(val bardUserId: String) : Persuade

        data class StrumLute(val bardUserId: String, val diceRoll: Int, val isSuccessful: Boolean) :
            Persuade

        data class CharmActivation(val bardUserId: String) : Persuade

        data object Lull : Persuade

        data class Charm(
            val bardUserId: String,
            val attackerUserId: String,
            val defenderUserId: String,
        ) : Persuade
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
        val illusionistUserId: String

        /**
         * Mirror Image interrupted the attacker's roll of [firstDie] and [secondDie] to offer the
         * Illusionist the reroll choice. The dice are carried so every device's resting dice show
         * the roll the reroll labels refer to — the Health roll emits no NormalRoll message before
         * this trigger, so this is the only record of what was rolled (SS-181). Null only on
         * messages persisted before the dice were carried.
         */
        data class Trigger(
            override val illusionistUserId: String,
            val firstDie: Int?,
            val secondDie: Int?,
        ) : MirrorImage

        /** The Illusionist let the attacker's roll stand; [outcome] is how that roll resolved. */
        data class Pass(
            override val illusionistUserId: String,
            val attackerUserId: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage

        /**
         * The Illusionist rerolled a die ([rerolledMirrorImageDie]) against [attackerUserId]'s
         * attack; [outcome] is how the modified roll resolved.
         */
        data class TemptFate(
            override val illusionistUserId: String,
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
