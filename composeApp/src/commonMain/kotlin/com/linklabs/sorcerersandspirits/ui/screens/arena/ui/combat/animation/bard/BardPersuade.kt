package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val LUTE_APPEAR_MILLIS = 240
private const val STRUM_RING_MILLIS = 540
private const val NOTE_ENTER_MILLIS = 380
private const val GRAY_MILLIS = 420
private const val NOTE_FALL_MILLIS = 640
private const val FLOURISH_MILLIS = 720
private const val FLUTE_CROSSFADE_MILLIS = 460
private const val AIR_FLOW_MILLIS = 1000
private const val SWIRL_MILLIS = 1200
private const val LUTE_FADE_MILLIS = 280
private const val LUTE_POP_SCALE = 0.72f

@Stable
internal class PersuadeEffectState {
    val luteAlpha = Animatable(initialValue = 0f)
    val luteScale = Animatable(initialValue = LUTE_POP_SCALE)
    val strum = Animatable(initialValue = 0f)
    val notes = Animatable(initialValue = 0f)
    val grayness = Animatable(initialValue = 0f)
    val fall = Animatable(initialValue = 0f)
    val flourish = Animatable(initialValue = 0f)
    val fluteReveal = Animatable(initialValue = 0f)
    val airFlow = Animatable(initialValue = 0f)
    val swirl = Animatable(initialValue = 0f)

    var scene by mutableStateOf<PersuadeScene?>(value = null)
}

@Immutable internal data class PersuadeScene(val anchor: Offset, val accentColor: Color)

@Composable
internal fun PersuadeOverlay(
    animatingMessage: ArenaMessageState.Persuade,
    bardBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { PersuadeEffectState() }
    val density = LocalDensity.current
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val scene =
            persuadeScene(
                bardBounds = bardBounds,
                accentColor = CharacterClassType.Bard.toColor(),
                density = density,
            )
        state.play(scene = scene, message = animatingMessage)

        onAvatarAnimationComplete()
    }

    Canvas(modifier = modifier) { drawPersuade(state = state) }
}

private fun persuadeScene(bardBounds: Rect, accentColor: Color, density: Density): PersuadeScene {
    val liftPx = with(density) { 6.dp.toPx() }
    return PersuadeScene(
        anchor = bardBounds.center - Offset(x = 0f, y = liftPx),
        accentColor = accentColor,
    )
}

private suspend fun PersuadeEffectState.play(
    scene: PersuadeScene,
    message: ArenaMessageState.Persuade,
) {
    when (message) {
        is ArenaMessageState.Persuade.Trigger -> readyLute(scene = scene)

        is ArenaMessageState.Persuade.StrumLute ->
            strumLute(scene = scene, isSuccessful = message.isSuccessful)

        is ArenaMessageState.Persuade.CharmActivation -> readyFlute(scene = scene)

        is ArenaMessageState.Persuade.Charm -> blowFlute(scene = scene)
    }
}

private suspend fun PersuadeEffectState.readyLute(scene: PersuadeScene) {
    resetChannels()
    luteAlpha.snapTo(targetValue = 0f)
    luteScale.snapTo(targetValue = LUTE_POP_SCALE)
    this.scene = scene

    coroutineScope {
        launch {
            luteAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = LUTE_APPEAR_MILLIS),
            )
        }
        launch {
            luteScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
            )
        }
    }
}

private suspend fun PersuadeEffectState.strumLute(scene: PersuadeScene, isSuccessful: Boolean) {
    beginLuteResolve(scene = scene)
    ringStrings()

    if (isSuccessful) {
        flourish.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FLOURISH_MILLIS, easing = FastOutSlowInEasing),
        )
    } else {
        coroutineScope {
            launch {
                grayness.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = GRAY_MILLIS),
                )
            }
            launch {
                fall.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(durationMillis = NOTE_FALL_MILLIS, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    fadeOut()
}

private suspend fun PersuadeEffectState.readyFlute(scene: PersuadeScene) {
    beginLuteResolve(scene = scene)

    // TODO: Why is ringStrings() here? Should it be something else?
    ringStrings()
    flourish.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = FLOURISH_MILLIS, easing = FastOutSlowInEasing),
    )
    fluteReveal.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = FLUTE_CROSSFADE_MILLIS),
    )
}

private suspend fun PersuadeEffectState.blowFlute(scene: PersuadeScene) {
    this.scene = scene
    luteAlpha.snapTo(targetValue = 1f)
    fluteReveal.snapTo(targetValue = 1f)
    flourish.snapTo(targetValue = 1f)
    airFlow.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)

    coroutineScope {
        launch {
            airFlow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = AIR_FLOW_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SWIRL_MILLIS, easing = LinearEasing),
            )
        }
    }
    fadeOut()
}

private suspend fun PersuadeEffectState.beginLuteResolve(scene: PersuadeScene) {
    this.scene = scene
    luteAlpha.snapTo(targetValue = 1f)
    luteScale.snapTo(targetValue = 1f)
    resetChannels()
}

private suspend fun PersuadeEffectState.ringStrings() {
    coroutineScope {
        launch {
            strum.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = STRUM_RING_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            notes.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = NOTE_ENTER_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
}

private suspend fun PersuadeEffectState.fadeOut() {
    luteAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = LUTE_FADE_MILLIS))
    this.scene = null
}

private suspend fun PersuadeEffectState.resetChannels() {
    strum.snapTo(targetValue = 0f)
    notes.snapTo(targetValue = 0f)
    grayness.snapTo(targetValue = 0f)
    fall.snapTo(targetValue = 0f)
    flourish.snapTo(targetValue = 0f)
    fluteReveal.snapTo(targetValue = 0f)
    airFlow.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)
}

private fun DrawScope.drawPersuade(state: PersuadeEffectState) {
    val scene = state.scene ?: return
    val grayness = state.grayness.value
    val luteAlpha = state.luteAlpha.value
    val fluteReveal = state.fluteReveal.value
    val accent = scene.accentColor

    val isLuteShowing = luteAlpha > 0f && fluteReveal < 1f
    if (isLuteShowing) {
        drawLute(
            anchor = scene.anchor,
            accentColor = accent,
            alpha = luteAlpha * (1f - fluteReveal),
            scale = state.luteScale.value,
            strum = state.strum.value,
            grayness = grayness,
        )
    }
    if (fluteReveal > 0f) {
        drawFlute(
            anchor = scene.anchor,
            accentColor = accent,
            alpha = luteAlpha * fluteReveal,
            airFlow = state.airFlow.value,
        )
    }
    drawPersuadeNotes(
        anchor = scene.anchor,
        accentColor = accent,
        grayness = grayness,
        enter = state.notes.value,
        fall = state.fall.value,
        flourish = state.flourish.value,
    )
    drawSwirlNotes(
        anchor = scene.anchor,
        accentColor = accent,
        swirl = state.swirl.value,
        alpha = luteAlpha,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeFailedPreview() {
    BardPersuadeStrumPreview(isSuccessful = false)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeStoppedPreview() {
    BardPersuadeStrumPreview(isSuccessful = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeCharmPreview() {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = CharacterClassType.Bard,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Persuade.Charm(
                id = id,
                bardUserId = PREVIEW_ACTOR_ID,
                attackerClassName = "Rogue",
                defenderClassName = "Mage",
            )
        },
    ) { message ->
        PersuadeOverlay(
            animatingMessage = message,
            bardBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BardPersuadeStrumPreview(isSuccessful: Boolean) {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = CharacterClassType.Bard,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Persuade.StrumLute(
                id = id,
                bardUserId = PREVIEW_ACTOR_ID,
                diceRoll = if (isSuccessful) 5 else 2,
                isSuccessful = isSuccessful,
            )
        },
    ) { message ->
        PersuadeOverlay(
            animatingMessage = message,
            bardBounds = avatars.boundsFor(userId = PREVIEW_ACTOR_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
