package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AnimationType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import kotlinx.coroutines.CompletableDeferred

// TODO:
//       - ArenaMessageState indicates which animation, if any, should play.
//       - It can be either a Flash Message or Avatar animations.
//       - One ArenaMessageState could turn into multiple animations, which should be played at the
//       same time.
//       - One ArenaMessageState means play everything it indicates at once.
//       - Multiple ArenaMessageState can be added at the same time, but each individual
//       ArenaMessageState should play its single or multiple animations before going on to the
//       next ArenaMessageState-derived grouping.

internal class MessageQueue(
    private val addAnimatingMessageToScreenState: (ArenaMessageState) -> Unit
) {
    private val queue = ArrayDeque<ArenaMessageState>()
    private var animatingMessageSignal: CompletableDeferred<Unit>? = null

    // TODO: To be called when new messages come in from Domain layer
    fun enqueue(newMessages: List<ArenaMessageState>) {
        val filteredMessages = newMessages.filterNot { it.animationType == AnimationType.None }

        queue.addAll(elements = filteredMessages)
    }

    // TODO: To be called from UI when dice settle back to DiceHome (after getting update from
    // Domain layer)
    //       OR when an ability is being used, like Impersonate (with no Dice roll). So, the name
    // should
    //       change from onDiceSettled() to something that indicates its an action being completed
    suspend fun onDiceSettled() {
        while (queue.isNotEmpty()) {
            animatingMessageSignal = CompletableDeferred()

            addAnimatingMessageToScreenState(/* arenaMessageState= */ queue.removeFirst())

            animatingMessageSignal?.await()
        }
    }

    // TODO: Called from UI when an animation has finished
    fun onAnimationComplete() {
        animatingMessageSignal?.complete(value = Unit)
        animatingMessageSignal = null
    }
}
