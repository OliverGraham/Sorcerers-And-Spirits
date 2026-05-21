package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMessageEntity {
    val id: String

    @Serializable
    @SerialName(value = CHOOSE_CHARACTER_CLASS)
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
    @SerialName(value = DICE_ROLL)
    data class DiceRoll(
        override val id: String,
        val rollerClassName: String,
        val rollValue: Int,
        val rollType: RollTypeEntity,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = ATTACK_RESULT)
    data class AttackResult(
        override val id: String,
        val attackerClassName: String,
        val targetClassName: String,
        val isHit: Boolean,
        val rollType: RollTypeEntity,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = TARGET_DEAD)
    data class TargetDead(
        override val id: String,
        val targetClassName: String,
        val killerClassName: String,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = GAME_FINISHED)
    data class GameFinished(override val id: String, val winnerClassName: String) :
        ArenaMessageEntity

    companion object {
        private const val CHOOSE_CHARACTER_CLASS = "ChoseCharacterClass"
        private const val CONNECTED_TO_ARENA = "ConnectedToArena"
        private const val NEW_ROUND = "NewRound"
        private const val NEW_TURN = "NewTurn"
        private const val DICE_ROLL = "DiceRoll"
        private const val ATTACK_RESULT = "AttackResult"
        private const val TARGET_DEAD = "TargetDead"
        private const val GAME_FINISHED = "GameFinished"
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

        is ArenaMessageEntity.DiceRoll ->
            IncomingArenaMessage.DiceRoll(
                id = id,
                rollerClassName = rollerClassName,
                rollValue = rollValue,
                rollType = rollType.toDomain(),
            )

        is ArenaMessageEntity.AttackResult ->
            IncomingArenaMessage.AttackResult(
                id = id,
                attackerClassName = attackerClassName,
                targetClassName = targetClassName,
                isHit = isHit,
                rollType = rollType.toDomain(),
            )

        is ArenaMessageEntity.TargetDead ->
            IncomingArenaMessage.TargetDead(
                id = id,
                targetClassName = targetClassName,
                killerClassName = killerClassName,
            )

        is ArenaMessageEntity.GameFinished ->
            IncomingArenaMessage.GameFinished(id = id, winnerClassName = winnerClassName)
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

        is IncomingArenaMessage.DiceRoll ->
            ArenaMessageEntity.DiceRoll(
                id = id,
                rollerClassName = rollerClassName,
                rollValue = rollValue,
                rollType = rollType.toEntity(),
            )

        is IncomingArenaMessage.AttackResult ->
            ArenaMessageEntity.AttackResult(
                id = id,
                attackerClassName = attackerClassName,
                targetClassName = targetClassName,
                isHit = isHit,
                rollType = rollType.toEntity(),
            )

        is IncomingArenaMessage.TargetDead ->
            ArenaMessageEntity.TargetDead(
                id = id,
                targetClassName = targetClassName,
                killerClassName = killerClassName,
            )

        is IncomingArenaMessage.GameFinished ->
            ArenaMessageEntity.GameFinished(id = id, winnerClassName = winnerClassName)
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

        is OutgoingArenaMessage.DiceRoll ->
            ArenaMessageEntity.DiceRoll(
                id = id,
                rollerClassName = rollerClassName,
                rollValue = rollValue,
                rollType = rollType.toEntity(),
            )

        is OutgoingArenaMessage.AttackResult ->
            ArenaMessageEntity.AttackResult(
                id = id,
                attackerClassName = attackerClassName,
                targetClassName = targetClassName,
                isHit = isHit,
                rollType = rollType.toEntity(),
            )

        is OutgoingArenaMessage.TargetDead ->
            ArenaMessageEntity.TargetDead(
                id = id,
                targetClassName = targetClassName,
                killerClassName = killerClassName,
            )

        is OutgoingArenaMessage.GameFinished ->
            ArenaMessageEntity.GameFinished(id = id, winnerClassName = winnerClassName)
    }

private fun CharacterClassMessageInfo.toEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)
