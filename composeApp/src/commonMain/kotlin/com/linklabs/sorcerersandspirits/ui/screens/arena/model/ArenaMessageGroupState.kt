package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ArenaMessageGroupState {
    val messages: List<ArenaMessageState>

    @Immutable
    data class Setup(override val messages: List<ArenaMessageState>) : ArenaMessageGroupState

    @Immutable
    data class Round(val roundNumber: Int, override val messages: List<ArenaMessageState>) :
        ArenaMessageGroupState

    fun withMessages(messages: List<ArenaMessageState>): ArenaMessageGroupState =
        when (this) {
            is Setup -> Setup(messages = messages)
            is Round -> Round(roundNumber = roundNumber, messages = messages)
        }
}

/**
 * Reveals the groups up to [count] messages of the flat, ordered stream, so the log and its handle
 * stay in step with the animation playback: a group only appears once its first message has been
 * revealed, and it only shows the messages revealed so far. Trailing empty groups drop out.
 */
fun List<ArenaMessageGroupState>.revealed(count: Int): List<ArenaMessageGroupState> {
    var consumed = 0
    return mapNotNull { group ->
        val revealedInGroup =
            (count - consumed).coerceIn(minimumValue = 0, maximumValue = group.messages.size)
        consumed += group.messages.size
        if (revealedInGroup == 0) {
            null
        } else {
            group.withMessages(messages = group.messages.take(n = revealedInGroup))
        }
    }
}
