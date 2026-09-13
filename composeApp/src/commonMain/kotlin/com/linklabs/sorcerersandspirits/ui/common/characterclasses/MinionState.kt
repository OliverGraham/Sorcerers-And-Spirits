package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.Minion
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionAttributes
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType

@Immutable
data class MinionState(
    override val type: MinionType,
    override val name: String,
    override val hasAttackedThisRound: Boolean,
    override val hitPoints: Int,
) : MinionAttributes

internal fun Minion.toState(): MinionState = MinionState(
    type = type,
    name = name,
    hasAttackedThisRound = hasAttackedThisRound,
    hitPoints = hitPoints,
)

internal fun MinionState.toDomain(): Minion = Minion(
    type = type,
    name = name,
    hasAttackedThisRound = hasAttackedThisRound,
    hitPoints = hitPoints,
)
