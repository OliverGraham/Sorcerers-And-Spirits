package com.linklabs.sorcerersandspirits.ui.screens.arena.playback

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatPlaybackStep
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashAnchor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toPlaybackStep
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.FLASH_EXIT_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.FLASH_HOLD_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.playbackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val DICE_SETTLE_TIMEOUT_MILLIS = 8000L
private const val MATCH_END_TAIL_MILLIS = 2000L

// TODO: Completely delete this. Use MessageQueue

internal class CombatPlaybackSequencer(
    private val scope: CoroutineScope,
    private val emit:
        (
            target: ArenaScreenState, compose: (current: ArenaScreenState) -> ArenaScreenState,
        ) -> Unit,
    private val onMatchPlaybackComplete: () -> Unit,
) {
    private val targets = Channel<ArenaScreenState>(capacity = Channel.UNLIMITED)
    private val diceSettled = Channel<Unit>(capacity = Channel.CONFLATED)

    private var revealedCount = 0
    private var lastDiceRoll: DiceRollState? = null

    private var currentCue: CombatAnimationCue? = null
    private var currentFlash: FlashCallout? = null
    private var currentFlashAnchor: FlashAnchor = FlashAnchor.Center
    private var collapsingSeatUserId: String? = null

    private var drainJob: Job? = null

    fun start() {
        drainJob =
            scope.launch {
                for (target in targets) {
                    try {
                        playThrough(target = target)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        recoverFrom(target = target)
                    }
                }
            }
    }

    fun submit(target: ArenaScreenState) {
        targets.trySend(element = target)
    }

    fun notifyDiceSettled() {
        diceSettled.trySend(element = Unit)
    }

    fun cancel() {
        drainJob?.cancel()
    }

    private suspend fun playThrough(target: ArenaScreenState) {
        val targetCombat = target.arenaState as? ArenaState.Combat
        if (targetCombat == null) {
            revealedCount = target.totalMessageCount
            lastDiceRoll = null
            clearChannel()
            emitFrame(target = target, isPlayingBack = false)
            return
        }

        val newMessageCount =
            target.numberOfNewMessages.coerceIn(
                minimumValue = 0,
                maximumValue = target.totalMessageCount,
            )
        val freshSteps =
            target.messageGroups
                .flatMap { it.messages }
                .takeLast(n = newMessageCount)
                .map { it.toPlaybackStep() }
        val hasAnimation = freshSteps.any { it.isAnimated }

        val roll = targetCombat.diceRollState
        val isNewRoll = roll != null && roll != lastDiceRoll
        lastDiceRoll = roll

        if (hasAnimation) {
            drainPlayback(
                target = target,
                isMatchOver = targetCombat.isMatchOver,
                freshSteps = freshSteps,
                isNewRoll = isNewRoll,
            )
        } else {
            revealedCount = target.totalMessageCount
            emitFrame(target = target, isPlayingBack = false)
        }

        if (targetCombat.isMatchOver) {
            delay(timeMillis = MATCH_END_TAIL_MILLIS)
            onMatchPlaybackComplete()
        }
    }

    private suspend fun drainPlayback(
        target: ArenaScreenState,
        isMatchOver: Boolean,
        freshSteps: List<CombatPlaybackStep>,
        isNewRoll: Boolean,
    ) {
        revealedCount = target.totalMessageCount - freshSteps.size
        emitFrame(target = target, isPlayingBack = true)

        if (isNewRoll) awaitDiceSettle()

        for (step in freshSteps) {
            if (step.isAnimated) {
                if (step.collapsingUserId != null) collapsingSeatUserId = step.collapsingUserId

                currentCue = step.cue
                emitFrame(target = target, isPlayingBack = true)

                val animationMillis = step.cue?.playbackDurationMillis() ?: 0L
                if (animationMillis > 0L) delay(timeMillis = animationMillis)

                if (step.callout != null) {
                    currentFlash = step.callout
                    currentFlashAnchor = step.anchor
                    emitFrame(target = target, isPlayingBack = true)
                    delay(timeMillis = FLASH_HOLD_MILLIS)

                    currentFlash = null
                    emitFrame(target = target, isPlayingBack = true)
                    delay(timeMillis = FLASH_EXIT_MILLIS)
                }

                val shouldReleaseCollapse = step.collapsingUserId != null && !isMatchOver
                if (shouldReleaseCollapse) collapsingSeatUserId = null
            }

            revealedCount++
            emitFrame(target = target, isPlayingBack = true)
        }

        emitFrame(target = target, isPlayingBack = false)
    }

    private suspend fun awaitDiceSettle() {
        withTimeoutOrNull(timeMillis = DICE_SETTLE_TIMEOUT_MILLIS) { diceSettled.receive() }
    }

    private fun clearChannel() {
        currentCue = null
        currentFlash = null
        currentFlashAnchor = FlashAnchor.Center
        collapsingSeatUserId = null
    }

    private fun recoverFrom(target: ArenaScreenState) {
        revealedCount = target.totalMessageCount
        clearChannel()
        emitFrame(target = target, isPlayingBack = false)
    }

    private fun emitFrame(target: ArenaScreenState, isPlayingBack: Boolean) {
        emit(target) { current ->
            target.copy(
                revealedMessageCount = revealedCount,
                isPlayingBack = isPlayingBack,
                currentCue = currentCue,
                currentFlash = currentFlash,
                currentFlashAnchor = currentFlashAnchor,
                collapsingSeatUserId = collapsingSeatUserId,
                secondsRemaining = current.secondsRemaining,
                showForfeitDialog = current.showForfeitDialog,
            )
        }
    }
}
