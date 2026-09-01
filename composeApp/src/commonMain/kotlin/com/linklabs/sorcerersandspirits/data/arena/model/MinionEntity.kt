package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.Minion
import com.linklabs.sorcerersandspirits.domain.game.MinionAttributes
import com.linklabs.sorcerersandspirits.domain.game.MinionType
import kotlinx.serialization.Serializable

@Serializable
data class MinionEntity(
    override val type: MinionType,
    override val hasAttackedThisRound: Boolean,
) : MinionAttributes

internal fun Minion.toEntity(): MinionEntity = MinionEntity(
    type = type,
    hasAttackedThisRound = hasAttackedThisRound,
)

internal fun MinionEntity.toDomain(): Minion = Minion(
    type = type,
    hasAttackedThisRound = hasAttackedThisRound,
)
