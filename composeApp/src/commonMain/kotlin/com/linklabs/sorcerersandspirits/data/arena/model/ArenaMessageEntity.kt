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
    @SerialName(value = NORMAL_ROLL_ATTACK)
    data class NormalRollAttack(
        override val id: String,
        val attackerUserId: String,
        val defenderUserId: String,
        val attackerClassType: CharacterClassType,
        val impersonatedCharacterType: CharacterClassType,
        val normalRollMessageType: NormalRollMessageType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = NORMAL_ROLL_OUTCOME)
    data class NormalRollOutcome(
        override val id: String,
        val attackerUserId: String,
        val defenderUserId: String,
        val attackerClassName: String,
        val defenderClassName: String,
        val firstDie: Int,
        val secondDie: Int,
        val bonus: Int = 0,
        val messageType: NormalRollMessageType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = KILLING_MOMENTUM)
    data class KillingMomentum(override val id: String, val attackerUserId: String) :
        ArenaMessageEntity

    @Serializable
    @SerialName(value = DODGE)
    data class Dodge(
        override val id: String,
        val didArmorIncrease: Boolean,
        val dodgerUserId: String,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = TELEPORT)
    sealed interface Teleport : ArenaMessageEntity {
        val mageUserId: String

        @Serializable
        @SerialName(value = TELEPORT_TRIGGER)
        data class Trigger(override val id: String, override val mageUserId: String) : Teleport

        @Serializable
        @SerialName(value = TELEPORT_ROLL)
        data class Roll(
            override val id: String,
            override val mageUserId: String,
            val userId: String,
            val className: String,
            val singleDie: Int,
        ) : Teleport

        @Serializable
        @SerialName(value = TELEPORT_TIE)
        data class Tie(override val id: String, override val mageUserId: String) : Teleport

        @Serializable
        @SerialName(value = TELEPORT_RESULT)
        data class Result(
            override val id: String,
            override val mageUserId: String,
            val didDefenderEscape: Boolean,
        ) : Teleport
    }

    @Serializable
    @SerialName(value = LUCKY_COIN)
    sealed interface LuckyCoin : ArenaMessageEntity {
        val gamblerUserId: String

        @Serializable
        @SerialName(value = LUCKY_COIN_TRIGGER)
        data class Trigger(override val id: String, override val gamblerUserId: String) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_FLIP)
        data class Flip(
            override val id: String,
            override val gamblerUserId: String,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_HIGH_ROLLER_TRIGGER)
        data class HighRollerTrigger(
            override val id: String,
            override val gamblerUserId: String,
            val attackerClassName: String,
        ) : LuckyCoin

        @Serializable
        @SerialName(value = LUCKY_COIN_HIGH_ROLLER_DAMAGE)
        data class HighRollerDamage(
            override val id: String,
            override val gamblerUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin
    }

    @Serializable
    @SerialName(value = RIPOSTE)
    sealed interface Riposte : ArenaMessageEntity {
        @Serializable
        @SerialName(value = RIPOSTE_TRIGGER)
        data class Trigger(
            override val id: String,
            val duelistUserId: String,
            val attackerUserId: String,
        ) : Riposte

        @Serializable
        @SerialName(value = RIPOSTE_EN_GARDE)
        data class EnGarde(
            override val id: String,
            val duelistUserId: String,
            val attackerUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
    }

    @Serializable
    @SerialName(value = SUMMON_MINION)
    data class SummonMinion(
        override val id: String,
        val necromancerUserId: String,
        val className: String,
        val classType: CharacterClassType,
        val singleDie: Int,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = PERSUADE)
    sealed interface Persuade : ArenaMessageEntity {
        @Serializable
        @SerialName(value = PERSUADE_TRIGGER)
        data class Trigger(override val id: String, val bardUserId: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_STRUM_LUTE)
        data class StrumLute(
            override val id: String,
            val bardUserId: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_CHARM_ACTIVATION)
        data class CharmActivation(override val id: String, val bardUserId: String) : Persuade

        @Serializable
        @SerialName(value = PERSUADE_CHARM)
        data class Charm(
            override val id: String,
            val bardUserId: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val defenderUserId: String,
            val defenderClassName: String,
        ) : Persuade
    }

    @Serializable
    @SerialName(value = ENCORE)
    sealed interface Encore : ArenaMessageEntity {
        val bardUserId: String
        val classType: CharacterClassType

        @Serializable
        @SerialName(value = ENCORE_TRIGGER)
        data class Trigger(
            override val id: String,
            override val bardUserId: String,
            override val classType: CharacterClassType,
        ) : Encore

        @Serializable
        @SerialName(value = ENCORE_ROLL)
        data class Roll(
            override val id: String,
            override val bardUserId: String,
            override val classType: CharacterClassType,
            val encoreDie: Int,
            val isSuccessful: Boolean,
        ) : Encore
    }

    @Serializable
    @SerialName(value = IMPERSONATE)
    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val illusionistName: String,
        val impersonateAbility: OffensiveAbilityEntity.Impersonate,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = MIRROR_IMAGE)
    sealed interface MirrorImage : ArenaMessageEntity {
        val illusionistUserId: String

        @Serializable
        @SerialName(value = MIRROR_IMAGE_TRIGGER)
        data class Trigger(
            override val id: String,
            override val illusionistUserId: String,
            val firstDie: Int? = null,
            val secondDie: Int? = null,
        ) : MirrorImage

        @Serializable
        @SerialName(value = MIRROR_IMAGE_PASS)
        data class Pass(
            override val id: String,
            override val illusionistUserId: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val messageType: NormalRollMessageType,
            val baseRoll: Int = 0,
            val doublesBonus: Int = 0,
        ) : MirrorImage

        @Serializable
        @SerialName(value = MIRROR_IMAGE_TEMPT_FATE)
        data class TemptFate(
            override val id: String,
            override val illusionistUserId: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val messageType: NormalRollMessageType,
            val originalRoll: Int,
            val reroll: Int? = null,
        ) : MirrorImage
    }

    @Serializable
    @SerialName(value = SPELL_POTENCY)
    data class SpellPotency(
        override val id: String,
        val mageUserId: String,
        val mageName: String,
        val mainTargetUserId: String,
        val mainTargetClassName: String,
        val firstDie: Int,
        val secondDie: Int,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String> = emptyList(),
        val isMainTargetKnockedOut: Boolean,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = HOLY_SHIELD)
    sealed interface HolyShield : ArenaMessageEntity {
        val paladinUserId: String

        @Serializable
        @SerialName(value = HOLY_SHIELD_TRIGGER)
        data class Trigger(override val id: String, override val paladinUserId: String) : HolyShield

        @Serializable
        @SerialName(value = HOLY_SHIELD_PRAYER)
        data class Prayer(
            override val id: String,
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        @Serializable
        @SerialName(value = HOLY_SHIELD_WRATH)
        data class Wrath(
            override val id: String,
            override val paladinUserId: String,
            val paladinName: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
        ) : HolyShield
    }

    @Serializable
    @SerialName(value = ALL_IN)
    data class AllIn(
        override val id: String,
        val gamblerUserId: String,
        val gamblerName: String,
        val classType: CharacterClassType,
        val singleDie: Int,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = PREPARE_ATTACK)
    data class PrepareAttack(
        override val id: String,
        val duelistUserId: String,
        val duelistName: String,
        val classType: CharacterClassType,
        val targetUserId: String,
        val targetClassName: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = DOUBLES)
    data class Doubles(override val id: String, val className: String) : ArenaMessageEntity

    companion object {
        private const val CHOSE_CHARACTER_CLASS = "ChoseCharacterClass"
        private const val CONNECTED_TO_ARENA = "ConnectedToArena"
        private const val NEW_ROUND = "NewRound"
        private const val NEW_TURN = "NewTurn"
        private const val NORMAL_ROLL_ATTACK = "NormalRollAttack"
        private const val NORMAL_ROLL_OUTCOME = "NormalRollOutcome"
        private const val DODGE = "Dodge"
        private const val TELEPORT = "Teleport"
        private const val TELEPORT_TRIGGER = "TeleportTrigger"
        private const val TELEPORT_ROLL = "TeleportRoll"
        private const val TELEPORT_TIE = "TeleportTie"
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

fun ArenaMessageEntity.toDomain(roundNumber: Int): IncomingArenaMessage {
    val message =
        when (this) {
            is ArenaMessageEntity.ChoseCharacterClass ->
                ArenaMessage.ChoseCharacterClass(
                    displayName = displayName,
                    characterClassInfo = characterClassInfo.toDomain(),
                )

            is ArenaMessageEntity.ConnectedToArena ->
                ArenaMessage.ConnectedToArena(displayName = displayName)

            is ArenaMessageEntity.NewRound -> ArenaMessage.NewRound(roundNumber = this.roundNumber)

            is ArenaMessageEntity.NewTurn ->
                ArenaMessage.NewTurn(characterClassInfo = characterClassInfo.toDomain())

            is ArenaMessageEntity.NormalRollAttack ->
                ArenaMessage.NormalRoll.Attack(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    attackerClassType = attackerClassType,
                    impersonatedCharacterType = impersonatedCharacterType,
                    normalRollMessageType = normalRollMessageType,
                )

            is ArenaMessageEntity.NormalRollOutcome ->
                ArenaMessage.NormalRoll.Outcome(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    attackerClassName = attackerClassName,
                    defenderClassName = defenderClassName,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    bonus = bonus,
                    type = messageType,
                )

            is ArenaMessageEntity.KillingMomentum ->
                ArenaMessage.KillingMomentum(attackerUserId = attackerUserId)

            is ArenaMessageEntity.Dodge ->
                ArenaMessage.Dodge(didArmorIncrease = didArmorIncrease, dodgerUserId = dodgerUserId)

            is ArenaMessageEntity.Teleport.Trigger ->
                ArenaMessage.Teleport.Trigger(mageUserId = mageUserId)

            is ArenaMessageEntity.Teleport.Roll ->
                ArenaMessage.Teleport.Roll(
                    userId = userId,
                    singleDie = singleDie,
                    mageUserId = mageUserId,
                    className = className,
                )

            is ArenaMessageEntity.Teleport.Tie -> ArenaMessage.Teleport.Tie(mageUserId = mageUserId)

            is ArenaMessageEntity.Teleport.Result ->
                ArenaMessage.Teleport.Result(
                    didDefenderEscape = didDefenderEscape,
                    mageUserId = mageUserId,
                )

            is ArenaMessageEntity.LuckyCoin.Trigger ->
                ArenaMessage.LuckyCoin.Trigger(gamblerUserId = gamblerUserId)

            is ArenaMessageEntity.LuckyCoin.Flip ->
                ArenaMessage.LuckyCoin.Flip(
                    gamblerUserId = gamblerUserId,
                    singleDie = singleDie,
                    didStopAttack = didStopAttack,
                )

            is ArenaMessageEntity.LuckyCoin.HighRollerTrigger ->
                ArenaMessage.LuckyCoin.HighRollerTrigger(
                    gamblerUserId = gamblerUserId,
                    attackerClassName = attackerClassName,
                )

            is ArenaMessageEntity.LuckyCoin.HighRollerDamage ->
                ArenaMessage.LuckyCoin.HighRollerDamage(
                    gamblerUserId = gamblerUserId,
                    attackerUserId = attackerUserId,
                    singleDie = singleDie,
                    isKnockout = isKnockout,
                )

            is ArenaMessageEntity.Riposte.Trigger ->
                ArenaMessage.Riposte.Trigger(
                    duelistUserId = duelistUserId,
                    attackerUserId = attackerUserId,
                )

            is ArenaMessageEntity.Riposte.EnGarde ->
                ArenaMessage.Riposte.EnGarde(
                    duelistUserId = duelistUserId,
                    attackerUserId = attackerUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageEntity.SummonMinion ->
                ArenaMessage.SummonMinion(
                    necromancerUserId = necromancerUserId,
                    className = className,
                    classType = classType,
                    singleDie = singleDie,
                )

            is ArenaMessageEntity.Persuade.Trigger ->
                ArenaMessage.Persuade.Trigger(bardUserId = bardUserId)

            is ArenaMessageEntity.Persuade.StrumLute ->
                ArenaMessage.Persuade.StrumLute(
                    bardUserId = bardUserId,
                    diceRoll = diceRoll,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageEntity.Persuade.CharmActivation ->
                ArenaMessage.Persuade.CharmActivation(bardUserId = bardUserId)

            is ArenaMessageEntity.Persuade.Charm ->
                ArenaMessage.Persuade.Charm(
                    bardUserId = bardUserId,
                    attackerUserId = attackerUserId,
                    attackerClassName = attackerClassName,
                    defenderUserId = defenderUserId,
                    defenderClassName = defenderClassName,
                )

            is ArenaMessageEntity.Encore.Trigger ->
                ArenaMessage.Encore.Trigger(bardUserId = bardUserId, classType = classType)

            is ArenaMessageEntity.Encore.Roll ->
                ArenaMessage.Encore.Roll(
                    bardUserId = bardUserId,
                    classType = classType,
                    encoreDie = encoreDie,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageEntity.Impersonate ->
                ArenaMessage.Impersonate(
                    illusionistUserId = illusionistUserId,
                    illusionistName = illusionistName,
                    impersonateAbility =
                        impersonateAbility.toDomain() as OffensiveAbility.Impersonate,
                )

            is ArenaMessageEntity.MirrorImage.Trigger ->
                ArenaMessage.MirrorImage.Trigger(
                    illusionistUserId = illusionistUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                )

            is ArenaMessageEntity.MirrorImage.Pass ->
                ArenaMessage.MirrorImage.Pass(
                    illusionistUserId = illusionistUserId,
                    attackerUserId = attackerUserId,
                    attackerClassName = attackerClassName,
                    attackerClassType = attackerClassType,
                    messageType = messageType,
                    baseRoll = baseRoll,
                    doublesBonus = doublesBonus,
                )

            is ArenaMessageEntity.MirrorImage.TemptFate ->
                ArenaMessage.MirrorImage.TemptFate(
                    illusionistUserId = illusionistUserId,
                    rerolledMirrorImageDie =
                        AbilityAction.Passive.MirrorImage.MirrorImageDie(
                            originalRoll = originalRoll,
                            reroll = reroll,
                        ),
                    attackerUserId = attackerUserId,
                    attackerClassName = attackerClassName,
                    messageType = messageType,
                )

            is ArenaMessageEntity.SpellPotency ->
                ArenaMessage.SpellPotency(
                    mageUserId = mageUserId,
                    mageName = mageName,
                    mainTargetUserId = mainTargetUserId,
                    mainTargetClassName = mainTargetClassName,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    mainDamage = mainDamage,
                    splashDamage = splashDamage,
                    splashTargetUserIds = splashTargetUserIds,
                    isMainTargetKnockedOut = isMainTargetKnockedOut,
                )

            is ArenaMessageEntity.HolyShield.Trigger ->
                ArenaMessage.HolyShield.Trigger(paladinUserId = paladinUserId)

            is ArenaMessageEntity.HolyShield.Prayer ->
                ArenaMessage.HolyShield.Prayer(
                    paladinUserId = paladinUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    didSurvive = didSurvive,
                )

            is ArenaMessageEntity.HolyShield.Wrath ->
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = paladinUserId,
                    paladinName = paladinName,
                    attackerUserId = attackerUserId,
                    attackerClassName = attackerClassName,
                    singleDie = singleDie,
                    isAttackerKnockedOut = isAttackerKnockedOut,
                )

            is ArenaMessageEntity.AllIn ->
                ArenaMessage.AllIn(
                    gamblerUserId = gamblerUserId,
                    gamblerName = gamblerName,
                    classType = classType,
                    singleDie = singleDie,
                )

            is ArenaMessageEntity.PrepareAttack ->
                ArenaMessage.PrepareAttack(
                    duelistUserId = duelistUserId,
                    duelistName = duelistName,
                    classType = classType,
                    targetUserId = targetUserId,
                    targetClassName = targetClassName,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageEntity.Doubles -> ArenaMessage.Doubles(className = className)
        }

    return IncomingArenaMessage(id = id, roundNumber = roundNumber, message = message)
}

/**
 * Maps the flat message entities to domain messages, tagging each with the round it belongs to. The
 * persisted stream already marks its round boundaries with NewRound entities, so a single in-order
 * pass carries the current round onto every message (pre-combat messages stay at round 0).
 */
internal fun List<ArenaMessageEntity>.toDomain(): List<IncomingArenaMessage> {
    var currentRoundNumber = 0
    return map { entity ->
        if (entity is ArenaMessageEntity.NewRound) currentRoundNumber = entity.roundNumber
        entity.toDomain(roundNumber = currentRoundNumber)
    }
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

        is ArenaMessage.NormalRoll.Attack ->
            ArenaMessageEntity.NormalRollAttack(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                attackerClassType = attackerClassType,
                impersonatedCharacterType = impersonatedCharacterType,
                normalRollMessageType = normalRollMessageType,
            )

        is ArenaMessage.NormalRoll.Outcome ->
            ArenaMessageEntity.NormalRollOutcome(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                attackerClassName = attackerClassName,
                defenderClassName = defenderClassName,
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = bonus,
                messageType = type,
            )

        is ArenaMessage.KillingMomentum ->
            ArenaMessageEntity.KillingMomentum(id = id, attackerUserId = attackerUserId)

        is ArenaMessage.Dodge ->
            ArenaMessageEntity.Dodge(
                id = id,
                didArmorIncrease = didArmorIncrease,
                dodgerUserId = dodgerUserId,
            )

        is ArenaMessage.Teleport.Trigger ->
            ArenaMessageEntity.Teleport.Trigger(id = id, mageUserId = mageUserId)

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageEntity.Teleport.Roll(
                id = id,
                userId = userId,
                singleDie = singleDie,
                mageUserId = mageUserId,
                className = className,
            )

        is ArenaMessage.Teleport.Tie ->
            ArenaMessageEntity.Teleport.Tie(id = id, mageUserId = mageUserId)

        is ArenaMessage.Teleport.Result ->
            ArenaMessageEntity.Teleport.Result(
                id = id,
                didDefenderEscape = didDefenderEscape,
                mageUserId = mageUserId,
            )

        is ArenaMessage.LuckyCoin.Trigger ->
            ArenaMessageEntity.LuckyCoin.Trigger(id = id, gamblerUserId = gamblerUserId)

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageEntity.LuckyCoin.Flip(
                id = id,
                gamblerUserId = gamblerUserId,
                singleDie = singleDie,
                didStopAttack = didStopAttack,
            )

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageEntity.LuckyCoin.HighRollerTrigger(
                id = id,
                gamblerUserId = gamblerUserId,
                attackerClassName = attackerClassName,
            )

        is ArenaMessage.LuckyCoin.HighRollerDamage ->
            ArenaMessageEntity.LuckyCoin.HighRollerDamage(
                id = id,
                gamblerUserId = gamblerUserId,
                attackerUserId = attackerUserId,
                singleDie = singleDie,
                isKnockout = isKnockout,
            )

        is ArenaMessage.Riposte.Trigger ->
            ArenaMessageEntity.Riposte.Trigger(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
            )

        is ArenaMessage.Riposte.EnGarde ->
            ArenaMessageEntity.Riposte.EnGarde(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.SummonMinion ->
            ArenaMessageEntity.SummonMinion(
                id = id,
                necromancerUserId = necromancerUserId,
                className = className,
                classType = classType,
                singleDie = singleDie,
            )

        is ArenaMessage.Persuade.Trigger ->
            ArenaMessageEntity.Persuade.Trigger(id = id, bardUserId = bardUserId)

        is ArenaMessage.Persuade.StrumLute ->
            ArenaMessageEntity.Persuade.StrumLute(
                id = id,
                bardUserId = bardUserId,
                diceRoll = diceRoll,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Persuade.CharmActivation ->
            ArenaMessageEntity.Persuade.CharmActivation(id = id, bardUserId = bardUserId)

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageEntity.Persuade.Charm(
                id = id,
                bardUserId = bardUserId,
                attackerUserId = attackerUserId,
                attackerClassName = attackerClassName,
                defenderUserId = defenderUserId,
                defenderClassName = defenderClassName,
            )

        is ArenaMessage.Encore.Trigger ->
            ArenaMessageEntity.Encore.Trigger(
                id = id,
                bardUserId = bardUserId,
                classType = classType,
            )

        is ArenaMessage.Encore.Roll ->
            ArenaMessageEntity.Encore.Roll(
                id = id,
                bardUserId = bardUserId,
                classType = classType,
                encoreDie = encoreDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Impersonate ->
            ArenaMessageEntity.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                illusionistName = illusionistName,
                impersonateAbility =
                    impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate,
            )

        is ArenaMessage.MirrorImage.Trigger ->
            ArenaMessageEntity.MirrorImage.Trigger(
                illusionistUserId = illusionistUserId,
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessage.MirrorImage.Pass ->
            ArenaMessageEntity.MirrorImage.Pass(
                illusionistUserId = illusionistUserId,
                id = id,
                attackerUserId = attackerUserId,
                attackerClassName = attackerClassName,
                attackerClassType = attackerClassType,
                messageType = messageType,
                baseRoll = baseRoll,
                doublesBonus = doublesBonus,
            )

        is ArenaMessage.MirrorImage.TemptFate ->
            ArenaMessageEntity.MirrorImage.TemptFate(
                illusionistUserId = illusionistUserId,
                id = id,
                originalRoll = rerolledMirrorImageDie.originalRoll,
                reroll = rerolledMirrorImageDie.reroll,
                attackerUserId = attackerUserId,
                attackerClassName = attackerClassName,
                messageType = messageType,
            )

        is ArenaMessage.SpellPotency ->
            ArenaMessageEntity.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mageName = mageName,
                mainTargetUserId = mainTargetUserId,
                mainTargetClassName = mainTargetClassName,
                firstDie = firstDie,
                secondDie = secondDie,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
                splashTargetUserIds = splashTargetUserIds,
                isMainTargetKnockedOut = isMainTargetKnockedOut,
            )

        is ArenaMessage.HolyShield.Trigger ->
            ArenaMessageEntity.HolyShield.Trigger(id = id, paladinUserId = paladinUserId)

        is ArenaMessage.HolyShield.Prayer ->
            ArenaMessageEntity.HolyShield.Prayer(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessage.HolyShield.Wrath ->
            ArenaMessageEntity.HolyShield.Wrath(
                id = id,
                paladinUserId = paladinUserId,
                paladinName = paladinName,
                attackerUserId = attackerUserId,
                attackerClassName = attackerClassName,
                singleDie = singleDie,
                isAttackerKnockedOut = isAttackerKnockedOut,
            )

        is ArenaMessage.AllIn ->
            ArenaMessageEntity.AllIn(
                id = id,
                gamblerUserId = gamblerUserId,
                gamblerName = gamblerName,
                classType = classType,
                singleDie = singleDie,
            )

        is ArenaMessage.PrepareAttack ->
            ArenaMessageEntity.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                duelistName = duelistName,
                classType = classType,
                targetUserId = targetUserId,
                targetClassName = targetClassName,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Doubles -> ArenaMessageEntity.Doubles(id = id, className = className)
    }

private fun CharacterClassMessageInfo.toEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)
