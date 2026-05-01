package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase

// TODO Arena: ArenaState should be a sealed interface, and then these properties can be
// non-nullable

@Immutable
sealed interface ArenaState1 {
    val messages: List<ArenaMessageState>
    val unhandledMessagesCount: Int

    data class WaitingForAllPlayers(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
    ) : ArenaState1

    data class Active(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
        val phase: ActivePhase,
        val activePlayer: PlayerState,
        val userPlayer: PlayerState,
        val otherPlayers: List<PlayerState>,
        val secondsRemaining: Int,
        val isItThisUsersTurn: Boolean,
    ) : ArenaState1 {
        val latestMessage =
            messages.getOrNull(
                index = messages.size - unhandledMessagesCount.coerceAtLeast(minimumValue = 1)
            )

        val filteredMessages =
            messages.dropLast(n = unhandledMessagesCount.coerceAtLeast(minimumValue = 1))
    }

    data class Finished(
        override val messages: List<ArenaMessageState>,
        override val unhandledMessagesCount: Int,
    ) : ArenaState1
}

@Immutable
enum class ActivePhase {
    ChoosingClasses,
    Combat,
}

@Immutable
data class ArenaState(
    val phase: ArenaPhase,
    val playerStatesMap: Map<String, PlayerState>,
    val messages: List<ArenaMessageState>,
    val unhandledMessagesCount: Int,
    val secondsRemaining: Int,
    private val userId: String?,
    private val activePlayerUserId: String?,
) {
    val activePlayer = playerStatesMap[activePlayerUserId]
    val userPlayer = playerStatesMap[userId]
    val isItThisUsersTurn = activePlayerUserId == userId

    val latestMessage =
        messages.getOrNull(
            index = messages.size - unhandledMessagesCount.coerceAtLeast(minimumValue = 1)
        )

    val filteredMessages =
        messages.dropLast(n = unhandledMessagesCount.coerceAtLeast(minimumValue = 1))
}

internal fun ArenaMatch.toState(
    previousUnhandledMessagesCount: Int,
    userId: String?,
    secondsRemaining: Int?,
) =
    ArenaState(
        phase = phase,
        playerStatesMap = playerMap.mapValues { it.value.toState() },
        messages = messages.map { it.toState() },
        unhandledMessagesCount = previousUnhandledMessagesCount + numberOfNewMessages,
        secondsRemaining = secondsRemaining ?: 0,
        activePlayerUserId = activePlayerUserId,
        userId = userId,
    )
