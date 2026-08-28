package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

sealed interface ArenaMessage {

    data class ChoseCharacterClass(
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : ArenaMessage

    data class ConnectedToArena(val displayName: String) : ArenaMessage

    data class NewRound(val roundNumber: Int) : ArenaMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : ArenaMessage

    data class Doubles(val className: String) : ArenaMessage

    sealed interface NormalRoll : ArenaMessage {
        val attackerUserId: String
        val defenderUserId: String

        data class Attack(
            override val attackerUserId: String,
            override val defenderUserId: String,
            val attackerClassType: CharacterClassType,
            val impersonatedCharacterType: CharacterClassType,
            val normalRollMessageType: NormalRollMessageType,
        ) : NormalRoll

        data class Outcome(
            override val attackerUserId: String,
            override val defenderUserId: String,
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val defenderClassName: String,
            val firstDie: Int,
            val secondDie: Int,
            val bonus: Int,
            val type: NormalRollMessageType,
        ) : NormalRoll
    }

    data class KillingMomentum(val attackerUserId: String) : ArenaMessage

    data class Dodge(val didArmorIncrease: Boolean, val dodgerUserId: String) : ArenaMessage

    sealed interface Teleport : ArenaMessage {
        val mageUserId: String

        data class Trigger(override val mageUserId: String) : Teleport

        data class Roll(
            override val mageUserId: String,
            val userId: String,
            val className: String,
            val singleDie: Int,
        ) : Teleport

        data class Tie(override val mageUserId: String) : Teleport

        data class Result(override val mageUserId: String, val didDefenderEscape: Boolean) : Teleport
    }

    sealed interface LuckyCoin : ArenaMessage {
        val gamblerUserId: String

        data class Trigger(override val gamblerUserId: String) : LuckyCoin

        data class Flip(
            override val gamblerUserId: String,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        data class HighRollerTrigger(
            override val gamblerUserId: String,
            val attackerClassName: String,
        ) : LuckyCoin

        data class HighRollerDamage(
            override val gamblerUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin
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

    data class SummonMinion(
        val necromancerUserId: String,
        val className: String,
        val classType: CharacterClassType,
        val singleDie: Int,
    ) : ArenaMessage

    sealed interface Persuade : ArenaMessage {

        data class Trigger(val bardUserId: String) : Persuade

        data class StrumLute(val bardUserId: String, val diceRoll: Int, val isSuccessful: Boolean) : Persuade

        data class CharmActivation(val bardUserId: String) : Persuade

        data class Charm(
            val bardUserId: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val defenderUserId: String,
            val defenderClassName: String,
        ) : Persuade
    }

    sealed interface Encore : ArenaMessage {
        val bardUserId: String
        val classType: CharacterClassType

        data class Trigger(
            override val bardUserId: String,
            override val classType: CharacterClassType,
        ) : Encore

        data class Roll(
            override val bardUserId: String,
            override val classType: CharacterClassType,
            val encoreDie: Int,
            val isSuccessful: Boolean,
        ) : Encore
    }

    data class Impersonate(
        val illusionistUserId: String,
        val illusionistName: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : ArenaMessage

    sealed interface MirrorImage : ArenaMessage {
        val illusionistUserId: String

        data class Trigger(
            override val illusionistUserId: String,
            val firstDie: Int?,
            val secondDie: Int?,
        ) : MirrorImage

        data class Pass(
            override val illusionistUserId: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val messageType: NormalRollMessageType,
            val baseRoll: Int,
            val doublesBonus: Int,
        ) : MirrorImage

        data class TemptFate(
            override val illusionistUserId: String,
            val rerolledMirrorImageDie: AbilityAction.Passive.MirrorImage.MirrorImageDie,
            val attackerUserId: String,
            val attackerClassName: String,
            val messageType: NormalRollMessageType,
        ) : MirrorImage
    }

    data class SpellPotency(
        val mageUserId: String,
        val mageName: String,
        val mainTargetUserId: String,
        val mainTargetClassName: String,
        val firstDie: Int,
        val secondDie: Int,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String>,
        val isMainTargetKnockedOut: Boolean,
    ) : ArenaMessage

    sealed interface HolyShield : ArenaMessage {
        val paladinUserId: String

        data class Trigger(override val paladinUserId: String) : HolyShield

        data class Prayer(
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        data class Wrath(
            override val paladinUserId: String,
            val paladinName: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
        ) : HolyShield
    }

    data class AllIn(
        val gamblerUserId: String,
        val gamblerName: String,
        val classType: CharacterClassType,
        val singleDie: Int,
    ) : ArenaMessage

    sealed interface PrepareAttack : ArenaMessage {
        val duelistUserId: String
        val classType: CharacterClassType
        val isSuccessful: Boolean

        data class Strike(
            override val duelistUserId: String,
            override val classType: CharacterClassType,
            val targetUserId: String,
            override val isSuccessful: Boolean,
            val isTargetKnockedOut: Boolean,
        ) : PrepareAttack

        data class Outcome(
            override val duelistUserId: String,
            override val classType: CharacterClassType,
            val duelistName: String,
            val targetClassName: String,
            val firstDie: Int,
            val secondDie: Int,
            override val isSuccessful: Boolean,
        ) : PrepareAttack
    }
}

// TODO(SS-259): This should be renamed to NormalRollOutcomeType or if there's another type that is
// already
//       doing this then it should be used
enum class NormalRollMessageType {
    ArmorClassHit,
    ArmorClassMiss,
    Damage,
    Knockout,
}
