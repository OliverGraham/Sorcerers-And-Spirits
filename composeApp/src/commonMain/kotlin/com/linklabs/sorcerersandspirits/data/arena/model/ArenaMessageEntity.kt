package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
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
        val rollerDisplayName: String,
        val rollValue: Int,
        val rollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = ATTACK_RESULT)
    data class AttackResult(
        override val id: String,
        val attackerDisplayName: String,
        val targetDisplayName: String,
        val isHit: Boolean,
        val rollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = TARGET_DEAD)
    data class TargetDead(
        override val id: String,
        val targetDisplayName: String,
        val killerDisplayName: String,
    ) : ArenaMessageEntity

    @Serializable
    @SerialName(value = GAME_FINISHED)
    data class GameFinished(override val id: String, val winnerDisplayName: String) :
        ArenaMessageEntity

    companion object {
        private const val ARENA_MESSAGE = "ArenaMessage"
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

fun ArenaMessageEntity.toDomain(): ArenaMessage =
    when (this) {
        is ArenaMessageEntity.ChoseCharacterClass ->
            ArenaMessage.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toDomain(),
            )

        is ArenaMessageEntity.ConnectedToArena ->
            ArenaMessage.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessageEntity.NewRound -> ArenaMessage.NewRound(id = id, roundNumber = roundNumber)

        is ArenaMessageEntity.NewTurn ->
            ArenaMessage.NewTurn(id = id, characterClassInfo = characterClassInfo.toDomain())

        is ArenaMessageEntity.DiceRoll ->
            ArenaMessage.DiceRoll(
                id = id,
                rollerDisplayName = rollerDisplayName,
                rollValue = rollValue,
                rollType = rollType,
            )

        is ArenaMessageEntity.AttackResult ->
            ArenaMessage.AttackResult(
                id = id,
                attackerDisplayName = attackerDisplayName,
                targetDisplayName = targetDisplayName,
                isHit = isHit,
                rollType = rollType,
            )

        is ArenaMessageEntity.TargetDead ->
            ArenaMessage.TargetDead(
                id = id,
                targetDisplayName = targetDisplayName,
                killerDisplayName = killerDisplayName,
            )

        is ArenaMessageEntity.GameFinished ->
            ArenaMessage.GameFinished(id = id, winnerDisplayName = winnerDisplayName)
    }

private fun CharacterClassMessageInfoEntity.toDomain() =
    CharacterClassMessageInfo(className = className, type = type)
