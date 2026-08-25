package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BARD_WAVE_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarImage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_APPEAR_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_STRIKE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_WINDUP_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_GLEAM_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_PULL_BACK_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_THRUST_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_TWIRL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_STRIKE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_RAISE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_READY_HOLD_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_STAGGER_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SWORD_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.ALL_IN_SETTLE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.ALL_IN_TOSS_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.THROW_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.MirrorSplitClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.getMirrorImageMessageOrNull
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.playMirrorImage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MAGE_BOLT_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.getTeleportMessageOrNull
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.playTeleport
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.meleeDashDistance
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NECROMANCER_REAP_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.HOLY_SHIELD_PRAY_GLOW_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.PALADIN_SWING_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_DASH_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_EMBED_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_THRUST_MILLIS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO(SS-258): Break out into separate package and files. One new package with utilities file and
//       per-class files - do this later, Claude

internal const val REACTION_DELAY_MILLIS = 170L
private const val SHAKE_SEGMENT_MILLIS = 55
private const val SLASH_LEAN_MILLIS = 130
private const val SLASH_RECOVER_MILLIS = 240

private const val ALL_IN_TOSS_GESTURE_MILLIS = 620

// TODO(SS-258): This function is set up so weird

/** The seated avatar art with its gesture transforms applied. */
@Composable
internal fun GestureClassAvatar(
    animatingMessage: ArenaMessageState?,
    isTopSeat: Boolean,
    userId: String,
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
) {
    // TODO(SS-258): This is playing the avatar gesture animation. Should indicate that
    val gestureState =
        rememberAvatarGesture(
            animatingMessage = animatingMessage,
            userId = userId,
            isTopSeat = isTopSeat,
        )

    val isMirrorSplit = gestureState.mirrorSplit.value > 0f
    if (!isMirrorSplit) {
        AvatarImage(
            characterClass = characterClass,
            colorFilter = gestureState.colorFilter,
            modifier = modifier.graphicsLayer { applyGesture(gestureState = gestureState) },
        )
        return
    }

    MirrorSplitClassAvatar(
        characterClass = characterClass,
        gestureState = gestureState,
        modifier = modifier,
    )
}

// TODO(SS-258): This naming is wrong. It does return a remembered value, but it plays the animation
// as
// well. I believe it should just remember something if needed, then return it.
// This is the refactor idea:
//    remember state
//  → interpret animatingMessage
//  → select animation path
//  → mutate gestureState
//  → render gestureState
@Composable
internal fun rememberAvatarGesture(
    animatingMessage: ArenaMessageState?,
    userId: String,
    isTopSeat: Boolean,
): AvatarGestureState {
    val gestureState = remember { AvatarGestureState() }
    if (animatingMessage == null || !animatingMessage.isAvatar()) return gestureState

    val density = LocalDensity.current
    val onAvatarComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage) {
        val mirrorImageMessage = animatingMessage.getMirrorImageMessageOrNull(userId = userId)
        if (mirrorImageMessage != null) {
            gestureState.playMirrorImage(message = mirrorImageMessage, density = density)
            if (animatingMessage.isAvatarCompletedByGesture) onAvatarComplete()
            return@LaunchedEffect
        }

        val teleportMessage = animatingMessage.getTeleportMessageOrNull(userId = userId)
        if (teleportMessage != null) {
            gestureState.playTeleport(message = teleportMessage, density = density)
            return@LaunchedEffect
        }

        // TODO(SS-258): gestures can be entirely determined at view model mapping time, for top and
        // bottom
        val gesture = animatingMessage.gestureFor(userId = userId) ?: return@LaunchedEffect
        gestureState.play(
            gesture = gesture,
            strikeDirection = if (isTopSeat) 1f else -1f,
            density = density,
            reactionDelayMillis = animatingMessage.defenderReactionDelayMillis(),
        )
        if (animatingMessage.isAvatarCompletedByGesture) onAvatarComplete()
    }

    return gestureState
}

// TODO(SS-258): Common
private fun ArenaMessageState.defenderReactionDelayMillis(): Long = when (this) {
    is ArenaMessageState.NormalRoll -> normalAttackReactionDelayMillis()

    is ArenaMessageState.PrepareAttack -> prepareAttackReactionDelayMillis()

    is ArenaMessageState.Riposte -> RIPOSTE_STRIKE_MILLIS

    is ArenaMessageState.SpellPotency -> MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS

    is ArenaMessageState.HolyShield ->
        if (this is ArenaMessageState.HolyShield.Wrath) {
            HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS.toLong()
        } else {
            HOLY_SHIELD_PRAY_GLOW_MILLIS.toLong()
        }

    else -> REACTION_DELAY_MILLIS
}

// TODO(SS-258): Common
private fun ArenaMessageState.NormalRoll.normalAttackReactionDelayMillis(): Long = when (attackerClassType) {
    CharacterClassType.Rogue -> MELEE_REACTION_DELAY_MILLIS
    CharacterClassType.Gambler -> THROW_REACTION_DELAY_MILLIS
    CharacterClassType.Duelist -> TWIN_SWORD_REACTION_DELAY_MILLIS
    CharacterClassType.Necromancer -> NECROMANCER_REAP_REACTION_DELAY_MILLIS
    CharacterClassType.Bard -> BARD_WAVE_REACTION_DELAY_MILLIS
    CharacterClassType.Mage -> MAGE_BOLT_REACTION_DELAY_MILLIS
    CharacterClassType.Paladin -> PALADIN_SWING_REACTION_DELAY_MILLIS
    else -> REACTION_DELAY_MILLIS
}

// TODO(SS-258): Duelist
private fun ArenaMessageState.PrepareAttack.prepareAttackReactionDelayMillis(): Long = if (isSuccessful) PREPARE_ATTACK_STRIKE_MILLIS else PREPARE_ATTACK_WINDUP_MILLIS

// TODO(SS-258): Common
private suspend fun AvatarGestureState.play(
    gesture: AvatarGesture,
    strikeDirection: Float,
    density: Density,
    reactionDelayMillis: Long,
) {
    snapToRest()
    with(density) {
        when (gesture) {
            AvatarGesture.Lunge ->
                lunge(distancePx = 48.dp.toPx() * strikeDirection, peakScale = 1.06f)

            AvatarGesture.LungeStrong ->
                lunge(distancePx = 68.dp.toPx() * strikeDirection, peakScale = 1.12f)

            AvatarGesture.LungeOvershoot ->
                lungeOvershoot(distancePx = 80.dp.toPx() * strikeDirection)

            AvatarGesture.Stab -> stab(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.StabFlurry ->
                stabFlurry(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.StabOvershoot ->
                stabOvershoot(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.Slash ->
                slashCombo(reachPx = 30.dp.toPx() * strikeDirection, isFinisher = false)

            AvatarGesture.SlashFinish ->
                slashCombo(reachPx = 30.dp.toPx() * strikeDirection, isFinisher = true)

            AvatarGesture.SlashWhiff -> slashWhiff(reachPx = 30.dp.toPx() * strikeDirection)

            AvatarGesture.Cast -> cast(reachPx = 14.dp.toPx() * strikeDirection)

            AvatarGesture.PreparedThrust -> preparedThrust(reachPx = 36.dp.toPx() * strikeDirection)

            AvatarGesture.PreparedThrustWhiff ->
                preparedThrustWhiff(reachPx = 36.dp.toPx() * strikeDirection)

            AvatarGesture.Throw ->
                throwFlick(flickPx = 26.dp.toPx() * strikeDirection, peakScale = 1.05f)

            AvatarGesture.ThrowStrong ->
                throwFlick(flickPx = 34.dp.toPx() * strikeDirection, peakScale = 1.1f)

            AvatarGesture.ThrowOvershoot -> throwOvershoot(flickPx = 32.dp.toPx() * strikeDirection)

            AvatarGesture.AllInToss -> allInToss(liftPx = 16.dp.toPx())

            AvatarGesture.AllInBust -> allInBust(liftPx = 16.dp.toPx())

            AvatarGesture.ReArm -> reArm(retreatPx = 30.dp.toPx() * -strikeDirection)

            AvatarGesture.Flinch ->
                flinch(
                    shakePx = 10.dp.toPx(),
                    tintPeak = 0.7f,
                    reactionDelayMillis = reactionDelayMillis,
                )

            AvatarGesture.FlinchHeavy ->
                flinchHeavy(shakePx = 16.dp.toPx(), reactionDelayMillis = reactionDelayMillis)

            AvatarGesture.Sidestep ->
                sidestep(distancePx = 40.dp.toPx(), reactionDelayMillis = reactionDelayMillis)

            AvatarGesture.DodgeFlourish -> dodgeFlourish(distancePx = 44.dp.toPx())

            AvatarGesture.DodgeBrace -> dodgeBrace(distancePx = 44.dp.toPx())

            AvatarGesture.Collapse -> collapse(reactionDelayMillis = reactionDelayMillis)
        }
    }
}

// TODO(SS-258): Common
private suspend fun AvatarGestureState.cast(reachPx: Float) {
    offsetY.animateTo(targetValue = reachPx, animationSpec = tween(durationMillis = 180))
    offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
}

// TODO(SS-258): Common
private suspend fun AvatarGestureState.lunge(distancePx: Float, peakScale: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 140))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280))
        }
        launch {
            stretchTo(targetX = peakScale, targetY = peakScale, durationMillis = 140)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 280)
        }
    }
}

// TODO(SS-258): Common
private suspend fun AvatarGestureState.lungeOvershoot(distancePx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 180))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 420))
        }
        launch {
            rotation.animateTo(targetValue = 8f, animationSpec = tween(durationMillis = 180))
            rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

private suspend fun AvatarGestureState.stab(dashPx: Float) {
    dashIn(dashPx = dashPx)
    offsetY.animateTo(
        targetValue = dashPx * 1.12f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS),
    )
    delay(timeMillis = MELEE_EMBED_MILLIS)
    settleBack()
}

private suspend fun AvatarGestureState.stabFlurry(dashPx: Float) {
    dashIn(dashPx = dashPx)
    offsetY.animateTo(
        targetValue = dashPx * 1.12f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS),
    )
    delay(timeMillis = 80L)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx * 1.28f,
                animationSpec = tween(durationMillis = 120),
            )
        }
        launch { stretchTo(targetX = 0.94f, targetY = 1.08f, durationMillis = 120) }
    }
    delay(timeMillis = MELEE_EMBED_MILLIS)
    settleBack()
}

private suspend fun AvatarGestureState.stabOvershoot(dashPx: Float) {
    dashIn(dashPx = dashPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx * 1.3f,
                animationSpec = tween(durationMillis = 150),
            )
        }
        launch { rotation.animateTo(targetValue = 8f, animationSpec = tween(durationMillis = 150)) }
    }
    rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 150))
    settleBack()
}

private suspend fun AvatarGestureState.dashIn(dashPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx,
                animationSpec = tween(durationMillis = MELEE_DASH_MILLIS),
            )
        }
        launch {
            stretchTo(targetX = 1.04f, targetY = 0.94f, durationMillis = MELEE_DASH_MILLIS / 2)
            stretchTo(targetX = 0.97f, targetY = 1.05f, durationMillis = MELEE_DASH_MILLIS / 2)
        }
    }
}

private suspend fun AvatarGestureState.settleBack() {
    coroutineScope {
        launch { offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280)) }
        launch { stretchTo(targetX = 1f, targetY = 1f, durationMillis = 280) }
        launch { rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280)) }
    }
}

private suspend fun AvatarGestureState.slashCombo(reachPx: Float, isFinisher: Boolean) {
    delay(timeMillis = twinSlashPreambleMillis())
    coroutineScope {
        launch { slashBeat(reachPx = reachPx, tiltDegrees = 7f) }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            val followReach = if (isFinisher) reachPx * 1.25f else reachPx
            slashBeat(reachPx = followReach, tiltDegrees = -7f)
        }
    }
}

private fun twinSlashPreambleMillis(): Long = (TWIN_SLASH_RAISE_MILLIS + TWIN_SLASH_READY_HOLD_MILLIS).toLong()

private suspend fun AvatarGestureState.slashBeat(reachPx: Float, tiltDegrees: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx,
                animationSpec =
                tween(durationMillis = SLASH_LEAN_MILLIS, easing = FastOutSlowInEasing),
            )
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_RECOVER_MILLIS),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = tiltDegrees,
                animationSpec = tween(durationMillis = SLASH_LEAN_MILLIS),
            )
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_RECOVER_MILLIS),
            )
        }
        launch {
            stretchTo(targetX = 1.04f, targetY = 0.98f, durationMillis = SLASH_LEAN_MILLIS)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = SLASH_RECOVER_MILLIS)
        }
    }
}

private suspend fun AvatarGestureState.slashWhiff(reachPx: Float) {
    delay(timeMillis = twinSlashPreambleMillis())
    coroutineScope {
        launch { slashBeat(reachPx = reachPx, tiltDegrees = 7f) }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            slashOvershoot(reachPx = reachPx)
        }
    }
}

private suspend fun AvatarGestureState.slashOvershoot(reachPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx * 1.3f,
                animationSpec = tween(durationMillis = 150),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            rotation.animateTo(targetValue = 12f, animationSpec = tween(durationMillis = 150))
            rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

private suspend fun AvatarGestureState.preparedThrust(reachPx: Float) {
    prepareCoil(reachPx = reachPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx,
                animationSpec =
                tween(durationMillis = PREPARE_THRUST_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            stretchTo(targetX = 0.96f, targetY = 1.06f, durationMillis = PREPARE_THRUST_MILLIS)
        }
    }
    settleBack()
}

private suspend fun AvatarGestureState.preparedThrustWhiff(reachPx: Float) {
    prepareCoil(reachPx = reachPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx * 1.3f,
                animationSpec =
                tween(durationMillis = PREPARE_THRUST_MILLIS, easing = FastOutLinearInEasing),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            rotation.animateTo(
                targetValue = 10f,
                animationSpec = tween(durationMillis = PREPARE_THRUST_MILLIS),
            )
            rotation.animateTo(targetValue = -4f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
        launch { stretchTo(targetX = 1f, targetY = 1f, durationMillis = 300) }
    }
}

private suspend fun AvatarGestureState.prepareCoil(reachPx: Float) {
    val coilMillis = PREPARE_APPEAR_MILLIS + PREPARE_TWIRL_MILLIS + PREPARE_PULL_BACK_MILLIS
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * reachPx,
                animationSpec = tween(durationMillis = coilMillis, easing = FastOutSlowInEasing),
            )
        }
        launch { stretchTo(targetX = 1.05f, targetY = 0.94f, durationMillis = coilMillis) }
    }
    delay(timeMillis = PREPARE_GLEAM_MILLIS.toLong())
}

private suspend fun AvatarGestureState.throwFlick(flickPx: Float, peakScale: Float) {
    val leanDirection = if (flickPx >= 0f) 1f else -1f
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * flickPx,
                animationSpec = tween(durationMillis = 110),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = -5f * leanDirection,
                animationSpec = tween(durationMillis = 110),
            )
        }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = flickPx, animationSpec = tween(durationMillis = 90))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 260))
        }
        launch {
            rotation.animateTo(
                targetValue = 4f * leanDirection,
                animationSpec = tween(durationMillis = 90),
            )
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 260))
        }
        launch {
            stretchTo(targetX = peakScale, targetY = peakScale, durationMillis = 90)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 260)
        }
    }
}

private suspend fun AvatarGestureState.throwOvershoot(flickPx: Float) {
    val leanDirection = if (flickPx >= 0f) 1f else -1f
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * flickPx,
                animationSpec = tween(durationMillis = 110),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = -6f * leanDirection,
                animationSpec = tween(durationMillis = 110),
            )
        }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = flickPx * 1.2f,
                animationSpec = tween(durationMillis = 100),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 320))
        }
        launch {
            rotation.animateTo(
                targetValue = 10f * leanDirection,
                animationSpec = tween(durationMillis = 100),
            )
            rotation.animateTo(
                targetValue = -4f * leanDirection,
                animationSpec = tween(durationMillis = 160),
            )
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

private suspend fun AvatarGestureState.allInToss(liftPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = liftPx * 0.5f,
                animationSpec = tween(durationMillis = 150),
            )
        }
        launch { stretchTo(targetX = 1.04f, targetY = 0.95f, durationMillis = 150) }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = -liftPx, animationSpec = tween(durationMillis = 170))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            stretchTo(targetX = 0.96f, targetY = 1.06f, durationMillis = 170)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 300)
        }
    }
}

private suspend fun AvatarGestureState.allInBust(liftPx: Float) {
    allInToss(liftPx = liftPx)
    delay(
        timeMillis =
        (ALL_IN_TOSS_MILLIS + ALL_IN_SETTLE_MILLIS - ALL_IN_TOSS_GESTURE_MILLIS).toLong(),
    )
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = liftPx * 0.4f,
                animationSpec = tween(durationMillis = 70),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 90))
        }
        launch {
            stretchTo(targetX = 1.08f, targetY = 0.9f, durationMillis = 70)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 90)
        }
    }
    collapse(reactionDelayMillis = 0L)
}

private suspend fun AvatarGestureState.reArm(retreatPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = retreatPx, animationSpec = tween(durationMillis = 140))
        }
        launch { stretchTo(targetX = 1.08f, targetY = 0.86f, durationMillis = 140) }
    }
    delay(timeMillis = 180L)
    coroutineScope {
        launch { offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 150)) }
        launch {
            stretchTo(targetX = 0.95f, targetY = 1.08f, durationMillis = 150)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 180)
        }
    }
}

private suspend fun AvatarGestureState.dodgeFlourish(distancePx: Float) {
    coroutineScope {
        launch {
            offsetX.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 110))
            offsetX.animateTo(
                targetValue = -0.35f * distancePx,
                animationSpec = tween(durationMillis = 180),
            )
            offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 160))
        }
        launch {
            rotation.animateTo(targetValue = -9f, animationSpec = tween(durationMillis = 110))
            rotation.animateTo(targetValue = 5f, animationSpec = tween(durationMillis = 180))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 160))
        }
    }
}

private suspend fun AvatarGestureState.dodgeBrace(distancePx: Float) {
    dodgeFlourish(distancePx = distancePx)
    coroutineScope {
        launch {
            armorTint.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 90))
            armorTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 320))
        }
        launch {
            stretchTo(targetX = 1.05f, targetY = 1.05f, durationMillis = 90)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 320)
        }
    }
}

internal suspend fun AvatarGestureState.flinch(
    shakePx: Float,
    tintPeak: Float,
    reactionDelayMillis: Long,
) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch { shake(shakePx, -0.7f * shakePx, 0.4f * shakePx, 0f) }
        launch {
            hurtTint.animateTo(targetValue = tintPeak, animationSpec = tween(durationMillis = 80))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
    }
}

internal suspend fun AvatarGestureState.flinchHeavy(shakePx: Float, reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch { shake(shakePx, -0.8f * shakePx, 0.5f * shakePx, -0.25f * shakePx, 0f) }
        launch {
            hurtTint.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 90))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 380))
        }
        launch {
            stretchTo(targetX = 1.06f, targetY = 0.9f, durationMillis = 110)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 240)
        }
    }
}

internal suspend fun AvatarGestureState.sidestep(distancePx: Float, reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch {
            offsetX.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 120))
            delay(timeMillis = 200L)
            offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
        launch {
            rotation.animateTo(targetValue = 6f, animationSpec = tween(durationMillis = 120))
            delay(timeMillis = 200L)
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

internal suspend fun AvatarGestureState.collapse(reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch {
            hurtTint.animateTo(targetValue = 0.8f, animationSpec = tween(durationMillis = 90))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500))
        }
        launch {
            collapseFraction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450),
            )
        }
        launch {
            saturation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 650))
        }
    }
}

private suspend fun AvatarGestureState.shake(vararg targetsPx: Float) {
    for (target in targetsPx) {
        offsetX.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = SHAKE_SEGMENT_MILLIS),
        )
    }
}

internal suspend fun AvatarGestureState.stretchTo(
    targetX: Float,
    targetY: Float,
    durationMillis: Int,
) {
    coroutineScope {
        launch {
            scaleX.animateTo(
                targetValue = targetX,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
        launch {
            scaleY.animateTo(
                targetValue = targetY,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
    }
}

internal suspend fun AvatarGestureState.snapToRest() {
    offsetX.snapTo(targetValue = 0f)
    offsetY.snapTo(targetValue = 0f)
    rotation.snapTo(targetValue = 0f)
    scaleX.snapTo(targetValue = 1f)
    scaleY.snapTo(targetValue = 1f)
    collapseFraction.snapTo(targetValue = 0f)
    hurtTint.snapTo(targetValue = 0f)
    armorTint.snapTo(targetValue = 0f)
    saturation.snapTo(targetValue = 1f)
    mirrorSplit.snapTo(targetValue = 0f)
    mirrorReturn.snapTo(targetValue = 0f)
    mirrorFakeVanish.snapTo(targetValue = 0f)
    teleportVanish.snapTo(targetValue = 0f)
}
