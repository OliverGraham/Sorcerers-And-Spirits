package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.combatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.flashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_TOTAL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.duelistNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.rogueNormalAttackDurationMillis
import kotlinx.coroutines.delay

private const val FLASH_HOLD_MILLIS = 850L
private const val FLASH_EXIT_MILLIS = 320L

/** Wall-clock time one queued flash callout occupies the overlay: its hold plus its pop-out. */
internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS

/**
 * The single sequencer for flash-worthy messages: newly-arrived ones play one at a time, so a
 * single resolution narrates as a sequence. Both displays of a playing message read from here — the
 * flash overlay shows its callout while the seated avatars act out its [combatAnimationCue] — so
 * the two can never drift apart (SS-191). The history present on first composition is skipped so
 * reconnecting never replays old events.
 */
@Stable
internal class FlashPlaybackState {
    var currentMessage by mutableStateOf<ArenaMessageState?>(value = null)
        private set

    val visibleState = MutableTransitionState(initialState = false)

    val combatAnimationCue: CombatAnimationCue?
        get() = currentMessage?.combatAnimationCue

    fun show(message: ArenaMessageState) {
        currentMessage = message
        visibleState.targetState = true
    }

    fun dismiss() {
        visibleState.targetState = false
    }
}

@Composable
internal fun rememberFlashPlayback(flashMessages: List<ArenaMessageState>): FlashPlaybackState {
    val playback = remember { FlashPlaybackState() }
    val latestMessages by rememberUpdatedState(newValue = flashMessages)

    LaunchedEffect(key1 = Unit) {
        var lastPlayedId = latestMessages.lastOrNull()?.id
        var lastSize = latestMessages.size
        snapshotFlow { latestMessages }
            .collect { messages ->
                val wasReset = messages.size < lastSize
                if (wasReset) lastPlayedId = null

                val fresh = messages.freshAfter(lastPlayedId = lastPlayedId)
                for (message in fresh) {
                    if (message.flashCallout == null) continue
                    playback.show(message = message)
                    val animationMillis = message.combatAnimationCue?.playbackDurationMillis() ?: 0L
                    delay(timeMillis = FLASH_HOLD_MILLIS)
                    playback.dismiss()
                    delay(timeMillis = FLASH_EXIT_MILLIS)
                    val remainingAnimationMillis = animationMillis - FLASH_MESSAGE_DURATION_MILLIS
                    if (remainingAnimationMillis > 0L) delay(timeMillis = remainingAnimationMillis)
                }
                lastPlayedId = messages.lastOrNull()?.id ?: lastPlayedId
                lastSize = messages.size
            }
    }

    return playback
}

/**
 * How long a cue's seated-avatar animation runs, so the sequencer holds its message until the
 * animation finishes rather than advancing on the fixed flash window and cutting a long strike off
 * (SS-194 / SS-201). A cue whose animation fits inside [FLASH_MESSAGE_DURATION_MILLIS] returns 0
 * and keeps today's flash-window pacing; only longer cues extend their dwell. New long cues opt in
 * here.
 */
internal fun CombatAnimationCue.playbackDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackPlaybackDurationMillis(
                attackerClassType = attackerClassType,
                outcome = outcome,
            )

        is CombatAnimationCue.KillingMomentum -> 0L

        is CombatAnimationCue.Dodge -> 0L

        is CombatAnimationCue.AllIn -> 0L

        is CombatAnimationCue.LuckyCoinFlip -> 0L

        is CombatAnimationCue.LuckyCoinHighRoller -> 0L

        is CombatAnimationCue.LuckyCoinHighRollerThrow -> 0L

        is CombatAnimationCue.PrepareAttack -> PREPARE_ATTACK_TOTAL_MILLIS
    }

/**
 * The attacker-side normal-attack animation length per class: each class that has landed its strike
 * (SS-192…SS-194) reports how long it runs, and any class still on the generic body lunge fits
 * inside the flash window and needs no extension.
 */
private fun normalAttackPlaybackDurationMillis(
    attackerClassType: CharacterClassType,
    outcome: NormalRollMessageType,
): Long =
    when (attackerClassType) {
        CharacterClassType.Rogue -> rogueNormalAttackDurationMillis(outcome = outcome)
        CharacterClassType.Gambler -> gamblerNormalAttackDurationMillis()
        CharacterClassType.Duelist -> duelistNormalAttackDurationMillis()
        else -> 0L
    }

/**
 * The flash messages that arrived after [lastPlayedId], i.e. the ones not yet played. A null
 * [lastPlayedId] means nothing has played, so every message is fresh; an unknown id (e.g. dropped
 * by the log cap) skips the batch rather than replaying the whole history.
 */
private fun List<ArenaMessageState>.freshAfter(lastPlayedId: String?): List<ArenaMessageState> {
    if (lastPlayedId == null) return this

    val lastPlayedIndex = indexOfLast { it.id == lastPlayedId }
    if (lastPlayedIndex == -1) return emptyList()

    return subList(lastPlayedIndex + 1, size)
}
