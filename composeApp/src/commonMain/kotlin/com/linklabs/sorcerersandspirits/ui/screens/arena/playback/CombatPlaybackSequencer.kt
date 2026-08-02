package com.linklabs.sorcerersandspirits.ui.screens.arena.playback

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
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

private data class PlaybackSubmission(
    val target: ArenaScreenState,
    val freshMessages: List<ArenaMessageState>,
)

internal class CombatPlaybackSequencer(
    private val scope: CoroutineScope,
    private val emit:
        (
            target: ArenaScreenState, compose: (current: ArenaScreenState) -> ArenaScreenState,
        ) -> Unit,
    private val appendMessage: (ArenaMessageState) -> Unit,
    private val onMatchPlaybackComplete: () -> Unit,
) {
    private val submissions = Channel<PlaybackSubmission>(capacity = Channel.UNLIMITED)
    private val diceSettled = Channel<Unit>(capacity = Channel.CONFLATED)

    private var lastDiceRoll: DiceRollState? = null

    private var currentCue: CombatAnimationCue? = null
    private var currentFlash: FlashCallout? = null
    private var currentFlashAnchor: FlashAnchor = FlashAnchor.Center
    private var collapsingSeatUserId: String? = null

    private var drainJob: Job? = null

    fun start() {
        drainJob =
            scope.launch {
                for (submission in submissions) {
                    try {
                        playThrough(submission = submission)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        recoverFrom(submission = submission)
                    }
                }
            }
    }

    fun submit(target: ArenaScreenState, freshMessages: List<ArenaMessageState>) {
        submissions.trySend(
            element = PlaybackSubmission(target = target, freshMessages = freshMessages)
        )
    }

    fun notifyDiceSettled() {
        diceSettled.trySend(element = Unit)
    }

    fun cancel() {
        drainJob?.cancel()
    }

    private suspend fun playThrough(submission: PlaybackSubmission) {
        val target = submission.target
        val steps = submission.freshMessages.map { it.toPlaybackStep() }

        val targetCombat = target.arenaState as? ArenaState.Combat
        if (targetCombat == null) {
            lastDiceRoll = null
            clearChannel()
            emitFrame(target = target, isPlayingBack = false)
            steps.forEach { appendMessage(it.message) }
            return
        }

        val hasAnimation = steps.any { it.isAnimated }

        val roll = targetCombat.diceRollState
        val isNewRoll = roll != null && roll != lastDiceRoll
        lastDiceRoll = roll

        if (hasAnimation) {
            drainPlayback(
                target = target,
                isMatchOver = targetCombat.isMatchOver,
                steps = steps,
                isNewRoll = isNewRoll,
            )
        } else {
            emitFrame(target = target, isPlayingBack = false)
            steps.forEach { appendMessage(it.message) }
        }

        if (targetCombat.isMatchOver) {
            delay(timeMillis = MATCH_END_TAIL_MILLIS)
            onMatchPlaybackComplete()
        }
    }

    private suspend fun drainPlayback(
        target: ArenaScreenState,
        isMatchOver: Boolean,
        steps: List<CombatPlaybackStep>,
        isNewRoll: Boolean,
    ) {
        emitFrame(target = target, isPlayingBack = true)

        if (isNewRoll) awaitDiceSettle()

        for (step in steps) {
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

            appendMessage(step.message)
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

    private fun recoverFrom(submission: PlaybackSubmission) {
        clearChannel()
        emitFrame(target = submission.target, isPlayingBack = false)
        submission.freshMessages.forEach { appendMessage(it) }
    }

    private fun emitFrame(target: ArenaScreenState, isPlayingBack: Boolean) {
        emit(target) { current ->
            target.copy(
                messages = current.messages,
                messageGroups = current.messageGroups,
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
