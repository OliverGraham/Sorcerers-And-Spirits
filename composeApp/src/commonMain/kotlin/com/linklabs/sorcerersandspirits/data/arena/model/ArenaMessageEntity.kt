package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMessageEntity {
    val id: String

    @Serializable
    @SerialName(value = CHOSE_CHARACTER_CLASS)
    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoEntity,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = CONNECTED_TO_ARENA)
    data class ConnectedToArena(override val id: String, val displayName: String) :
        ArenaMessageEntity

    @Serializable
    @SerialName(value = NEW_ROUND)
    data class NewRound(override val id: String, val roundNumber: Int) : ArenaMessageEntity

    @Serializable
    @SerialName(value = NEW_TURN)
    data class NewTurn(
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoEntity,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = NORMAL_ROLL)
    data class NormalRoll(
        override val id: String,
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = DODGE)
    data class Dodge(override val id: String, val didArmorIncrease: Boolean) : ArenaMessageEntity

    @Serializable
    @SerialName(value = TELEPORT)
    sealed interface Teleport : ArenaMessageEntity {
        @Serializable
        @SerialName(value = TELEPORT_TRIGGER)
        data class Trigger(override val id: String) : Teleport

        @Serializable
        @SerialName(value = TELEPORT_ROLL)
        data class Roll(override val id: String, val userId: String, val singleDie: Int) : Teleport

        @Serializable
        @SerialName(value = TELEPORT_RESULT)
        data class Result(override val id: String, val didDefenderEscape: Boolean) : Teleport
    }

    @Serializable
    @SerialName(value = LUCKY_COIN)
    sealed interface LuckyCoin : ArenaMessageEntity {
        @Serializable
        @SerialName(value = LUCKY_COIN_TRIGGER)
        data class Trigger(override val id: String) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_ARMOR_CLASS_ROLL)
        data class ArmorClassRoll(override val id: String, val singleDie: Int) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_REFLECT_ROLL)
        data class ReflectRoll(override val id: String, val singleDie: Int) : LuckyCoin
    }

    @Serializable
    @SerialName(value = RIPOSTE)
    sealed interface Riposte : ArenaMessageEntity {
        @Serializable
        @SerialName(value = RIPOSTE_TRIGGER)
        data class Trigger(override val id: String) : Riposte

        @Serializable
        @SerialName(value = RIPOSTE_EN_GARDE)
        data class EnGarde(override val id: String, val firstDie: Int, val secondDie: Int) : Riposte
    }

    @Serializable
    @SerialName(value = SUMMON_MINION)
    data class SummonMinion(override val id: String, val necroUserId: String, val singleDie: Int) :
        ArenaMessageEntity

    @Serializable
    @SerialName(value = PERSUADE)
    sealed interface Persuade : ArenaMessageEntity {
        @Serializable
        @SerialName(value = PERSUADE_TRIGGER)
        data class Trigger(override val id: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_LULL)
        data class Lull(override val id: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_CHARM)
        data class Charm(override val id: String, val charmedPlayerUserId: String) : Persuade
    }

    @Serializable
    @SerialName(value = ENCORE)
    data class Encore(override val id: String, val bardUserId: String, val triggered: Boolean) :
        ArenaMessageEntity

    @Serializable
    @SerialName(value = IMPERSONATE)
    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val impersonateAbility: OffensiveAbilityEntity.Impersonate,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = MIRROR_IMAGE)
    sealed interface MirrorImage : ArenaMessageEntity {
        @Serializable
        @SerialName(value = MIRROR_IMAGE_TRIGGER)
        data class Trigger(override val id: String) : MirrorImage

        @Serializable
        @SerialName(value = MIRROR_IMAGE_PASS)
        data class Pass(override val id: String) : MirrorImage

        @Serializable
        @SerialName(value = MIRROR_IMAGE_REROLL)
        data class Reroll(override val id: String, val firstDie: Int, val secondDie: Int) :
            MirrorImage
    }

    @Serializable
    @SerialName(value = SPELL_POTENCY)
    data class SpellPotency(
        override val id: String,
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = HOLY_SHIELD)
    sealed interface HolyShield : ArenaMessageEntity {
        @Serializable
        @SerialName(value = HOLY_SHIELD_TRIGGER)
        data class Trigger(override val id: String) : HolyShield

        @Serializable
        @SerialName(value = HOLY_SHIELD_SURVIVAL)
        data class Survival(
            override val id: String,
            val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        @Serializable
        @SerialName(value = HOLY_SHIELD_REFLECT)
        data class Reflect(override val id: String, val paladinUserId: String, val singleDie: Int) :
            HolyShield
    }

    @Serializable
    @SerialName(value = KILLING_MOMENTUM)
    data class KillingMomentum(
        override val id: String,
        val rogueUserId: String,
        val isTriggered: Boolean,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = ALL_IN)
    data class AllIn(override val id: String, val gamblerUserId: String, val singleDie: Int) :
        ArenaMessageEntity

    @Serializable
    @SerialName(value = PREPARE_ATTACK)
    data class PrepareAttack(
        override val id: String,
        val duelistUserId: String,
        val targetUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessageEntity

    companion object {
        private const val CHOSE_CHARACTER_CLASS = "ChoseCharacterClass"
        private const val CONNECTED_TO_ARENA = "ConnectedToArena"
        private const val NEW_ROUND = "NewRound"
        private const val NEW_TURN = "NewTurn"
        private const val NORMAL_ROLL = "NormalRoll"
        private const val DODGE = "Dodge"
        private const val TELEPORT = "Teleport"
        private const val TELEPORT_TRIGGER = "TeleportTrigger"
        private const val TELEPORT_ROLL = "TeleportRoll"
        private const val TELEPORT_RESULT = "TeleportResult"
        private const val LUCKY_COIN = "LuckyCoin"
        private const val LUCKY_COIN_TRIGGER = "LuckyCoinTrigger"
        private const val LUCKY_COIN_ARMOR_CLASS_ROLL = "LuckyCoinArmorClassRoll"
        private const val LUCKY_COIN_REFLECT_ROLL = "LuckyCoinReflectRoll"
        private const val RIPOSTE = "Riposte"
        private const val RIPOSTE_TRIGGER = "RiposteTrigger"
        private const val RIPOSTE_EN_GARDE = "RiposteEnGarde"
        private const val SUMMON_MINION = "SummonMinion"
        private const val PERSUADE = "Persuade"
        private const val PERSUADE_TRIGGER = "PersuadeTrigger"
        private const val PERSUADE_LULL = "PersuadeLull"
        private const val PERSUADE_CHARM = "PersuadeCharm"
        private const val ENCORE = "Encore"
        private const val IMPERSONATE = "Impersonate"
        private const val MIRROR_IMAGE = "MirrorImage"
        private const val MIRROR_IMAGE_TRIGGER = "MirrorImageTrigger"
        private const val MIRROR_IMAGE_PASS = "MirrorImagePass"
        private const val MIRROR_IMAGE_REROLL = "MirrorImageReroll"
        private const val SPELL_POTENCY = "SpellPotency"
        private const val HOLY_SHIELD = "HolyShield"
        private const val HOLY_SHIELD_TRIGGER = "HolyShieldTrigger"
        private const val HOLY_SHIELD_SURVIVAL = "HolyShieldSurvival"
        private const val HOLY_SHIELD_REFLECT = "HolyShieldReflect"
        private const val KILLING_MOMENTUM = "KillingMomentum"
        private const val ALL_IN = "AllIn"
        private const val PREPARE_ATTACK = "PrepareAttack"
    }
}

@Serializable
data class CharacterClassMessageInfoEntity(val className: String, val type: CharacterClassType)

fun ArenaMessageEntity.toDomain(): IncomingArenaMessage =
    when (this) {
        is ArenaMessageEntity.ChoseCharacterClass ->
            IncomingArenaMessage.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toDomain(),
            )

        is ArenaMessageEntity.ConnectedToArena ->
            IncomingArenaMessage.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessageEntity.NewRound ->
            IncomingArenaMessage.NewRound(id = id, roundNumber = roundNumber)

        is ArenaMessageEntity.NewTurn ->
            IncomingArenaMessage.NewTurn(
                id = id,
                characterClassInfo = characterClassInfo.toDomain(),
            )

        is ArenaMessageEntity.NormalRoll ->
            IncomingArenaMessage.NormalRoll(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                type = type,
            )

        is ArenaMessageEntity.Dodge ->
            IncomingArenaMessage.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is ArenaMessageEntity.Teleport.Trigger -> IncomingArenaMessage.Teleport.Trigger(id = id)

        is ArenaMessageEntity.Teleport.Roll ->
            IncomingArenaMessage.Teleport.Roll(id = id, userId = userId, singleDie = singleDie)

        is ArenaMessageEntity.Teleport.Result ->
            IncomingArenaMessage.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is ArenaMessageEntity.LuckyCoin.Trigger -> IncomingArenaMessage.LuckyCoin.Trigger(id = id)

        is ArenaMessageEntity.LuckyCoin.ArmorClassRoll ->
            IncomingArenaMessage.LuckyCoin.ArmorClassRoll(id = id, singleDie = singleDie)

        is ArenaMessageEntity.LuckyCoin.ReflectRoll ->
            IncomingArenaMessage.LuckyCoin.ReflectRoll(id = id, singleDie = singleDie)

        is ArenaMessageEntity.Riposte.Trigger -> IncomingArenaMessage.Riposte.Trigger(id = id)

        is ArenaMessageEntity.Riposte.EnGarde ->
            IncomingArenaMessage.Riposte.EnGarde(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessageEntity.SummonMinion ->
            IncomingArenaMessage.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                singleDie = singleDie,
            )

        is ArenaMessageEntity.Persuade.Trigger -> IncomingArenaMessage.Persuade.Trigger(id = id)

        is ArenaMessageEntity.Persuade.Lull -> IncomingArenaMessage.Persuade.Lull(id = id)

        is ArenaMessageEntity.Persuade.Charm ->
            IncomingArenaMessage.Persuade.Charm(id = id, charmedPlayerUserId = charmedPlayerUserId)

        is ArenaMessageEntity.Encore ->
            IncomingArenaMessage.Encore(id = id, bardUserId = bardUserId, triggered = triggered)

        is ArenaMessageEntity.Impersonate ->
            IncomingArenaMessage.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                impersonateAbility = impersonateAbility.toDomain() as OffensiveAbility.Impersonate,
            )

        is ArenaMessageEntity.MirrorImage.Trigger ->
            IncomingArenaMessage.MirrorImage.Trigger(id = id)

        is ArenaMessageEntity.MirrorImage.Pass -> IncomingArenaMessage.MirrorImage.Pass(id = id)

        is ArenaMessageEntity.MirrorImage.Reroll ->
            IncomingArenaMessage.MirrorImage.Reroll(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessageEntity.SpellPotency ->
            IncomingArenaMessage.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mainTargetUserId = mainTargetUserId,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is ArenaMessageEntity.HolyShield.Trigger -> IncomingArenaMessage.HolyShield.Trigger(id = id)

        is ArenaMessageEntity.HolyShield.Survival ->
            IncomingArenaMessage.HolyShield.Survival(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessageEntity.HolyShield.Reflect ->
            IncomingArenaMessage.HolyShield.Reflect(
                id = id,
                paladinUserId = paladinUserId,
                singleDie = singleDie,
            )

        is ArenaMessageEntity.KillingMomentum ->
            IncomingArenaMessage.KillingMomentum(
                id = id,
                rogueUserId = rogueUserId,
                isTriggered = isTriggered,
            )

        is ArenaMessageEntity.AllIn ->
            IncomingArenaMessage.AllIn(
                id = id,
                gamblerUserId = gamblerUserId,
                singleDie = singleDie,
            )

        is ArenaMessageEntity.PrepareAttack ->
            IncomingArenaMessage.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                targetUserId = targetUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )
    }

private fun CharacterClassMessageInfoEntity.toDomain() =
    CharacterClassMessageInfo(className = className, type = type)

internal fun IncomingArenaMessage.toEntity(): ArenaMessageEntity =
    when (this) {
        is IncomingArenaMessage.ChoseCharacterClass ->
            ArenaMessageEntity.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toEntity(),
            )

        is IncomingArenaMessage.ConnectedToArena ->
            ArenaMessageEntity.ConnectedToArena(id = id, displayName = displayName)

        is IncomingArenaMessage.NewRound ->
            ArenaMessageEntity.NewRound(id = id, roundNumber = roundNumber)

        is IncomingArenaMessage.NewTurn ->
            ArenaMessageEntity.NewTurn(id = id, characterClassInfo = characterClassInfo.toEntity())

        is IncomingArenaMessage.NormalRoll ->
            ArenaMessageEntity.NormalRoll(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                type = type,
            )

        is IncomingArenaMessage.Dodge ->
            ArenaMessageEntity.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is IncomingArenaMessage.Teleport.Trigger -> ArenaMessageEntity.Teleport.Trigger(id = id)

        is IncomingArenaMessage.Teleport.Roll ->
            ArenaMessageEntity.Teleport.Roll(id = id, userId = userId, singleDie = singleDie)

        is IncomingArenaMessage.Teleport.Result ->
            ArenaMessageEntity.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is IncomingArenaMessage.LuckyCoin.Trigger -> ArenaMessageEntity.LuckyCoin.Trigger(id = id)

        is IncomingArenaMessage.LuckyCoin.ArmorClassRoll ->
            ArenaMessageEntity.LuckyCoin.ArmorClassRoll(id = id, singleDie = singleDie)

        is IncomingArenaMessage.LuckyCoin.ReflectRoll ->
            ArenaMessageEntity.LuckyCoin.ReflectRoll(id = id, singleDie = singleDie)

        is IncomingArenaMessage.Riposte.Trigger -> ArenaMessageEntity.Riposte.Trigger(id = id)

        is IncomingArenaMessage.Riposte.EnGarde ->
            ArenaMessageEntity.Riposte.EnGarde(id = id, firstDie = firstDie, secondDie = secondDie)

        is IncomingArenaMessage.SummonMinion ->
            ArenaMessageEntity.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                singleDie = singleDie,
            )

        is IncomingArenaMessage.Persuade.Trigger -> ArenaMessageEntity.Persuade.Trigger(id = id)

        is IncomingArenaMessage.Persuade.Lull -> ArenaMessageEntity.Persuade.Lull(id = id)

        is IncomingArenaMessage.Persuade.Charm ->
            ArenaMessageEntity.Persuade.Charm(id = id, charmedPlayerUserId = charmedPlayerUserId)

        is IncomingArenaMessage.Encore ->
            ArenaMessageEntity.Encore(id = id, bardUserId = bardUserId, triggered = triggered)

        is IncomingArenaMessage.Impersonate ->
            ArenaMessageEntity.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                impersonateAbility =
                    impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate,
            )

        is IncomingArenaMessage.MirrorImage.Trigger ->
            ArenaMessageEntity.MirrorImage.Trigger(id = id)

        is IncomingArenaMessage.MirrorImage.Pass -> ArenaMessageEntity.MirrorImage.Pass(id = id)

        is IncomingArenaMessage.MirrorImage.Reroll ->
            ArenaMessageEntity.MirrorImage.Reroll(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is IncomingArenaMessage.SpellPotency ->
            ArenaMessageEntity.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mainTargetUserId = mainTargetUserId,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is IncomingArenaMessage.HolyShield.Trigger -> ArenaMessageEntity.HolyShield.Trigger(id = id)

        is IncomingArenaMessage.HolyShield.Survival ->
            ArenaMessageEntity.HolyShield.Survival(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is IncomingArenaMessage.HolyShield.Reflect ->
            ArenaMessageEntity.HolyShield.Reflect(
                id = id,
                paladinUserId = paladinUserId,
                singleDie = singleDie,
            )

        is IncomingArenaMessage.KillingMomentum ->
            ArenaMessageEntity.KillingMomentum(
                id = id,
                rogueUserId = rogueUserId,
                isTriggered = isTriggered,
            )

        is IncomingArenaMessage.AllIn ->
            ArenaMessageEntity.AllIn(id = id, gamblerUserId = gamblerUserId, singleDie = singleDie)

        is IncomingArenaMessage.PrepareAttack ->
            ArenaMessageEntity.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                targetUserId = targetUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )
    }

internal fun OutgoingArenaMessage.toEntity(id: String): ArenaMessageEntity =
    when (this) {
        is OutgoingArenaMessage.ChoseCharacterClass ->
            ArenaMessageEntity.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toEntity(),
            )

        is OutgoingArenaMessage.ConnectedToArena ->
            ArenaMessageEntity.ConnectedToArena(id = id, displayName = displayName)

        is OutgoingArenaMessage.NewRound ->
            ArenaMessageEntity.NewRound(id = id, roundNumber = roundNumber)

        is OutgoingArenaMessage.NewTurn ->
            ArenaMessageEntity.NewTurn(id = id, characterClassInfo = characterClassInfo.toEntity())

        is OutgoingArenaMessage.NormalRoll ->
            ArenaMessageEntity.NormalRoll(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                type = type,
            )

        is OutgoingArenaMessage.Dodge ->
            ArenaMessageEntity.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is OutgoingArenaMessage.Teleport.Trigger -> ArenaMessageEntity.Teleport.Trigger(id = id)

        is OutgoingArenaMessage.Teleport.Roll ->
            ArenaMessageEntity.Teleport.Roll(id = id, userId = userId, singleDie = singleDie)

        is OutgoingArenaMessage.Teleport.Result ->
            ArenaMessageEntity.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is OutgoingArenaMessage.LuckyCoin.Trigger -> ArenaMessageEntity.LuckyCoin.Trigger(id = id)

        is OutgoingArenaMessage.LuckyCoin.ArmorClassRoll ->
            ArenaMessageEntity.LuckyCoin.ArmorClassRoll(id = id, singleDie = singleDie)

        is OutgoingArenaMessage.LuckyCoin.ReflectRoll ->
            ArenaMessageEntity.LuckyCoin.ReflectRoll(id = id, singleDie = singleDie)

        is OutgoingArenaMessage.Riposte.Trigger -> ArenaMessageEntity.Riposte.Trigger(id = id)

        is OutgoingArenaMessage.Riposte.EnGarde ->
            ArenaMessageEntity.Riposte.EnGarde(id = id, firstDie = firstDie, secondDie = secondDie)

        is OutgoingArenaMessage.SummonMinion ->
            ArenaMessageEntity.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                singleDie = singleDie,
            )

        is OutgoingArenaMessage.Persuade.Trigger -> ArenaMessageEntity.Persuade.Trigger(id = id)

        is OutgoingArenaMessage.Persuade.Lull -> ArenaMessageEntity.Persuade.Lull(id = id)

        is OutgoingArenaMessage.Persuade.Charm ->
            ArenaMessageEntity.Persuade.Charm(id = id, charmedPlayerUserId = charmedPlayerUserId)

        is OutgoingArenaMessage.Encore ->
            ArenaMessageEntity.Encore(id = id, bardUserId = bardUserId, triggered = triggered)

        is OutgoingArenaMessage.Impersonate ->
            ArenaMessageEntity.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                impersonateAbility =
                    impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate,
            )

        is OutgoingArenaMessage.MirrorImage.Trigger ->
            ArenaMessageEntity.MirrorImage.Trigger(id = id)

        is OutgoingArenaMessage.MirrorImage.Pass -> ArenaMessageEntity.MirrorImage.Pass(id = id)

        is OutgoingArenaMessage.MirrorImage.Reroll ->
            ArenaMessageEntity.MirrorImage.Reroll(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is OutgoingArenaMessage.SpellPotency ->
            ArenaMessageEntity.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mainTargetUserId = mainTargetUserId,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is OutgoingArenaMessage.HolyShield.Trigger -> ArenaMessageEntity.HolyShield.Trigger(id = id)

        is OutgoingArenaMessage.HolyShield.Survival ->
            ArenaMessageEntity.HolyShield.Survival(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is OutgoingArenaMessage.HolyShield.Reflect ->
            ArenaMessageEntity.HolyShield.Reflect(
                id = id,
                paladinUserId = paladinUserId,
                singleDie = singleDie,
            )

        is OutgoingArenaMessage.KillingMomentum ->
            ArenaMessageEntity.KillingMomentum(
                id = id,
                rogueUserId = rogueUserId,
                isTriggered = isTriggered,
            )

        is OutgoingArenaMessage.AllIn ->
            ArenaMessageEntity.AllIn(id = id, gamblerUserId = gamblerUserId, singleDie = singleDie)

        is OutgoingArenaMessage.PrepareAttack ->
            ArenaMessageEntity.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                targetUserId = targetUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )
    }

private fun CharacterClassMessageInfo.toEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)
