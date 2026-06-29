package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: never name a serialized property "type" (reserved by the polymorphic serializer).

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
        val bonus: Int = 0,
        val messageType: NormalRollMessageType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = KILLING_MOMENTUM)
    data class KillingMomentum(override val id: String) : ArenaMessageEntity

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
        @SerialName(value = LUCKY_COIN_FLIP)
        data class Flip(override val id: String, val singleDie: Int, val didStopAttack: Boolean) :
            LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_HIGH_ROLLER_TRIGGER)
        data class HighRollerTrigger(override val id: String, val targetUserId: String) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_HIGH_ROLLER_DAMAGE)
        data class HighRollerDamage(
            override val id: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin
    }

    @Serializable
    @SerialName(value = RIPOSTE)
    sealed interface Riposte : ArenaMessageEntity {
        @Serializable
        @SerialName(value = RIPOSTE_TRIGGER)
        data class Trigger(override val id: String) : Riposte

        @Serializable
        @SerialName(value = RIPOSTE_EN_GARDE)
        data class EnGarde(
            override val id: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
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
        @SerialName(value = PERSUADE_STRUM_LUTE)
        data class StrumLute(
            override val id: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_CHARM_ACTIVATION)
        data class CharmActivation(override val id: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_LULL)
        data class Lull(override val id: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_CHARM)
        data class Charm(
            override val id: String,
            val attackerUserId: String,
            val defenderUserId: String,
        ) : Persuade
    }

    @Serializable
    @SerialName(value = ENCORE)
    sealed interface Encore : ArenaMessageEntity {
        @Serializable
        @SerialName(value = ENCORE_TRIGGER)
        data class Trigger(override val id: String) : Encore

        @Serializable
        @SerialName(value = ENCORE_ROLL)
        data class Roll(override val id: String, val encoreDie: Int, val isSuccessful: Boolean) :
            Encore
    }

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
        data class Pass(
            override val id: String,
            val attackerUserId: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage

        @Serializable
        @SerialName(value = MIRROR_IMAGE_TEMPT_FATE)
        data class TemptFate(
            override val id: String,
            val originalRoll: Int,
            val reroll: Int? = null,
            val attackerUserId: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage
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
        @SerialName(value = HOLY_SHIELD_PRAYER)
        data class Prayer(
            override val id: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        @Serializable
        @SerialName(value = HOLY_SHIELD_WRATH)
        data class Wrath(
            override val id: String,
            val paladinUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
        ) : HolyShield
    }

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

    @Serializable
    @SerialName(value = DOUBLES)
    data class Doubles(override val id: String, val userId: String) : ArenaMessageEntity

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
        private const val LUCKY_COIN_FLIP = "LuckyCoinFlip"
        private const val LUCKY_COIN_HIGH_ROLLER_TRIGGER = "LuckyCoinHighRollerTrigger"
        private const val LUCKY_COIN_HIGH_ROLLER_DAMAGE = "LuckyCoinHighRollerDamage"
        private const val RIPOSTE = "Riposte"
        private const val RIPOSTE_TRIGGER = "RiposteTrigger"
        private const val RIPOSTE_EN_GARDE = "RiposteEnGarde"
        private const val SUMMON_MINION = "SummonMinion"
        private const val PERSUADE = "Persuade"
        private const val PERSUADE_TRIGGER = "PersuadeTrigger"
        private const val PERSUADE_STRUM_LUTE = "PersuadeStrumLute"
        private const val PERSUADE_CHARM_ACTIVATION = "PersuadeCharmActivation"
        private const val PERSUADE_LULL = "PersuadeLull"
        private const val PERSUADE_CHARM = "PersuadeCharm"
        private const val ENCORE = "Encore"
        private const val ENCORE_TRIGGER = "EncoreTrigger"
        private const val ENCORE_ROLL = "EncoreRoll"
        private const val IMPERSONATE = "Impersonate"
        private const val MIRROR_IMAGE = "MirrorImage"
        private const val MIRROR_IMAGE_TRIGGER = "MirrorImageTrigger"
        private const val MIRROR_IMAGE_PASS = "MirrorImagePass"
        private const val MIRROR_IMAGE_TEMPT_FATE = "MirrorImageTemptFate"
        private const val SPELL_POTENCY = "SpellPotency"
        private const val HOLY_SHIELD = "HolyShield"
        private const val HOLY_SHIELD_TRIGGER = "HolyShieldTrigger"
        private const val HOLY_SHIELD_PRAYER = "HolyShieldPrayer"
        private const val HOLY_SHIELD_WRATH = "HolyShieldWrath"
        private const val KILLING_MOMENTUM = "KillingMomentum"
        private const val ALL_IN = "AllIn"
        private const val PREPARE_ATTACK = "PrepareAttack"
        private const val DOUBLES = "Doubles"
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
                    bonus = bonus,
                    type = messageType,
                )

            is ArenaMessageEntity.KillingMomentum -> ArenaMessage.KillingMomentum

            is ArenaMessageEntity.Dodge -> ArenaMessage.Dodge(didArmorIncrease = didArmorIncrease)

            is ArenaMessageEntity.Teleport.Trigger -> ArenaMessage.Teleport.Trigger

            is ArenaMessageEntity.Teleport.Roll ->
                ArenaMessage.Teleport.Roll(userId = userId, singleDie = singleDie)

            is ArenaMessageEntity.Teleport.Result ->
                ArenaMessage.Teleport.Result(didDefenderEscape = didDefenderEscape)

            is ArenaMessageEntity.LuckyCoin.Trigger -> ArenaMessage.LuckyCoin.Trigger

            is ArenaMessageEntity.LuckyCoin.Flip ->
                ArenaMessage.LuckyCoin.Flip(singleDie = singleDie, didStopAttack = didStopAttack)

            is ArenaMessageEntity.LuckyCoin.HighRollerTrigger ->
                ArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = targetUserId)

            is ArenaMessageEntity.LuckyCoin.HighRollerDamage ->
                ArenaMessage.LuckyCoin.HighRollerDamage(
                    singleDie = singleDie,
                    isKnockout = isKnockout,
                )

            is ArenaMessageEntity.Riposte.Trigger -> ArenaMessage.Riposte.Trigger

            is ArenaMessageEntity.Riposte.EnGarde ->
                ArenaMessage.Riposte.EnGarde(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageEntity.SummonMinion ->
                ArenaMessage.SummonMinion(necroUserId = necroUserId, singleDie = singleDie)

            is ArenaMessageEntity.Persuade.Trigger -> ArenaMessage.Persuade.Trigger

            is ArenaMessageEntity.Persuade.StrumLute ->
                ArenaMessage.Persuade.StrumLute(diceRoll = diceRoll, isSuccessful = isSuccessful)

            is ArenaMessageEntity.Persuade.CharmActivation -> ArenaMessage.Persuade.CharmActivation

            is ArenaMessageEntity.Persuade.Lull -> ArenaMessage.Persuade.Lull

            is ArenaMessageEntity.Persuade.Charm ->
                ArenaMessage.Persuade.Charm(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                )

            is ArenaMessageEntity.Encore.Trigger -> ArenaMessage.Encore.Trigger

            is ArenaMessageEntity.Encore.Roll ->
                ArenaMessage.Encore.Roll(encoreDie = encoreDie, isSuccessful = isSuccessful)

            is ArenaMessageEntity.Impersonate ->
                ArenaMessage.Impersonate(
                    illusionistUserId = illusionistUserId,
                    impersonateAbility =
                        impersonateAbility.toDomain() as OffensiveAbility.Impersonate,
                )

            is ArenaMessageEntity.MirrorImage.Trigger -> ArenaMessage.MirrorImage.Trigger

            is ArenaMessageEntity.MirrorImage.Pass ->
                ArenaMessage.MirrorImage.Pass(attackerUserId = attackerUserId, outcome = outcome)

            is ArenaMessageEntity.MirrorImage.TemptFate ->
                ArenaMessage.MirrorImage.TemptFate(
                    rerolledMirrorImageDie =
                        AbilityAction.Passive.MirrorImage.MirrorImageDie(
                            originalRoll = originalRoll,
                            reroll = reroll,
                        ),
                    attackerUserId = attackerUserId,
                    outcome = outcome,
                )

            is ArenaMessageEntity.SpellPotency ->
                ArenaMessage.SpellPotency(
                    mageUserId = mageUserId,
                    mainTargetUserId = mainTargetUserId,
                    mainDamage = mainDamage,
                    splashDamage = splashDamage,
                )

            is ArenaMessageEntity.HolyShield.Trigger -> ArenaMessage.HolyShield.Trigger

            is ArenaMessageEntity.HolyShield.Prayer ->
                ArenaMessage.HolyShield.Prayer(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    didSurvive = didSurvive,
                )

            is ArenaMessageEntity.HolyShield.Wrath ->
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = paladinUserId,
                    attackerUserId = attackerUserId,
                    singleDie = singleDie,
                )

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

            is ArenaMessageEntity.Doubles -> ArenaMessage.Doubles(userId = userId)
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
                bonus = bonus,
                messageType = type,
            )

        is ArenaMessage.KillingMomentum -> ArenaMessageEntity.KillingMomentum(id = id)

        is ArenaMessage.Dodge ->
            ArenaMessageEntity.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is ArenaMessage.Teleport.Trigger -> ArenaMessageEntity.Teleport.Trigger(id = id)

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageEntity.Teleport.Roll(id = id, userId = userId, singleDie = singleDie)

        is ArenaMessage.Teleport.Result ->
            ArenaMessageEntity.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is ArenaMessage.LuckyCoin.Trigger -> ArenaMessageEntity.LuckyCoin.Trigger(id = id)

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageEntity.LuckyCoin.Flip(
                id = id,
                singleDie = singleDie,
                didStopAttack = didStopAttack,
            )

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageEntity.LuckyCoin.HighRollerTrigger(id = id, targetUserId = targetUserId)

        is ArenaMessage.LuckyCoin.HighRollerDamage ->
            ArenaMessageEntity.LuckyCoin.HighRollerDamage(
                id = id,
                singleDie = singleDie,
                isKnockout = isKnockout,
            )

        is ArenaMessage.Riposte.Trigger -> ArenaMessageEntity.Riposte.Trigger(id = id)

        is ArenaMessage.Riposte.EnGarde ->
            ArenaMessageEntity.Riposte.EnGarde(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.SummonMinion ->
            ArenaMessageEntity.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                singleDie = singleDie,
            )

        is ArenaMessage.Persuade.Trigger -> ArenaMessageEntity.Persuade.Trigger(id = id)

        is ArenaMessage.Persuade.StrumLute ->
            ArenaMessageEntity.Persuade.StrumLute(
                id = id,
                diceRoll = diceRoll,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Persuade.CharmActivation ->
            ArenaMessageEntity.Persuade.CharmActivation(id = id)

        is ArenaMessage.Persuade.Lull -> ArenaMessageEntity.Persuade.Lull(id = id)

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageEntity.Persuade.Charm(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )

        is ArenaMessage.Encore.Trigger -> ArenaMessageEntity.Encore.Trigger(id = id)

        is ArenaMessage.Encore.Roll ->
            ArenaMessageEntity.Encore.Roll(
                id = id,
                encoreDie = encoreDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Impersonate ->
            ArenaMessageEntity.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                impersonateAbility =
                    impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate,
            )

        is ArenaMessage.MirrorImage.Trigger -> ArenaMessageEntity.MirrorImage.Trigger(id = id)

        is ArenaMessage.MirrorImage.Pass ->
            ArenaMessageEntity.MirrorImage.Pass(
                id = id,
                attackerUserId = attackerUserId,
                outcome = outcome,
            )

        is ArenaMessage.MirrorImage.TemptFate ->
            ArenaMessageEntity.MirrorImage.TemptFate(
                id = id,
                originalRoll = rerolledMirrorImageDie.originalRoll,
                reroll = rerolledMirrorImageDie.reroll,
                attackerUserId = attackerUserId,
                outcome = outcome,
            )

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
            ArenaMessageEntity.HolyShield.Prayer(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessage.HolyShield.Wrath ->
            ArenaMessageEntity.HolyShield.Wrath(
                id = id,
                paladinUserId = paladinUserId,
                attackerUserId = attackerUserId,
                singleDie = singleDie,
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

        is ArenaMessage.Doubles -> ArenaMessageEntity.Doubles(id = id, userId = userId)
    }

private fun CharacterClassMessageInfo.toEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)
