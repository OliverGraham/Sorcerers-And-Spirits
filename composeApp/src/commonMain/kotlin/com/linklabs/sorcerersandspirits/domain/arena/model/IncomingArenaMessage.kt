package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

sealed interface IncomingArenaMessage {
    val id: String

    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfo,
    ) : IncomingArenaMessage

    data class ConnectedToArena(override val id: String, val displayName: String) :
        IncomingArenaMessage

    data class NewRound(override val id: String, val roundNumber: Int) : IncomingArenaMessage

    data class NewTurn(override val id: String, val characterClassInfo: CharacterClassMessageInfo) :
        IncomingArenaMessage

    data class NormalRoll(
        override val id: String,
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : IncomingArenaMessage

    data class Dodge(override val id: String, val didArmorIncrease: Boolean) : IncomingArenaMessage

    sealed interface Teleport : IncomingArenaMessage {
        data class Trigger(override val id: String) : Teleport

        data class Roll(override val id: String, val userId: String, val singleDie: Int) : Teleport

        data class Result(override val id: String, val didDefenderEscape: Boolean) : Teleport
    }

    sealed interface LuckyCoin : IncomingArenaMessage {

        data class Trigger(override val id: String) : LuckyCoin

        data class ArmorClassRoll(override val id: String, val singleDie: Int) : LuckyCoin

        data class ReflectRoll(override val id: String, val singleDie: Int) : LuckyCoin
    }

    sealed interface Riposte : IncomingArenaMessage {
        data class Trigger(override val id: String) : Riposte

        data class EnGarde(override val id: String, val firstDie: Int, val secondDie: Int) : Riposte
    }

    data class SummonMinion(override val id: String, val necroUserId: String, val singleDie: Int) :
        IncomingArenaMessage

    sealed interface Persuade : IncomingArenaMessage {

        data class Trigger(override val id: String) : Persuade

        data class Lull(override val id: String) : Persuade

        data class Charm(override val id: String, val charmedPlayerUserId: String) : Persuade
    }

    data class Encore(override val id: String, val bardUserId: String, val triggered: Boolean) :
        IncomingArenaMessage

    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbility.Impersonate,
    ) : IncomingArenaMessage

    sealed interface MirrorImage : IncomingArenaMessage {
        data class Trigger(override val id: String) : MirrorImage

        data class Pass(override val id: String) : MirrorImage

        data class Reroll(override val id: String, val firstDie: Int, val secondDie: Int) :
            MirrorImage
    }

    data class SpellPotency(
        override val id: String,
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : IncomingArenaMessage

    sealed interface HolyShield : IncomingArenaMessage {

        data class Trigger(override val id: String) : HolyShield

        data class Survival(
            override val id: String,
            val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        data class Reflect(override val id: String, val paladinUserId: String, val singleDie: Int) :
            HolyShield
    }

    data class KillingMomentum(
        override val id: String,
        val rogueUserId: String,
        val isTriggered: Boolean,
    ) : IncomingArenaMessage

    data class AllIn(override val id: String, val gamblerUserId: String, val singleDie: Int) :
        IncomingArenaMessage

    data class PrepareAttack(
        override val id: String,
        val duelistUserId: String,
        val targetUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : IncomingArenaMessage
}

data class CharacterClassMessageInfo(val className: String, val type: CharacterClassType)
