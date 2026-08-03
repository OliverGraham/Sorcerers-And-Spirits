package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll

internal class MessageQueue(
    private val onAnimationTriggered: (ArenaMessageState?) -> Unit,
    private val onMessageCommitted: (ArenaMessageState) -> Unit,
    private val onSequenceComplete: () -> Unit,
) {
    private val queue = ArrayDeque<ArenaMessageState>()
    private var flashSignal: CompletableDeferred<Unit>? = null
    private var avatarSignal: CompletableDeferred<Unit>? = null

    fun enqueue(newMessages: List<ArenaMessageState>) {
        queue.addAll(elements = newMessages)
    }

    suspend fun onActionResolved() {
        while (queue.isNotEmpty()) {
            val message = queue.removeFirst()

            onAnimationTriggered(/* arenaMessageState= */ message)

            val animationSignals = message.animation.toAnimationSignals()
            animationSignals.awaitAll()

            onMessageCommitted(/* arenaMessageState= */ message)
        }

        onSequenceComplete()
    }

    fun onFlashComplete() {
        flashSignal?.complete(value = Unit)
        flashSignal = null
    }

    fun onAvatarComplete() {
        avatarSignal?.complete(value = Unit)
        avatarSignal = null
    }

    private fun ArenaAnimationState.toAnimationSignals() = buildList {
        if (isFlash()) {
            val flashSignal = CompletableDeferred<Unit>()
            this@MessageQueue.flashSignal = flashSignal
            add(element = flashSignal)
        }

        if (isAvatar()) {
            val avatarSignal = CompletableDeferred<Unit>()
            this@MessageQueue.avatarSignal = avatarSignal
            add(element = avatarSignal)
        }
    }
}
