package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.Minion
import com.linklabs.sorcerersandspirits.domain.game.MinionAttributes
import com.linklabs.sorcerersandspirits.domain.game.MinionType

@Immutable
data class MinionState(
    override val type: MinionType,
    override val hasAttackedThisRound: Boolean,
) : MinionAttributes

internal fun Minion.toState(): MinionState = MinionState(
    type = type,
    hasAttackedThisRound = hasAttackedThisRound,
)

internal fun MinionState.toDomain(): Minion = Minion(
    type = type,
    hasAttackedThisRound = hasAttackedThisRound,
)
