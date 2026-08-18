package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.GestureClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.visibleAvatarWidth
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PORTAL_OPEN_MILLIS = 420
private const val SWIRL_PERIOD_MILLIS = 2600
private const val SWALLOW_MILLIS = 300
private const val PORTAL_CLOSE_MILLIS = 300
private const val PORTAL_SNAP_MILLIS = 200
private const val GRAY_BLOOM_MILLIS = 260
private const val GRAY_FADE_MILLIS = 460

private const val SPIRAL_TURNS = 1.35f

private const val PORTAL_RADIUS_X_FRACTION = 0.3f
private const val PORTAL_RADIUS_Y_FRACTION = 0.36f
private const val PORTAL_SIDE_OFFSET_FRACTION = 0.52f
private const val PORTAL_HEIGHT_FRACTION = 0.3f

private const val GRAY_WASH_PEAK_ALPHA = 0.55f

private val PORTAL_CORE = Color(color = 0xFFF2FBFF)
private val PORTAL_RIM = Color(color = 0xFF8FE0FF)
private val PORTAL_GLOW = Color(color = 0xFF3AA0FF)
private val PORTAL_SPIRAL = Color(color = 0xFF6FC8FF)
private val PORTAL_VOID = Color(color = 0xFF081733)
private val PORTAL_GRAY = Color(color = 0xFF9AA0A6)

@Composable
internal fun MageTeleportOverlay(
    animatingMessage: ArenaMessageState.Teleport,
    mageBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { TeleportPortalState() }
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val scene = teleportScene(mageBounds = mageBounds)
        state.play(scene = scene, message = animatingMessage)

        onAvatarAnimationComplete()
    }

    Canvas(modifier = modifier) { drawTeleport(state = state) }
}

private fun teleportScene(mageBounds: Rect): TeleportScene {
    val avatarWidth = mageBounds.visibleAvatarWidth()
    val radiusX = avatarWidth * PORTAL_RADIUS_X_FRACTION
    val radiusY = avatarWidth * PORTAL_RADIUS_Y_FRACTION
    val center =
        Offset(
            x = mageBounds.center.x + avatarWidth * PORTAL_SIDE_OFFSET_FRACTION,
            y = mageBounds.top + mageBounds.height * PORTAL_HEIGHT_FRACTION,
        )
    return TeleportScene(center = center, radiusX = radiusX, radiusY = radiusY)
}

private suspend fun TeleportPortalState.play(
    scene: TeleportScene,
    message: ArenaMessageState.Teleport,
) {
    when (message) {
        is ArenaMessageState.Teleport.Trigger -> open(scene = scene)

        is ArenaMessageState.Teleport.Roll,
        is ArenaMessageState.Teleport.Tie -> hold(scene = scene)

        is ArenaMessageState.Teleport.Result ->
            if (message.didDefenderEscape) {
                resolveEscape(scene = scene)
            } else {
                resolveFail(scene = scene)
            }
    }
}

private suspend fun TeleportPortalState.open(scene: TeleportScene) {
    this.scene = scene
    open.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)
    swallow.snapTo(targetValue = 0f)
    gray.snapTo(targetValue = 0f)

    open.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PORTAL_OPEN_MILLIS, easing = FastOutSlowInEasing),
    )
    swirlForever()
}

private suspend fun TeleportPortalState.hold(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)
    swirlForever()
}

private suspend fun TeleportPortalState.swirlForever() {
    while (true) {
        swirl.animateTo(
            targetValue = swirl.value + 1f,
            animationSpec = tween(durationMillis = SWIRL_PERIOD_MILLIS, easing = LinearEasing),
        )
    }
}

private suspend fun TeleportPortalState.resolveEscape(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)

    coroutineScope {
        launch {
            swallow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SWALLOW_MILLIS),
            )
            swallow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PORTAL_CLOSE_MILLIS),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = swirl.value + 1.2f,
                animationSpec =
                    tween(
                        durationMillis = SWALLOW_MILLIS + PORTAL_CLOSE_MILLIS,
                        easing = LinearEasing,
                    ),
            )
        }
        launch {
            delay(timeMillis = SWALLOW_MILLIS.toLong())
            open.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = PORTAL_CLOSE_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }

    this.scene = null
}

private suspend fun TeleportPortalState.resolveFail(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)

    coroutineScope {
        launch {
            open.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = PORTAL_SNAP_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = swirl.value + 0.6f,
                animationSpec = tween(durationMillis = PORTAL_SNAP_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            gray.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GRAY_BLOOM_MILLIS),
            )
        }
    }
    gray.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = GRAY_FADE_MILLIS))

    this.scene = null
}

private fun DrawScope.drawTeleport(state: TeleportPortalState) {
    val scene = state.scene ?: return

    val open = state.open.value
    if (open > 0f) {
        drawPortal(
            scene = scene,
            open = open,
            swirl = state.swirl.value,
            swallow = state.swallow.value,
        )
    }

    val gray = state.gray.value
    if (gray > 0f) {
        drawRect(color = PORTAL_GRAY.copy(alpha = gray * GRAY_WASH_PEAK_ALPHA), size = size)
    }
}

private fun DrawScope.drawPortal(scene: TeleportScene, open: Float, swirl: Float, swallow: Float) {
    val center = scene.center
    val radius = scene.radiusX * open
    val yScale = scene.radiusY / scene.radiusX

    scale(scaleX = 1f, scaleY = yScale, pivot = center) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(PORTAL_GLOW.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = radius * 1.3f,
                ),
            radius = radius * 1.3f,
            center = center,
        )
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(PORTAL_VOID, PORTAL_GLOW.copy(alpha = 0.85f)),
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
        for (strand in 0 until 3) {
            drawSpiralStrand(
                center = center,
                radius = radius,
                swirl = swirl + strand / 3f,
                alpha = 0.6f * open,
            )
        }
        drawCircle(
            color = PORTAL_RIM.copy(alpha = open),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.12f),
        )
        for (spark in 0 until 5) {
            val angle = (swirl * 2f + spark / 5f) * 2f * PI.toFloat()
            val orbit = radius * 0.72f
            drawCircle(
                color = PORTAL_CORE.copy(alpha = 0.85f * open),
                radius = radius * 0.05f,
                center = center + Offset(x = cos(angle) * orbit, y = sin(angle) * orbit),
            )
        }
        if (swallow > 0f) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(PORTAL_CORE.copy(alpha = swallow), Color.Transparent),
                        center = center,
                        radius = radius * (0.4f + 0.6f * swallow),
                    ),
                radius = radius * (0.4f + 0.6f * swallow),
                center = center,
            )
        }
    }
}

private fun DrawScope.drawSpiralStrand(center: Offset, radius: Float, swirl: Float, alpha: Float) {
    val steps = 56
    val path = Path()
    for (step in 0..steps) {
        val fraction = step / steps.toFloat()
        val angle = (fraction * SPIRAL_TURNS + swirl) * 2f * PI.toFloat()
        val strandRadius = fraction * radius
        val point = center + Offset(x = cos(angle) * strandRadius, y = sin(angle) * strandRadius)
        if (step == 0) {
            path.moveTo(x = point.x, y = point.y)
        } else {
            path.lineTo(x = point.x, y = point.y)
        }
    }
    drawPath(
        path = path,
        color = PORTAL_SPIRAL.copy(alpha = alpha),
        style = Stroke(width = radius * 0.05f),
    )
}

@Stable
private class TeleportPortalState {
    val open = Animatable(initialValue = 0f)
    val swirl = Animatable(initialValue = 0f)
    val swallow = Animatable(initialValue = 0f)
    val gray = Animatable(initialValue = 0f)

    var scene by mutableStateOf<TeleportScene?>(value = null)
}

@Immutable
private data class TeleportScene(val center: Offset, val radiusX: Float, val radiusY: Float)

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageTeleportEscapePreview() {
    MageTeleportPreview(didEscape = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageTeleportFailPreview() {
    MageTeleportPreview(didEscape = false)
}

@Composable
private fun MageTeleportPreview(didEscape: Boolean) {
    val avatars = rememberSoloAvatar()
    val bounds = avatars.boundsFor(PREVIEW_ACTOR_ID)
    val density = LocalDensity.current
    val classState = previewCharacterClassState(type = CharacterClassType.Mage)
    AnimationPreview(
        accentClass = CharacterClassType.Mage,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Teleport.Result(
                id = id,
                mageUserId = PREVIEW_ACTOR_ID,
                didDefenderEscape = didEscape,
                animation = ArenaAnimationState.FlashAndAvatar,
            )
        },
    ) { message ->
        GestureClassAvatar(
            animatingMessage = message,
            isTopSeat = false,
            userId = PREVIEW_ACTOR_ID,
            characterClass = classState,
            modifier =
                Modifier.offset(
                        x = with(density) { bounds.left.toDp() },
                        y = with(density) { bounds.top.toDp() },
                    )
                    .size(
                        width = with(density) { bounds.width.toDp() },
                        height = with(density) { bounds.height.toDp() },
                    ),
        )
        MageTeleportOverlay(
            animatingMessage = message,
            mageBounds = bounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
