package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import kotlinx.serialization.Serializable

@Serializable
data class ArenaMessageEntity(
    val id: String = "",
    val type: ArenaMessageType = ArenaMessageType.Round,
    val roundNumber: Int? = null,
    val rollerName: String? = null,
    val rollerValue: Int? = null,
    val defenderName: String? = null,
    val rollType: RollType? = null,
    val message: String? = null,
)

@Serializable
enum class ArenaMessageType {
    Round,
    Action,
    Result,
    Generic,
}

fun ArenaMessageEntity.toDomain(): ArenaMessage =
    when (type) {
        ArenaMessageType.Round -> ArenaMessage.Round(id = id, roundNumber = roundNumber ?: 0)

        ArenaMessageType.Action ->
            ArenaMessage.Action(
                id = id,
                rollerName = rollerName.orEmpty(),
                rollerValue = rollerValue ?: 0,
                defenderName = defenderName.orEmpty(),
                rollType = rollType ?: RollType.Ability,
            )

        ArenaMessageType.Result ->
            ArenaMessage.Result(
                id = id,
                rollerName = rollerName.orEmpty(),
                defenderName = defenderName.orEmpty(),
                rollType = rollType ?: RollType.Ability,
            )

        ArenaMessageType.Generic -> ArenaMessage.Generic(id = id, message = message.orEmpty())
    }

fun ArenaMessage.toEntity(): ArenaMessageEntity =
    when (this) {
        is ArenaMessage.Round ->
            ArenaMessageEntity(id = id, type = ArenaMessageType.Round, roundNumber = roundNumber)

        is ArenaMessage.Action ->
            ArenaMessageEntity(
                id = id,
                type = ArenaMessageType.Action,
                rollerName = rollerName,
                rollerValue = rollerValue,
                defenderName = defenderName,
                rollType = rollType,
            )

        is ArenaMessage.Result ->
            ArenaMessageEntity(
                id = id,
                type = ArenaMessageType.Result,
                rollerName = rollerName,
                defenderName = defenderName,
                rollType = rollType,
            )

        is ArenaMessage.Generic ->
            ArenaMessageEntity(id = id, type = ArenaMessageType.Generic, message = message)
    }
