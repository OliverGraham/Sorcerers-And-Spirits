package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toMessageStates
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll

// TODO: ArenaMatchStateQueue?
//       Takes in new ArenaMatch, plays messages and then the finally the update
internal class MessageQueue(
    private val triggerAnimation: (ArenaMessageState?) -> Unit,
    private val addMessageToScreenState: (ArenaMessageState) -> Unit,
    private val onAnimationsComplete: (ArenaMatch.Combat) -> Unit,
) {
    // TODO: Queue is ArenaMatch updates. Queue the entire object, dequeue based on type
    //       If Combat, do by messages
    //       else, just update immediately
    //       combat -> dequeue messages from ArenaMatch and then send out updated ArenaMatchState
    //                 with messages in correct spot (groups, no animating message), then copy
    //                 over any timer and open dialog or box states, etc.
    private val queue = ArrayDeque<ArenaMatch>()
    private var flashSignal: CompletableDeferred<Unit>? = null
    private var avatarSignal: CompletableDeferred<Unit>? = null

    fun enqueue(arenaMatchUpdate: ArenaMatch) {
        queue.add(element = arenaMatchUpdate)
    }

    // TODO: Happens AFTER first update, which only puts out diceRoll
    suspend fun onCombatActionResolved() {
        // TODO: Figure out if handling other ArenaMatch types is necessary
        val arenaMatchCombatUpdate = queue.removeFirst() as? ArenaMatch.Combat ?: return
        val messages = arenaMatchCombatUpdate.messages.toMessageStates()

        messages.forEach { message ->
            triggerAnimation(/* arenaMessageState= */ message)

            val animationSignals = message.animation.toAnimationSignals()
            animationSignals.awaitAll()

            addMessageToScreenState(/* arenaMessageState= */ message)
        }

        // TODO: Just needs final update parts. What are they? CharacterStates?

        onAnimationsComplete(arenaMatchCombatUpdate)
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
