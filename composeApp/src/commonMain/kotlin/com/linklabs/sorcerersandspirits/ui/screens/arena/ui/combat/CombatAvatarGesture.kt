package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.HolyShieldPhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.MirrorImagePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TeleportPhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.gestureFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.weaponClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BARD_WAVE_REACTION_DELAY_MILLIS
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MAGE_BOLT_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS
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

// TODO: Break out into separate package and files. One new package with utilities file and
//       per-class files - do this later, Claude

private const val REACTION_DELAY_MILLIS = 170L
private const val SHAKE_SEGMENT_MILLIS = 55
private const val SLASH_LEAN_MILLIS = 130
private const val SLASH_RECOVER_MILLIS = 240

private const val ALL_IN_TOSS_GESTURE_MILLIS = 620
private const val COLLAPSE_ROTATION_DEGREES = 80f
private const val COLLAPSE_FEET_SLIDE_FRACTION = 0.4f

private const val TELEPORT_REACT_MILLIS = 120L
private const val TELEPORT_ENTER_MILLIS = 300
private const val TELEPORT_GONE_MILLIS = 220L
private const val TELEPORT_RETURN_MILLIS = 260
private const val TELEPORT_LUNGE_MILLIS = 160
private const val TELEPORT_RECOIL_MILLIS = 180
private const val TELEPORT_GRAY_MILLIS = 320
private const val TELEPORT_ENTER_SCALE = 0.5f
private const val TELEPORT_RETURN_SCALE = 0.72f
private const val TELEPORT_GRAY_SATURATION = 0.12f

private const val MIRROR_SPLIT_MILLIS = 360
private const val MIRROR_SPLIT_HOLD_MILLIS = 160L
private const val MIRROR_POOF_MILLIS = 340
private const val MIRROR_RETURN_MILLIS = 320
private const val MIRROR_SPLIT_OFFSET_FRACTION = 0.28f
private const val MIRROR_COPY_ALPHA = 0.72f
private const val MIRROR_POOF_SCALE_GROWTH = 0.2f
private const val MIRROR_POOF_RISE_FRACTION = 0.12f
private const val HURT_TINT_MAX_RED_OFFSET = 130f
private const val HURT_TINT_CHANNEL_DAMP = 0.45f
private const val ARMOR_TINT_MAX_BLUE_OFFSET = 110f
private const val ARMOR_TINT_CHANNEL_DAMP = 0.3f

private const val FLINCH_BODY_MILLIS = 380L
private const val FLINCH_HEAVY_BODY_MILLIS = 470L
private const val SIDESTEP_BODY_MILLIS = 520L
private const val COLLAPSE_BODY_MILLIS = 650L
private const val DODGE_FLOURISH_BODY_MILLIS = 450L
private const val DODGE_BRACE_BODY_MILLIS = 860L
private const val REARM_BODY_MILLIS = 650L
private const val CAST_BODY_MILLIS = 480L
private const val ALL_IN_BUST_JOLT_MILLIS = 160L

private val feetPivot = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)

@Stable
internal class AvatarGestureState {
    val offsetX = Animatable(initialValue = 0f)
    val offsetY = Animatable(initialValue = 0f)
    val rotation = Animatable(initialValue = 0f)
    val scaleX = Animatable(initialValue = 1f)
    val scaleY = Animatable(initialValue = 1f)
    val collapseFraction = Animatable(initialValue = 0f)
    val hurtTint = Animatable(initialValue = 0f)
    val armorTint = Animatable(initialValue = 0f)
    val saturation = Animatable(initialValue = 1f)

    val mirrorSplit = Animatable(initialValue = 0f)
    val mirrorReturn = Animatable(initialValue = 0f)
    val mirrorFakeVanish = Animatable(initialValue = 0f)

    val teleportVanish = Animatable(initialValue = 0f)

    val colorFilter: ColorFilter?
        get() {
            val isAtRest = hurtTint.value == 0f && armorTint.value == 0f && saturation.value == 1f
            if (isAtRest) return null

            val matrix = ColorMatrix().apply { setToSaturation(sat = saturation.value) }
            matrix *= hurtTintMatrix(tint = hurtTint.value)
            matrix *= armorTintMatrix(tint = armorTint.value)
            return ColorFilter.colorMatrix(colorMatrix = matrix)
        }
}

/** The seated avatar art with its gesture transforms applied. */
@Composable
internal fun GestureClassAvatar(
    characterClass: CharacterClassState,
    gestureState: AvatarGestureState,
    modifier: Modifier = Modifier,
) {
    val isMirrorSplit = gestureState.mirrorSplit.value > 0f
    if (!isMirrorSplit) {
        ClassAvatar(
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

@Composable
private fun MirrorSplitClassAvatar(
    characterClass: CharacterClassState,
    gestureState: AvatarGestureState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        ClassAvatar(
            characterClass = characterClass,
            modifier =
                Modifier.matchParentSize().graphicsLayer {
                    applyMirrorFake(gestureState = gestureState)
                },
        )
        ClassAvatar(
            characterClass = characterClass,
            colorFilter = gestureState.colorFilter,
            modifier =
                Modifier.matchParentSize().graphicsLayer {
                    applyMirrorReal(gestureState = gestureState)
                },
        )
    }
}

private fun GraphicsLayerScope.applyMirrorFake(gestureState: AvatarGestureState) {
    val split = gestureState.mirrorSplit.value
    val vanish = gestureState.mirrorFakeVanish.value
    transformOrigin = feetPivot
    translationX = -size.width * MIRROR_SPLIT_OFFSET_FRACTION * split
    translationY = -size.height * MIRROR_POOF_RISE_FRACTION * vanish
    val grow = 1f + MIRROR_POOF_SCALE_GROWTH * vanish
    scaleX = grow
    scaleY = grow
    alpha = MIRROR_COPY_ALPHA * (1f - vanish)
}

private fun GraphicsLayerScope.applyMirrorReal(gestureState: AvatarGestureState) {
    applyGesture(gestureState = gestureState)
    val split = gestureState.mirrorSplit.value
    val returnFraction = gestureState.mirrorReturn.value
    translationX += size.width * MIRROR_SPLIT_OFFSET_FRACTION * split * (1f - returnFraction)
    alpha = MIRROR_COPY_ALPHA + (1f - MIRROR_COPY_ALPHA) * returnFraction
}

private fun GraphicsLayerScope.applyGesture(gestureState: AvatarGestureState) {
    val collapse = gestureState.collapseFraction.value
    val lyingScale = (size.width / size.height).coerceAtMost(maximumValue = 1f)
    val collapseScale = 1f + (lyingScale - 1f) * collapse

    transformOrigin = feetPivot
    translationX = gestureState.offsetX.value - size.width * COLLAPSE_FEET_SLIDE_FRACTION * collapse
    translationY = gestureState.offsetY.value
    rotationZ = gestureState.rotation.value + COLLAPSE_ROTATION_DEGREES * collapse
    scaleX = gestureState.scaleX.value * collapseScale
    scaleY = gestureState.scaleY.value * collapseScale
    alpha = 1f - gestureState.teleportVanish.value
}

@Composable
internal fun rememberAvatarGesture(
    animationCue: CombatAnimationCue?,
    userId: String,
    isTopSeat: Boolean,
): AvatarGestureState {
    val gestureState = remember { AvatarGestureState() }
    val density = LocalDensity.current
    var lastPlayedCueId by remember { mutableStateOf(value = animationCue?.id) }

    LaunchedEffect(key1 = animationCue) {
        val cue = animationCue ?: return@LaunchedEffect
        if (cue.id == lastPlayedCueId) return@LaunchedEffect
        lastPlayedCueId = cue.id

        val mirrorImageCue = cue.mirrorImageActingFor(userId = userId)
        if (mirrorImageCue != null) {
            gestureState.playMirrorImage(cue = mirrorImageCue, density = density)
            return@LaunchedEffect
        }

        val teleportCue = cue.teleportActingFor(userId = userId)
        if (teleportCue != null) {
            gestureState.playTeleport(cue = teleportCue, density = density)
            return@LaunchedEffect
        }

        val gesture = cue.gestureFor(userId = userId) ?: return@LaunchedEffect
        gestureState.play(
            gesture = gesture,
            strikeDirection = if (isTopSeat) 1f else -1f,
            density = density,
            reactionDelayMillis = cue.defenderReactionDelayMillis(),
        )
    }

    return gestureState
}

private fun CombatAnimationCue.mirrorImageActingFor(
    userId: String
): CombatAnimationCue.MirrorImage? =
    (this as? CombatAnimationCue.MirrorImage)?.takeIf { it.illusionistUserId == userId }

private fun CombatAnimationCue.teleportActingFor(userId: String): CombatAnimationCue.Teleport? =
    (this as? CombatAnimationCue.Teleport)?.takeIf { it.mageUserId == userId }

private suspend fun AvatarGestureState.playTeleport(
    cue: CombatAnimationCue.Teleport,
    density: Density,
) {
    if (cue.phase != TeleportPhase.Resolve) return
    val didEscape = cue.didEscape ?: return

    snapToRest()
    if (didEscape) teleportEscape(density = density) else teleportFail(density = density)
}

private suspend fun AvatarGestureState.teleportEscape(density: Density) =
    with(density) {
        delay(timeMillis = TELEPORT_REACT_MILLIS)
        coroutineScope {
            launch {
                offsetY.animateTo(
                    targetValue = -60.dp.toPx(),
                    animationSpec =
                        tween(
                            durationMillis = TELEPORT_ENTER_MILLIS,
                            easing = FastOutLinearInEasing,
                        ),
                )
            }
            launch {
                stretchTo(
                    targetX = TELEPORT_ENTER_SCALE,
                    targetY = TELEPORT_ENTER_SCALE,
                    durationMillis = TELEPORT_ENTER_MILLIS,
                )
            }
            launch {
                teleportVanish.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = TELEPORT_ENTER_MILLIS),
                )
            }
        }

        delay(timeMillis = TELEPORT_GONE_MILLIS)

        offsetY.snapTo(targetValue = 0f)
        scaleX.snapTo(targetValue = TELEPORT_RETURN_SCALE)
        scaleY.snapTo(targetValue = TELEPORT_RETURN_SCALE)
        coroutineScope {
            launch {
                teleportVanish.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = TELEPORT_RETURN_MILLIS),
                )
            }
            launch {
                stretchTo(targetX = 1f, targetY = 1f, durationMillis = TELEPORT_RETURN_MILLIS)
            }
        }
    }

private suspend fun AvatarGestureState.teleportFail(density: Density) =
    with(density) {
        delay(timeMillis = TELEPORT_REACT_MILLIS)
        offsetY.animateTo(
            targetValue = -24.dp.toPx(),
            animationSpec =
                tween(durationMillis = TELEPORT_LUNGE_MILLIS, easing = FastOutSlowInEasing),
        )
        coroutineScope {
            launch {
                offsetY.animateTo(
                    targetValue = 8.dp.toPx(),
                    animationSpec = tween(durationMillis = TELEPORT_RECOIL_MILLIS / 2),
                )
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = TELEPORT_RECOIL_MILLIS / 2),
                )
            }
            launch {
                saturation.animateTo(
                    targetValue = TELEPORT_GRAY_SATURATION,
                    animationSpec = tween(durationMillis = TELEPORT_GRAY_MILLIS),
                )
            }
        }
    }

private suspend fun AvatarGestureState.playMirrorImage(
    cue: CombatAnimationCue.MirrorImage,
    density: Density,
) {
    when (cue.phase) {
        MirrorImagePhase.Split -> mirrorSplitOut()
        MirrorImagePhase.Resolve -> mirrorResolve(outcome = cue.messageType, density = density)
    }
}

private suspend fun AvatarGestureState.mirrorSplitOut() {
    snapToRest()
    mirrorSplit.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MIRROR_SPLIT_MILLIS, easing = FastOutSlowInEasing),
    )
}

private suspend fun AvatarGestureState.mirrorResolve(
    outcome: NormalRollMessageType?,
    density: Density,
) {
    if (mirrorSplit.value == 0f) mirrorSplit.snapTo(targetValue = 1f)
    mirrorReturn.snapTo(targetValue = 0f)
    mirrorFakeVanish.snapTo(targetValue = 0f)

    coroutineScope {
        launch {
            mirrorFakeVanish.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = MIRROR_POOF_MILLIS),
            )
        }
        launch {
            mirrorReturn.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = MIRROR_RETURN_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        if (outcome != null) {
            launch { playDefenderReaction(outcome = outcome, density = density) }
        }
    }

    mirrorSplit.snapTo(targetValue = 0f)
    mirrorReturn.snapTo(targetValue = 0f)
    mirrorFakeVanish.snapTo(targetValue = 0f)
}

private suspend fun AvatarGestureState.playDefenderReaction(
    outcome: NormalRollMessageType,
    density: Density,
) =
    with(density) {
        when (outcome) {
            NormalRollMessageType.ArmorClassHit ->
                flinch(
                    shakePx = 10.dp.toPx(),
                    tintPeak = 0.7f,
                    reactionDelayMillis = REACTION_DELAY_MILLIS,
                )

            NormalRollMessageType.ArmorClassMiss ->
                sidestep(distancePx = 40.dp.toPx(), reactionDelayMillis = REACTION_DELAY_MILLIS)

            NormalRollMessageType.Damage ->
                flinchHeavy(shakePx = 16.dp.toPx(), reactionDelayMillis = REACTION_DELAY_MILLIS)

            NormalRollMessageType.Knockout -> collapse(reactionDelayMillis = REACTION_DELAY_MILLIS)
        }
    }

private fun CombatAnimationCue.defenderReactionDelayMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack -> normalAttackReactionDelayMillis()

        is CombatAnimationCue.PrepareAttack -> prepareAttackReactionDelayMillis()

        is CombatAnimationCue.Riposte -> RIPOSTE_STRIKE_MILLIS

        is CombatAnimationCue.SpellPotency -> MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS

        is CombatAnimationCue.HolyShield ->
            if (phase == HolyShieldPhase.Wrath) {
                HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS.toLong()
            } else {
                HOLY_SHIELD_PRAY_GLOW_MILLIS.toLong()
            }

        else -> REACTION_DELAY_MILLIS
    }

private fun CombatAnimationCue.NormalAttack.normalAttackReactionDelayMillis(): Long =
    when (weaponClassType) {
        CharacterClassType.Rogue -> MELEE_REACTION_DELAY_MILLIS
        CharacterClassType.Gambler -> THROW_REACTION_DELAY_MILLIS
        CharacterClassType.Duelist -> TWIN_SWORD_REACTION_DELAY_MILLIS
        CharacterClassType.Necromancer -> NECROMANCER_REAP_REACTION_DELAY_MILLIS
        CharacterClassType.Bard -> BARD_WAVE_REACTION_DELAY_MILLIS
        CharacterClassType.Mage -> MAGE_BOLT_REACTION_DELAY_MILLIS
        CharacterClassType.Paladin -> PALADIN_SWING_REACTION_DELAY_MILLIS
        else -> REACTION_DELAY_MILLIS
    }

private fun CombatAnimationCue.PrepareAttack.prepareAttackReactionDelayMillis(): Long =
    if (isSuccessful) PREPARE_ATTACK_STRIKE_MILLIS else PREPARE_ATTACK_WINDUP_MILLIS

internal fun CombatAnimationCue.seatedAvatarDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackReactionDelayMillis() +
                defenderReactionBodyMillis(normalRollMessageType = normalRollMessageType)

        is CombatAnimationCue.KillingMomentum -> REARM_BODY_MILLIS

        is CombatAnimationCue.Dodge ->
            if (didArmorIncrease) DODGE_BRACE_BODY_MILLIS else DODGE_FLOURISH_BODY_MILLIS

        is CombatAnimationCue.AllIn -> allInBodyMillis(outcome = outcome)

        is CombatAnimationCue.LuckyCoinFlip -> 0L

        is CombatAnimationCue.LuckyCoinHighRoller -> 0L

        is CombatAnimationCue.LuckyCoinHighRollerThrow ->
            REACTION_DELAY_MILLIS +
                if (isKnockout) COLLAPSE_BODY_MILLIS else FLINCH_HEAVY_BODY_MILLIS

        is CombatAnimationCue.PrepareAttack ->
            prepareAttackReactionDelayMillis() +
                if (isSuccessful) COLLAPSE_BODY_MILLIS else SIDESTEP_BODY_MILLIS

        is CombatAnimationCue.Riposte ->
            if (phase == RipostePhase.Landed) RIPOSTE_STRIKE_MILLIS + FLINCH_BODY_MILLIS else 0L

        is CombatAnimationCue.SummonMinion -> CAST_BODY_MILLIS

        is CombatAnimationCue.Persuade -> 0L

        is CombatAnimationCue.Encore -> 0L

        is CombatAnimationCue.Impersonate -> 0L

        is CombatAnimationCue.MirrorImage -> mirrorImageBodyMillis()

        is CombatAnimationCue.SpellPotency ->
            MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS + FLINCH_HEAVY_BODY_MILLIS

        is CombatAnimationCue.Teleport -> teleportBodyMillis()

        is CombatAnimationCue.HolyShield -> {
            val isWrath = phase == HolyShieldPhase.Wrath
            val didFailPrayer = phase == HolyShieldPhase.Pray && didSurvive == false
            when {
                isWrath -> HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS.toLong() + FLINCH_HEAVY_BODY_MILLIS
                didFailPrayer -> HOLY_SHIELD_PRAY_GLOW_MILLIS.toLong() + COLLAPSE_BODY_MILLIS
                else -> 0L
            }
        }
    }

private fun CombatAnimationCue.Teleport.teleportBodyMillis(): Long =
    when (phase) {
        TeleportPhase.Open,
        TeleportPhase.Hold -> 0L

        TeleportPhase.Resolve ->
            if (didEscape == true) {
                TELEPORT_REACT_MILLIS +
                    TELEPORT_ENTER_MILLIS +
                    TELEPORT_GONE_MILLIS +
                    TELEPORT_RETURN_MILLIS
            } else {
                TELEPORT_REACT_MILLIS +
                    TELEPORT_LUNGE_MILLIS +
                    maxOf(TELEPORT_RECOIL_MILLIS.toLong(), TELEPORT_GRAY_MILLIS.toLong())
            }
    }

private fun CombatAnimationCue.MirrorImage.mirrorImageBodyMillis(): Long =
    when (phase) {
        MirrorImagePhase.Split -> MIRROR_SPLIT_MILLIS + MIRROR_SPLIT_HOLD_MILLIS

        MirrorImagePhase.Resolve -> {
            val resolvedMessageType = messageType
            val reactionMillis =
                if (resolvedMessageType != null) {
                    REACTION_DELAY_MILLIS +
                        defenderReactionBodyMillis(normalRollMessageType = resolvedMessageType)
                } else {
                    0L
                }
            maxOf(MIRROR_POOF_MILLIS.toLong(), MIRROR_RETURN_MILLIS.toLong(), reactionMillis)
        }
    }

private fun defenderReactionBodyMillis(normalRollMessageType: NormalRollMessageType): Long =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> FLINCH_BODY_MILLIS
        NormalRollMessageType.ArmorClassMiss -> SIDESTEP_BODY_MILLIS
        NormalRollMessageType.Damage -> FLINCH_HEAVY_BODY_MILLIS
        NormalRollMessageType.Knockout -> COLLAPSE_BODY_MILLIS
    }

private fun allInBodyMillis(outcome: AllInOutcome): Long =
    when (outcome) {
        AllInOutcome.SelfKnockout ->
            (ALL_IN_TOSS_MILLIS + ALL_IN_SETTLE_MILLIS).toLong() +
                ALL_IN_BUST_JOLT_MILLIS +
                COLLAPSE_BODY_MILLIS

        AllInOutcome.NoEffect,
        AllInOutcome.DoubleAttack -> ALL_IN_TOSS_GESTURE_MILLIS.toLong()
    }

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

private suspend fun AvatarGestureState.cast(reachPx: Float) {
    offsetY.animateTo(targetValue = reachPx, animationSpec = tween(durationMillis = 180))
    offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
}

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

private fun twinSlashPreambleMillis(): Long =
    (TWIN_SLASH_RAISE_MILLIS + TWIN_SLASH_READY_HOLD_MILLIS).toLong()

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
            (ALL_IN_TOSS_MILLIS + ALL_IN_SETTLE_MILLIS - ALL_IN_TOSS_GESTURE_MILLIS).toLong()
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

private suspend fun AvatarGestureState.flinch(
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

private suspend fun AvatarGestureState.flinchHeavy(shakePx: Float, reactionDelayMillis: Long) {
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

private suspend fun AvatarGestureState.sidestep(distancePx: Float, reactionDelayMillis: Long) {
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

private suspend fun AvatarGestureState.collapse(reactionDelayMillis: Long) {
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

private suspend fun AvatarGestureState.stretchTo(
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

private suspend fun AvatarGestureState.snapToRest() {
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

private fun armorTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - ARMOR_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                ARMOR_TINT_MAX_BLUE_OFFSET * tint,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}

private fun hurtTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - HURT_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                HURT_TINT_MAX_RED_OFFSET * tint,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}
