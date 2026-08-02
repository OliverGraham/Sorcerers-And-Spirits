package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import kotlinx.coroutines.CompletableDeferred

internal class MessageQueue(
    private val onAnimationTriggered: (ArenaMessageState?) -> Unit,
    private val onMessageCommitted: (ArenaMessageState) -> Unit,
    private val onSequenceComplete: () -> Unit,
) {
    private val queue = ArrayDeque<ArenaMessageState>()
    private var currentAnimationSignal: CompletableDeferred<Unit>? = null

    fun enqueue(newMessages: List<ArenaMessageState>) {
        queue.addAll(elements = newMessages)
    }

    suspend fun onActionResolved() {
        while (queue.isNotEmpty()) {
            val message = queue.removeFirst()

            onAnimationTriggered(/* arenaMessageState= */ message)

            currentAnimationSignal = CompletableDeferred()
            currentAnimationSignal?.await()

            onMessageCommitted(/* arenaMessageState= */ message)
        }
        onSequenceComplete()
    }

    fun onAnimationComplete() {
        currentAnimationSignal?.complete(value = Unit)
        currentAnimationSignal = null
    }
}
