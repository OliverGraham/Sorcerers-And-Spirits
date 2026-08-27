package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import kotlinx.coroutines.CompletableDeferred

internal class MessageAnimationCoordinator(private val message: ArenaMessageState) {
    private val pendingAnimationKeys = message.getExpectedAnimationKeys()
    private val completion = CompletableDeferred<Unit>()

    // TODO: This is called because of the messages that don't animate. Would be better to
    //   determine this at the call site as opposed to wasting resrouces like this
    init {
        if (pendingAnimationKeys.isEmpty()) {
            completion.complete(value = Unit)
        }
    }

    fun markAnimationAsFinishedIfApplicable(arguments: AnimationCompleteArguments) {
        if (arguments.messageId != message.id) return

        val wasAnimationKeyExpected = pendingAnimationKeys.remove(element = arguments.animationKey)
        if (!wasAnimationKeyExpected) return

        if (pendingAnimationKeys.isEmpty()) {
            completion.complete(value = Unit)
        }
    }

    suspend fun await() {
        completion.await()
    }

    private fun ArenaMessageState.getExpectedAnimationKeys() =
        mutableSetOf<AnimationKey>().apply {
            if (isFlash()) {
                add(element = AnimationKey.FlashMessage)
            }

            if (isAvatar()) {
                addAll(
                    elements =
                        gestures.keys.map { userId -> AnimationKey.AvatarGesture(userId = userId) }
                )
                if (!isAvatarCompletedByGesture) {
                    add(element = AnimationKey.AvatarOverlay)
                }
            }
        }
}
