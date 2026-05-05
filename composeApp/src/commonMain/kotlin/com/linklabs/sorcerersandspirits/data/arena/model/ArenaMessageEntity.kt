package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import dev.gitlive.firebase.FirebaseClassDiscriminator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@FirebaseClassDiscriminator(discriminator = ArenaMessageEntity.ARENA_MESSAGE)
sealed interface ArenaMessageEntity {
    val id: String

    @SerialName(value = CHOOSE_CHARACTER_CLASS)
    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoEntity,
    ) : ArenaMessageEntity

    @SerialName(value = CONNECTED_TO_ARENA)
    data class ConnectedToArena(override val id: String, val displayName: String) :
        ArenaMessageEntity

    @SerialName(value = NEW_ROUND)
    data class NewRound(override val id: String, val roundNumber: Int) : ArenaMessageEntity

    @SerialName(value = NEW_TURN)
    data class NewTurn(
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoEntity,
    ) : ArenaMessageEntity

    // TODO Arena: Rolling, ability, etc. messages
    //     Action,
    //     Result,

    companion object {
        private const val ARENA_MESSAGE = "ArenaMessage"
        private const val CHOOSE_CHARACTER_CLASS = "ChoseCharacterClass"
        private const val CONNECTED_TO_ARENA = "ConnectedToArena"
        private const val NEW_ROUND = "NewRound"
        private const val NEW_TURN = "NewTurn"
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
    }

fun ArenaMessage.toEntity(): ArenaMessageEntity =
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
    }

private fun CharacterClassMessageInfoEntity.toDomain() =
    CharacterClassMessageInfo(className = className, type = type)

private fun CharacterClassMessageInfo.toEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)
