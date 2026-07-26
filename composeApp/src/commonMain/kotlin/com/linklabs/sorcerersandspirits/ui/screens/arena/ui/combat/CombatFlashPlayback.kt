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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.collapsingUserId
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

private const val FLASH_HOLD_MILLIS = 850L
private const val FLASH_EXIT_MILLIS = 320L

internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS

@Stable
internal class FlashPlaybackState(initialLastPlayedMessageId: String?) {
    var currentMessage by mutableStateOf<ArenaMessageState?>(value = null)
        private set

    var currentCallout by mutableStateOf<FlashCallout?>(value = null)
        private set

    var currentAnchor by mutableStateOf<FlashAnchor>(value = FlashAnchor.Center)
        private set

    var combatAnimationCue by mutableStateOf<CombatAnimationCue?>(value = null)
        private set

    var finishedCollapseMessageId by mutableStateOf<String?>(value = null)
        private set

    var isActing by mutableStateOf(value = false)
        private set

    var lastPlayedMessageId by mutableStateOf(value = initialLastPlayedMessageId)
        private set

    val visibleState = MutableTransitionState(initialState = false)

    fun markActing() {
        isActing = true
    }

    fun clearActing() {
        isActing = false
    }

    fun settlePlayback(lastPlayedMessageId: String?) {
        this.lastPlayedMessageId = lastPlayedMessageId
    }

    // TODO(SS-250): don't derive "animation in progress" by scanning the message stream; drive it
    //  from the sequencer's own play/idle lifecycle so it never depends on ArenaMessage shape.
    fun isAnimating(messages: List<PlayableMessage>): Boolean =
        isActing ||
            messages.freshAfter(lastPlayedId = lastPlayedMessageId).any {
                it.cue != null || it.callout != null
            }

    fun playAnimation(cue: CombatAnimationCue?) {
        combatAnimationCue = cue
    }

    fun markCollapseFinished(messageId: String) {
        finishedCollapseMessageId = messageId
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
    diceStateHolder: DiceStateHolder,
): FlashPlaybackState {
    val playback = remember {
        FlashPlaybackState(initialLastPlayedMessageId = messages.lastOrNull()?.id)
    }
    val latestMessages by rememberUpdatedState(newValue = messages)
    val latestDiceRoll by rememberUpdatedState(newValue = diceRollState)
    val latestDiceBadge by rememberUpdatedState(newValue = diceBadgeState)
    val latestIsBadgeBonusSpent by rememberUpdatedState(newValue = isBadgeBonusSpent)

    LaunchedEffect(key1 = Unit) {
        var lastPlayedId = latestMessages.lastOrNull()?.id
        var lastSize = latestMessages.size
        var lastRoll = latestDiceRoll
        snapshotFlow { latestMessages }
            .collect { messages ->
                playback.clearActing()

                val wasReset = messages.size < lastSize
                if (wasReset) lastPlayedId = null

                val fresh = messages.freshAfter(lastPlayedId = lastPlayedId)

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
                    if (cue == null && callout == null) continue

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

                    if (cue?.collapsingUserId != null) {
                        playback.markCollapseFinished(messageId = playable.message.id)
                    }
                }

                lastPlayedId = messages.lastOrNull()?.id ?: lastPlayedId
                lastSize = messages.size
                playback.settlePlayback(lastPlayedMessageId = lastPlayedId)
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

private fun List<PlayableMessage>.freshAfter(lastPlayedId: String?): List<PlayableMessage> {
    if (lastPlayedId == null) return this

    val lastPlayedIndex = indexOfLast { it.id == lastPlayedId }
    if (lastPlayedIndex == -1) return emptyList()

    return subList(lastPlayedIndex + 1, size)
}
