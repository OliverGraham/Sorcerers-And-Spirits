package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase

@Immutable
data class ArenaState(
    val phase: ArenaPhase,
    val playerStatesMap: Map<String, PlayerState>,
    private val activePlayerUserId: String?,
) {
    val activePlayer = playerStatesMap[activePlayerUserId]
}

internal fun ArenaMatch.toState() =
    ArenaState(
        phase = phase,
        playerStatesMap = playerMap.mapValues { it.value.toState() },
        activePlayerUserId = activePlayerUserId,
    )
