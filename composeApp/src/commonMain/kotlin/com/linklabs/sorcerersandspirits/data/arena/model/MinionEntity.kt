package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.Minion
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionAttributes
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import kotlinx.serialization.Serializable

@Serializable
data class MinionEntity(
    override val type: MinionType,
    override val name: String,
    override val hasAttackedThisRound: Boolean,
    override val hitPoints: Int,
) : MinionAttributes

internal fun Minion.toEntity(): MinionEntity = MinionEntity(
    type = type,
    name = name,
    hasAttackedThisRound = hasAttackedThisRound,
    hitPoints = hitPoints,
)

internal fun MinionEntity.toDomain(): Minion = Minion(
    type = type,
    name = name,
    hasAttackedThisRound = hasAttackedThisRound,
    hitPoints = hitPoints,
)
