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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.collapsingUserId
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.combatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.flashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.weaponClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.bardNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.encoreDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.persuadeDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_TOTAL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_READY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_TOTAL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.duelistNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerAllInDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinFlipDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinHighRollerDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinThrowDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.illusionistImpersonateDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerSummonMinionDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.rogueNormalAttackDurationMillis
import kotlinx.coroutines.delay

private const val FLASH_HOLD_MILLIS = 850L
private const val FLASH_EXIT_MILLIS = 320L

/** Wall-clock time one queued flash callout occupies the overlay: its hold plus its pop-out. */
internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS

/**
 * The single sequencer for flash-worthy messages: newly-arrived ones play one at a time, so a
 * single resolution narrates as a sequence. A message's seated-avatar [combatAnimationCue] plays to
 * completion first, then its flash callout appears to spell out what the roll and animation meant
 * (SS-201), so an effect is never flashed over before it finishes. The cue is held separately from
 * [currentMessage] so it can go live ahead of the callout. The history present on first composition
 * is skipped so reconnecting never replays old events.
 */
@Stable
internal class FlashPlaybackState {
    var currentMessage by mutableStateOf<ArenaMessageState?>(value = null)
        private set

    var combatAnimationCue by mutableStateOf<CombatAnimationCue?>(value = null)
        private set

    /**
     * The id of the most recent collapse message whose playback window has fully closed, so the
     * seating can release the downed player from their seat once their Collapse has played and let
     * them morph to a head icon in a match that continues (SS-213).
     */
    var finishedCollapseMessageId by mutableStateOf<String?>(value = null)
        private set

    val visibleState = MutableTransitionState(initialState = false)

    /**
     * Send a message's seated-avatar cue live; its flash callout follows once the cue plays out.
     */
    fun playAnimation(cue: CombatAnimationCue?) {
        combatAnimationCue = cue
    }

    /** Marks a collapse message's playback window closed, releasing its downed player's seat. */
    fun markCollapseFinished(messageId: String) {
        finishedCollapseMessageId = messageId
    }

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
                    val cue = message.combatAnimationCue
                    playback.playAnimation(cue = cue)
                    val animationMillis = cue?.playbackDurationMillis() ?: 0L
                    if (animationMillis > 0L) delay(timeMillis = animationMillis)
                    playback.show(message = message)
                    delay(timeMillis = FLASH_HOLD_MILLIS)
                    playback.dismiss()
                    delay(timeMillis = FLASH_EXIT_MILLIS)
                    if (cue?.collapsingUserId != null) {
                        playback.markCollapseFinished(messageId = message.id)
                    }
                }
                lastPlayedId = messages.lastOrNull()?.id ?: lastPlayedId
                lastSize = messages.size
            }
    }

    return playback
}

/**
 * How long a cue's whole animation runs — its weapon and effect overlays and its seated-avatar
 * gestures, whichever ends last — so the sequencer can play the animation to completion before the
 * flash callout lands (SS-201). Every cue reports its real length; a cue with no animation returns
 * 0 and the callout shows at once. New cues report their length here.
 */
internal fun CombatAnimationCue.playbackDurationMillis(): Long =
    maxOf(effectOverlayDurationMillis(), seatedAvatarDurationMillis())

/**
 * The length of a cue's weapon and effect overlays, apart from the seated avatars' own gestures.
 */
private fun CombatAnimationCue.effectOverlayDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackOverlayDurationMillis(weaponClassType = weaponClassType, outcome = outcome)

        is CombatAnimationCue.KillingMomentum -> 0L

        is CombatAnimationCue.Dodge -> 0L

        is CombatAnimationCue.AllIn -> gamblerAllInDurationMillis(outcome = outcome)

        is CombatAnimationCue.LuckyCoinFlip ->
            gamblerLuckyCoinFlipDurationMillis(didStopAttack = didStopAttack)

        is CombatAnimationCue.LuckyCoinHighRoller -> gamblerLuckyCoinHighRollerDurationMillis()

        is CombatAnimationCue.LuckyCoinHighRollerThrow -> gamblerLuckyCoinThrowDurationMillis()

        is CombatAnimationCue.PrepareAttack -> PREPARE_ATTACK_TOTAL_MILLIS

        is CombatAnimationCue.Riposte ->
            when (phase) {
                RipostePhase.Ready -> RIPOSTE_READY_MILLIS

                RipostePhase.Landed,
                RipostePhase.Missed -> RIPOSTE_TOTAL_MILLIS
            }

        is CombatAnimationCue.SummonMinion ->
            necromancerSummonMinionDurationMillis(outcome = outcome)

        is CombatAnimationCue.Persuade -> persuadeDurationMillis(phase = phase)

        is CombatAnimationCue.Encore -> encoreDurationMillis(phase = phase)

        is CombatAnimationCue.Impersonate -> illusionistImpersonateDurationMillis()

        is CombatAnimationCue.MirrorImage -> 0L
    }

/**
 * The attacker-side normal-attack overlay length per weapon: each class that has landed its weapon
 * (SS-192…SS-196) reports how long its strike runs, while a class still on the generic body lunge
 * has no weapon overlay, so its length comes from the seated avatars alone. Keyed on the borrowed
 * weapon, not the raw attacker, so an Illusionist's strike runs as long as the weapon it copied.
 */
private fun normalAttackOverlayDurationMillis(
    weaponClassType: CharacterClassType,
    outcome: NormalRollMessageType,
): Long =
    when (weaponClassType) {
        CharacterClassType.Rogue -> rogueNormalAttackDurationMillis(outcome = outcome)
        CharacterClassType.Gambler -> gamblerNormalAttackDurationMillis()
        CharacterClassType.Duelist -> duelistNormalAttackDurationMillis()
        CharacterClassType.Necromancer -> necromancerNormalAttackDurationMillis()
        CharacterClassType.Bard -> bardNormalAttackDurationMillis()
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
