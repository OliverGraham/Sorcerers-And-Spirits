package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase

@Immutable
data class ArenaState(
    val phase: ArenaPhase,
    val playerStatesMap: Map<String, PlayerState>,
    val incomingMessages: List<ArenaMessageState>,
    val messageHistory: List<ArenaMessageState>,
    val secondsRemaining: Int,
    private val userId: String?,
    private val activePlayerUserId: String?,
) {
    val activePlayer = playerStatesMap[activePlayerUserId]
    val userPlayer = playerStatesMap[userId]
    val isItThisUsersTurn = activePlayerUserId == userId

    val incomingMessage = incomingMessages.firstOrNull()
}

internal fun ArenaMatch.toState(
    incomingMessages: List<ArenaMessageState>,
    messageHistory: List<ArenaMessageState>,
    userId: String?,
    secondsRemaining: Int?,
) =
    ArenaState(
        phase = phase,
        playerStatesMap = playerMap.mapValues { it.value.toState() },
        messageHistory = messageHistory,
        incomingMessages = incomingMessages,
        secondsRemaining = secondsRemaining ?: 0,
        activePlayerUserId = activePlayerUserId,
        userId = userId,
    )
