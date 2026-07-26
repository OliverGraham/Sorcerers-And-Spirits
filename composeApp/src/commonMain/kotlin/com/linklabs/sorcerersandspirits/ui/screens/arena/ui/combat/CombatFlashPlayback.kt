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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashAnchor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayableMessage
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.mageNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.mageSpellPotencyDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.teleportOverlayDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerSummonMinionDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.paladinHolyShieldOverlayDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.paladinNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.rogueNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val FLASH_HOLD_MILLIS = 1100L
private const val FLASH_EXIT_MILLIS = 320L

internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS

@Stable
internal class FlashPlaybackState(
    initialPlayedMessageCount: Int,
    initialPlayedAnimationCount: Int,
) {
    var currentMessage by mutableStateOf<ArenaMessageState?>(value = null)
        private set

    var currentCallout by mutableStateOf<FlashCallout?>(value = null)
        private set

    var currentAnchor by mutableStateOf<FlashAnchor>(value = FlashAnchor.Center)
        private set

    var combatAnimationCue by mutableStateOf<CombatAnimationCue?>(value = null)
        private set

    var collapsingSeatUserId by mutableStateOf<String?>(value = null)
        private set

    var isActing by mutableStateOf(value = false)
        private set

    var playedMessageCount by mutableStateOf(value = initialPlayedMessageCount)
        private set

    var playedAnimationCount by mutableStateOf(value = initialPlayedAnimationCount)
        private set

    val visibleState = MutableTransitionState(initialState = false)

    fun markActing() {
        isActing = true
    }

    fun clearActing() {
        isActing = false
    }

    fun resetPlayback() {
        playedMessageCount = 0
        playedAnimationCount = 0
        collapsingSeatUserId = null
    }

    fun advancePlayedAnimation() {
        playedAnimationCount++
    }

    fun revealMessage() {
        playedMessageCount++
    }

    fun isAnimating(animationCount: Int): Boolean =
        isActing || playedAnimationCount < animationCount

    fun playAnimation(cue: CombatAnimationCue?) {
        combatAnimationCue = cue
    }

    fun beginCollapse(userId: String) {
        collapsingSeatUserId = userId
    }

    fun endCollapse() {
        collapsingSeatUserId = null
    }

    fun show(message: ArenaMessageState, callout: FlashCallout, anchor: FlashAnchor) {
        currentMessage = message
        currentCallout = callout
        currentAnchor = anchor
        visibleState.targetState = true
    }

    fun dismiss() {
        visibleState.targetState = false
    }
}

@Composable
internal fun rememberFlashPlayback(
    messages: List<PlayableMessage>,
    diceRollState: DiceRollState?,
    diceBadgeState: DiceBadgeState?,
    isBadgeBonusSpent: Boolean,
    isMatchEnding: Boolean,
    diceStateHolder: DiceStateHolder,
): FlashPlaybackState {
    val playback = remember {
        FlashPlaybackState(
            initialPlayedMessageCount = messages.size,
            initialPlayedAnimationCount = messages.count { it.cue != null || it.callout != null },
        )
    }
    val latestMessages by rememberUpdatedState(newValue = messages)
    val latestDiceRoll by rememberUpdatedState(newValue = diceRollState)
    val latestDiceBadge by rememberUpdatedState(newValue = diceBadgeState)
    val latestIsBadgeBonusSpent by rememberUpdatedState(newValue = isBadgeBonusSpent)
    val latestIsMatchEnding by rememberUpdatedState(newValue = isMatchEnding)

    LaunchedEffect(key1 = Unit) {
        var lastRoll = latestDiceRoll
        snapshotFlow { latestMessages }
            .collect { messages ->
                playback.clearActing()

                val wasReset = messages.size < playback.playedMessageCount
                if (wasReset) playback.resetPlayback()

                val fresh = messages.drop(n = playback.playedMessageCount)

                val roll = latestDiceRoll
                val isNewRoll = roll != null && roll != lastRoll
                lastRoll = roll

                if (isNewRoll) {
                    diceStateHolder.commitRoll(
                        diceRollState = roll,
                        diceBadge = latestDiceBadge,
                        isBadgeBonusSpent = latestIsBadgeBonusSpent,
                    )
                    snapshotFlow { diceStateHolder.isSettled }.first { it }
                }

                for (playable in fresh) {
                    val cue = playable.cue
                    val callout = playable.callout
                    val hasAnimation = cue != null || callout != null

                    if (hasAnimation) {
                        val collapsingUserId = playable.collapsingUserId
                        if (collapsingUserId != null) {
                            playback.beginCollapse(userId = collapsingUserId)
                        }

                        playback.playAnimation(cue = cue)
                        val animationMillis = cue?.playbackDurationMillis() ?: 0L
                        if (animationMillis > 0L) delay(timeMillis = animationMillis)

                        if (callout != null) {
                            playback.show(
                                message = playable.message,
                                callout = callout,
                                anchor = playable.anchor,
                            )
                            delay(timeMillis = FLASH_HOLD_MILLIS)
                            playback.dismiss()
                            delay(timeMillis = FLASH_EXIT_MILLIS)
                        }

                        val shouldReleaseCollapsedSeat =
                            collapsingUserId != null && !latestIsMatchEnding
                        if (shouldReleaseCollapsedSeat) playback.endCollapse()

                        playback.advancePlayedAnimation()
                    }

                    playback.revealMessage()
                }
            }
    }

    return playback
}

internal fun CombatAnimationCue.playbackDurationMillis(): Long =
    maxOf(a = effectOverlayDurationMillis(), b = seatedAvatarDurationMillis())

/**
 * The length of a cue's weapon and effect overlays, apart from the seated avatars' own gestures.
 */
private fun CombatAnimationCue.effectOverlayDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackOverlayDurationMillis(
                weaponClassType = weaponClassType,
                normalRollMessageType = normalRollMessageType,
            )

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

        is CombatAnimationCue.SpellPotency ->
            mageSpellPotencyDurationMillis(mainDamage = mainDamage)

        is CombatAnimationCue.Teleport ->
            teleportOverlayDurationMillis(phase = phase, didEscape = didEscape)

        is CombatAnimationCue.HolyShield ->
            paladinHolyShieldOverlayDurationMillis(phase = phase, didSurvive = didSurvive)
    }

private fun normalAttackOverlayDurationMillis(
    weaponClassType: CharacterClassType,
    normalRollMessageType: NormalRollMessageType,
): Long =
    when (weaponClassType) {
        CharacterClassType.Rogue ->
            rogueNormalAttackDurationMillis(normalRollMessageType = normalRollMessageType)

        CharacterClassType.Gambler -> gamblerNormalAttackDurationMillis()

        CharacterClassType.Duelist -> duelistNormalAttackDurationMillis()

        CharacterClassType.Necromancer -> necromancerNormalAttackDurationMillis()

        CharacterClassType.Bard -> bardNormalAttackDurationMillis()

        CharacterClassType.Mage ->
            mageNormalAttackDurationMillis(normalRollMessageType = normalRollMessageType)

        CharacterClassType.Paladin -> paladinNormalAttackDurationMillis()

        else -> 0L
    }
