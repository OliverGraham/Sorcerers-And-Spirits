package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.EncorePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloSeat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.bard_avatar
import sorcerersandspirits.composeapp.generated.resources.duelist_avatar
import sorcerersandspirits.composeapp.generated.resources.gambler_avatar
import sorcerersandspirits.composeapp.generated.resources.illusionist_avatar
import sorcerersandspirits.composeapp.generated.resources.mage_avatar
import sorcerersandspirits.composeapp.generated.resources.necromancer_avatar
import sorcerersandspirits.composeapp.generated.resources.paladin_avatar
import sorcerersandspirits.composeapp.generated.resources.rogue_avatar

private const val DARKNESS_IN_MILLIS = 620
private const val SPOTLIGHT_IN_MILLIS = 520
private const val STAGE_HOLD_MILLIS = 260L
private const val ROLL_MILLIS = 620
private const val REACTION_MILLIS = 760
private const val GRAY_MILLIS = 500
private const val LIGHTS_UP_MILLIS = 640

private const val SWAY_CYCLE_MILLIS = 2600
private const val GLINT_CYCLE_MILLIS = 1900

private const val ENCORE_AMBIENT_LABEL = "encore-ambient"
private const val ENCORE_SWAY_LABEL = "encore-sway"
private const val ENCORE_GLINT_LABEL = "encore-glint"

@Immutable internal data class EncoreScene(val accentColor: Color, val isSuccessful: Boolean)

@Stable
internal class EncoreEffectState {
    val darkness = Animatable(initialValue = 0f)
    val spotlight = Animatable(initialValue = 0f)
    val roll = Animatable(initialValue = 0f)
    val reaction = Animatable(initialValue = 0f)
    val grayness = Animatable(initialValue = 0f)

    var scene by mutableStateOf<EncoreScene?>(value = null)
}

@Composable
internal fun EncoreOverlay(
    animatingMessage: ArenaMessageState.Encore,
    modifier: Modifier = Modifier,
) {
    val state = remember { EncoreEffectState() }
    val crowd = remember { encoreCrowd() }
    val avatars = encoreCrowdAvatars()


    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        state.play(message = animatingMessage, accentColor = animatingMessage.actingClassType.toColor())
        onAvatarAnimationComplete()
    }

    val ambient = rememberInfiniteTransition(label = ENCORE_AMBIENT_LABEL)
    val sway by
    ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = SWAY_CYCLE_MILLIS, easing = LinearEasing)
            ),
        label = ENCORE_SWAY_LABEL,
    )
    val glint by
    ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLINT_CYCLE_MILLIS, easing = LinearEasing)
            ),
        label = ENCORE_GLINT_LABEL,
    )
    Canvas(modifier = modifier) {
        drawEncore(state = state, crowd = crowd, avatars = avatars, sway = sway, glint = glint)
    }
}

internal fun encoreDurationMillis(message: ArenaMessageState.Encore) =
    when (message) {
        is ArenaMessageState.Encore.Roll ->
            ROLL_MILLIS.toLong() + REACTION_MILLIS.toLong() + LIGHTS_UP_MILLIS.toLong()
        is ArenaMessageState.Encore.Trigger -> DARKNESS_IN_MILLIS.toLong() + STAGE_HOLD_MILLIS
    }

@Composable
private fun encoreCrowdAvatars(): List<ImageBitmap> =
    listOf(
        imageResource(resource = Res.drawable.rogue_avatar),
        imageResource(resource = Res.drawable.gambler_avatar),
        imageResource(resource = Res.drawable.duelist_avatar),
        imageResource(resource = Res.drawable.necromancer_avatar),
        imageResource(resource = Res.drawable.bard_avatar),
        imageResource(resource = Res.drawable.illusionist_avatar),
        imageResource(resource = Res.drawable.mage_avatar),
        imageResource(resource = Res.drawable.paladin_avatar),
    )

private suspend fun EncoreEffectState.play(message: ArenaMessageState.Encore, accentColor: Color) {
    when (message) {
        is ArenaMessageState.Encore.Roll -> resolve(accentColor = accentColor, isSuccessful = message.isSuccessful)
        is ArenaMessageState.Encore.Trigger -> setStage(accentColor = accentColor)
    }
}

private suspend fun EncoreEffectState.setStage(accentColor: Color) {
    resetResolveChannels()
    darkness.snapTo(targetValue = 0f)
    spotlight.snapTo(targetValue = 0f)
    scene = EncoreScene(accentColor = accentColor, isSuccessful = false)

    coroutineScope {
        launch {
            darkness.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = DARKNESS_IN_MILLIS),
            )
        }
        launch {
            spotlight.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SPOTLIGHT_IN_MILLIS),
            )
        }
    }
}

private suspend fun EncoreEffectState.resolve(accentColor: Color, isSuccessful: Boolean) {
    scene = EncoreScene(accentColor = accentColor, isSuccessful = isSuccessful)
    darkness.snapTo(targetValue = 1f)
    spotlight.snapTo(targetValue = 1f)
    resetResolveChannels()

    roll.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = ROLL_MILLIS))

    coroutineScope {
        launch {
            reaction.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = REACTION_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        if (!isSuccessful) {
            launch {
                grayness.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = GRAY_MILLIS),
                )
            }
        }
    }

    liftLights()
}

private suspend fun EncoreEffectState.liftLights() {
    coroutineScope {
        launch {
            darkness.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = LIGHTS_UP_MILLIS),
            )
        }
        launch {
            spotlight.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = LIGHTS_UP_MILLIS),
            )
        }
    }
    scene = null
}

private suspend fun EncoreEffectState.resetResolveChannels() {
    roll.snapTo(targetValue = 0f)
    reaction.snapTo(targetValue = 0f)
    grayness.snapTo(targetValue = 0f)
}

private fun DrawScope.drawEncore(
    state: EncoreEffectState,
    crowd: List<EncoreCrowdMember>,
    avatars: List<ImageBitmap>,
    sway: Float,
    glint: Float,
) {
    val scene = state.scene ?: return
    val darkness = state.darkness.value
    val grayness = state.grayness.value

    drawEncoreScrim(darkness = darkness)
    drawEncoreCrowd(
        crowd = crowd,
        avatars = avatars,
        sway = sway,
        reaction = state.reaction.value,
        isSuccessful = scene.isSuccessful,
        darkness = darkness,
    )
    drawEncoreStage(alpha = darkness)
    drawEncoreSpotlight(spotlight = state.spotlight.value)
    drawMicrophone(glint = glint, grayness = grayness, alpha = darkness)
    drawEncoreMusic(
        accentColor = scene.accentColor,
        roll = state.roll.value,
        grayness = grayness,
        alpha = darkness,
    )
    drawEncoreVignette(darkness = darkness)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardEncoreSuccessPreview() {
    BardEncorePreview(resolution = EncorePhase.Success)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardEncoreFailurePreview() {
    BardEncorePreview(resolution = EncorePhase.Failure)
}

@Composable
private fun BardEncorePreview(resolution: EncorePhase) {
    val seats = rememberSoloSeat()
    val cue =
        rememberReplayingCues(replayKey = resolution) { iteration ->
            listOf(
                encoreCue(iteration = iteration, phase = EncorePhase.SetStage),
                encoreCue(iteration = iteration, phase = resolution),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Bard, seats = seats) {
        EncoreOverlay(animationCue = cue, modifier = Modifier.fillMaxSize())
    }
}

private fun encoreCue(iteration: Int, phase: EncorePhase): CombatAnimationCue.Encore =
    CombatAnimationCue.Encore(
        id = "encore-$phase-$iteration",
        bardUserId = "bardUserId",
        phase = phase,
    )
