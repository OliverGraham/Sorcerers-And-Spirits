package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: Add SerialNames everywhere. Also, make sure to never name a property as "type"

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
        val messageType: NormalRollMessageType,
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

        @Serializable
        data class HighRollerTrigger(override val id: String, val targetUserId: String) : LuckyCoin
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

fun ArenaMessageEntity.toDomain(): IncomingArenaMessage {
    val message =
        when (this) {
            is ArenaMessageEntity.ChoseCharacterClass ->
                ArenaMessage.ChoseCharacterClass(
                    displayName = displayName,
                    characterClassInfo = characterClassInfo.toDomain(),
                )

            is ArenaMessageEntity.ConnectedToArena ->
                ArenaMessage.ConnectedToArena(displayName = displayName)

            is ArenaMessageEntity.NewRound -> ArenaMessage.NewRound(roundNumber = roundNumber)

            is ArenaMessageEntity.NewTurn ->
                ArenaMessage.NewTurn(characterClassInfo = characterClassInfo.toDomain())

            is ArenaMessageEntity.NormalRoll ->
                ArenaMessage.NormalRoll(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = messageType,
                )

            is ArenaMessageEntity.Dodge -> ArenaMessage.Dodge(didArmorIncrease = didArmorIncrease)

            is ArenaMessageEntity.Teleport.Trigger -> ArenaMessage.Teleport.Trigger

            is ArenaMessageEntity.Teleport.Roll ->
                ArenaMessage.Teleport.Roll(userId = userId, singleDie = singleDie)

            is ArenaMessageEntity.Teleport.Result ->
                ArenaMessage.Teleport.Result(didDefenderEscape = didDefenderEscape)

            is ArenaMessageEntity.LuckyCoin.Trigger -> ArenaMessage.LuckyCoin.Trigger

            is ArenaMessageEntity.LuckyCoin.ArmorClassRoll ->
                ArenaMessage.LuckyCoin.Flip(singleDie = singleDie, didStopAttack = false)

            is ArenaMessageEntity.LuckyCoin.ReflectRoll ->
                ArenaMessage.LuckyCoin.HighRollerDamage(singleDie = singleDie, isKnockout = false)

            is ArenaMessageEntity.LuckyCoin.HighRollerTrigger ->
                ArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = targetUserId)

            is ArenaMessageEntity.Riposte.Trigger -> ArenaMessage.Riposte.Trigger

            is ArenaMessageEntity.Riposte.EnGarde ->
                ArenaMessage.Riposte.EnGarde(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = false, // TODO
                )

            is ArenaMessageEntity.SummonMinion ->
                ArenaMessage.SummonMinion(necroUserId = necroUserId, singleDie = singleDie)

            is ArenaMessageEntity.Persuade.Trigger -> ArenaMessage.Persuade.Trigger

            is ArenaMessageEntity.Persuade.Lull -> ArenaMessage.Persuade.Lull

            is ArenaMessageEntity.Persuade.Charm ->
                ArenaMessage.Persuade.Charm(
                    attackerUserId = "", // TODO
                    defenderUserId = charmedPlayerUserId,
                )

            is ArenaMessageEntity.Encore ->
                ArenaMessage.Encore.Roll(encoreDie = 0, isSuccessful = triggered)

            // TODO

            is ArenaMessageEntity.Impersonate ->
                ArenaMessage.Impersonate(
                    illusionistUserId = illusionistUserId,
                    impersonateAbility =
                        impersonateAbility.toDomain() as OffensiveAbility.Impersonate,
                )

            is ArenaMessageEntity.MirrorImage.Trigger -> ArenaMessage.MirrorImage.Trigger

            is ArenaMessageEntity.MirrorImage.Pass -> ArenaMessage.MirrorImage.Pass

            is ArenaMessageEntity.MirrorImage.Reroll ->
                ArenaMessage.MirrorImage.Reroll(firstDie = firstDie, secondDie = secondDie)

            is ArenaMessageEntity.SpellPotency ->
                ArenaMessage.SpellPotency(
                    mageUserId = mageUserId,
                    mainTargetUserId = mainTargetUserId,
                    mainDamage = mainDamage,
                    splashDamage = splashDamage,
                )

            is ArenaMessageEntity.HolyShield.Trigger -> ArenaMessage.HolyShield.Trigger

            is ArenaMessageEntity.HolyShield.Survival ->
                ArenaMessage.HolyShield.Prayer(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    didSurvive = didSurvive,
                )

            is ArenaMessageEntity.HolyShield.Reflect ->
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = paladinUserId,
                    attackerUserId = "", // TODO
                    singleDie = singleDie,
                )

            is ArenaMessageEntity.KillingMomentum -> ArenaMessage.KillingMomentum

            is ArenaMessageEntity.AllIn ->
                ArenaMessage.AllIn(gamblerUserId = gamblerUserId, singleDie = singleDie)

            is ArenaMessageEntity.PrepareAttack ->
                ArenaMessage.PrepareAttack(
                    duelistUserId = duelistUserId,
                    targetUserId = targetUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )
        }

    return IncomingArenaMessage(id = id, message = message)
}

private fun CharacterClassMessageInfoEntity.toDomain() =
    CharacterClassMessageInfo(className = className, type = type)

internal fun ArenaMessage.toEntity(id: String): ArenaMessageEntity =
    when (this) {
        is ArenaMessage.ChoseCharacterClass ->
            ArenaMessageEntity.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toEntity(),
            )

        is ArenaMessage.ConnectedToArena ->
            ArenaMessageEntity.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessage.NewRound -> ArenaMessageEntity.NewRound(id = id, roundNumber = roundNumber)

        is ArenaMessage.NewTurn ->
            ArenaMessageEntity.NewTurn(id = id, characterClassInfo = characterClassInfo.toEntity())

        is ArenaMessage.NormalRoll ->
            ArenaMessageEntity.NormalRoll(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                messageType = type,
            )

        is ArenaMessage.Dodge ->
            ArenaMessageEntity.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is ArenaMessage.Teleport.Trigger -> ArenaMessageEntity.Teleport.Trigger(id = id)

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageEntity.Teleport.Roll(id = id, userId = userId, singleDie = singleDie)

        is ArenaMessage.Teleport.Result ->
            ArenaMessageEntity.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is ArenaMessage.LuckyCoin.Trigger -> ArenaMessageEntity.LuckyCoin.Trigger(id = id)

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageEntity.LuckyCoin.ArmorClassRoll(id = id, singleDie = singleDie)

        is ArenaMessage.LuckyCoin.HighRollerDamage ->
            ArenaMessageEntity.LuckyCoin.ReflectRoll(id = id, singleDie = singleDie)

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageEntity.LuckyCoin.HighRollerTrigger(id = id, targetUserId = targetUserId)

        is ArenaMessage.Riposte.Trigger -> ArenaMessageEntity.Riposte.Trigger(id = id)

        is ArenaMessage.Riposte.EnGarde ->
            ArenaMessageEntity.Riposte.EnGarde(id = id, firstDie = firstDie, secondDie = secondDie)

        is ArenaMessage.SummonMinion ->
            ArenaMessageEntity.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                singleDie = singleDie,
            )

        is ArenaMessage.Persuade.Trigger -> ArenaMessageEntity.Persuade.Trigger(id = id)

        is ArenaMessage.Persuade.Lull -> ArenaMessageEntity.Persuade.Lull(id = id)

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageEntity.Persuade.Charm(id = id, charmedPlayerUserId = defenderUserId)

        is ArenaMessage.Persuade.CharmActivation -> ArenaMessageEntity.Persuade.Trigger(id = id)

        // TODO

        is ArenaMessage.Persuade.StrumLute -> ArenaMessageEntity.Persuade.Trigger(id = id)

        // TODO

        is ArenaMessage.Encore.Trigger ->
            ArenaMessageEntity.Encore(id = id, bardUserId = "", triggered = false)

        // TODO

        is ArenaMessage.Encore.Roll ->
            ArenaMessageEntity.Encore(id = id, bardUserId = "", triggered = isSuccessful)

        // TODO

        is ArenaMessage.Impersonate ->
            ArenaMessageEntity.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                impersonateAbility =
                    impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate,
            )

        is ArenaMessage.MirrorImage.Trigger -> ArenaMessageEntity.MirrorImage.Trigger(id = id)

        is ArenaMessage.MirrorImage.Pass -> ArenaMessageEntity.MirrorImage.Pass(id = id)

        is ArenaMessage.MirrorImage.Reroll ->
            ArenaMessageEntity.MirrorImage.Reroll(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessage.MirrorImage.TemptFate -> ArenaMessageEntity.MirrorImage.Trigger(id = id)

        // TODO

        is ArenaMessage.SpellPotency ->
            ArenaMessageEntity.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mainTargetUserId = mainTargetUserId,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is ArenaMessage.HolyShield.Trigger -> ArenaMessageEntity.HolyShield.Trigger(id = id)

        is ArenaMessage.HolyShield.Prayer ->
            ArenaMessageEntity.HolyShield.Survival(
                id = id,
                paladinUserId = "", // TODO
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessage.HolyShield.Wrath ->
            ArenaMessageEntity.HolyShield.Reflect(
                id = id,
                paladinUserId = paladinUserId,
                singleDie = singleDie,
            )

        is ArenaMessage.KillingMomentum ->
            ArenaMessageEntity.KillingMomentum(
                id = id,
                rogueUserId = "", // TODO
                isTriggered = true, // TODO
            )

        is ArenaMessage.AllIn ->
            ArenaMessageEntity.AllIn(id = id, gamblerUserId = gamblerUserId, singleDie = singleDie)

        is ArenaMessage.PrepareAttack ->
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
