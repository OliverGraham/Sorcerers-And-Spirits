package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toBustResource
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.visibleAvatarWidth
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val RISE_MILLIS = 420
private const val EMPTY_HOLD_MILLIS = 260L
private const val SELF_FADE_MILLIS = 420
private const val SELF_HOLD_MILLIS = 420L
private const val MORPH_MILLIS = 720
private const val FINAL_HOLD_MILLIS = 520L
private const val LOWER_MILLIS = 360

private const val BLUR_COPY_COUNT = 6

private val glassCore = Color(color = 0xFF2A3B54)
private val glassEdge = Color(color = 0xFF0E1626)
private val rimLight = Color(color = 0xFFDCE8F7)
private val rimMid = Color(color = 0xFF7C97BD)
private val rimDark = Color(color = 0xFF34496B)
private val handleLight = Color(color = 0xFF9FB4D2)
private val handleDark = Color(color = 0xFF243651)

@Immutable
internal data class ImpersonateScene(
    val glassCenter: Offset,
    val glassRadiusX: Float,
    val glassRadiusY: Float,
    val rimWidth: Float,
    val handle: Rect,
    val riseDistancePx: Float,
)

@Stable
internal class ImpersonateEffectState {
    val rise = Animatable(initialValue = 0f)
    val selfReveal = Animatable(initialValue = 0f)
    val morph = Animatable(initialValue = 0f)

    var scene by mutableStateOf<ImpersonateScene?>(value = null)
}

@Composable
internal fun ImpersonateOverlay(
    animatingMessage: ArenaMessageState.Impersonate,
    avatarBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { ImpersonateEffectState() }
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        state.play(scene = impersonateScene(bounds = avatarBounds))
        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = animatingMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    val selfImage = imageResource(resource = CharacterClassType.Illusionist.toBustResource())
    val impersonatedImage =
        imageResource(resource = animatingMessage.impersonatedClassType.toBustResource())
    Canvas(modifier = modifier) {
        drawImpersonate(state = state, selfImage = selfImage, impersonatedImage = impersonatedImage)
    }
}

private suspend fun ImpersonateEffectState.play(scene: ImpersonateScene) {
    rise.snapTo(targetValue = 0f)
    selfReveal.snapTo(targetValue = 0f)
    morph.snapTo(targetValue = 0f)
    this.scene = scene

    rise.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = RISE_MILLIS, easing = FastOutSlowInEasing),
    )
    delay(timeMillis = EMPTY_HOLD_MILLIS)

    selfReveal.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = SELF_FADE_MILLIS))
    delay(timeMillis = SELF_HOLD_MILLIS)

    morph.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MORPH_MILLIS, easing = LinearEasing),
    )
    delay(timeMillis = FINAL_HOLD_MILLIS)

    rise.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = LOWER_MILLIS, easing = FastOutSlowInEasing),
    )

    this.scene = null
}

private fun impersonateScene(bounds: Rect): ImpersonateScene {
    val glassRadiusX = bounds.visibleAvatarWidth() * 0.30f
    val glassRadiusY = glassRadiusX * 1.30f
    val rimWidth = glassRadiusX * 0.16f
    val glassCenter = Offset(x = bounds.center.x, y = bounds.center.y - bounds.height * 0.08f)

    val handleWidth = glassRadiusX * 0.34f
    val handleTop = glassCenter.y + glassRadiusY + rimWidth * 0.2f
    val handleLength = glassRadiusY * 1.5f
    val handle =
        Rect(
            left = glassCenter.x - handleWidth / 2f,
            top = handleTop,
            right = glassCenter.x + handleWidth / 2f,
            bottom = handleTop + handleLength,
        )

    return ImpersonateScene(
        glassCenter = glassCenter,
        glassRadiusX = glassRadiusX,
        glassRadiusY = glassRadiusY,
        rimWidth = rimWidth,
        handle = handle,
        riseDistancePx = bounds.height * 0.55f,
    )
}

private fun DrawScope.drawImpersonate(
    state: ImpersonateEffectState,
    selfImage: ImageBitmap,
    impersonatedImage: ImageBitmap,
) {
    val scene = state.scene ?: return
    val present = state.rise.value
    if (present <= 0.01f) return

    val lower = (1f - present) * scene.riseDistancePx
    translate(top = lower) {
        drawMirrorHandle(scene = scene, alpha = present)
        drawMirrorGlass(scene = scene, alpha = present)
        drawReflections(
            scene = scene,
            selfImage = selfImage,
            impersonatedImage = impersonatedImage,
            selfReveal = state.selfReveal.value,
            morph = state.morph.value,
            alpha = present,
        )
        drawGlassSheen(scene = scene, alpha = present)
        drawMirrorRim(scene = scene, alpha = present)
    }
}

private fun DrawScope.drawMirrorHandle(scene: ImpersonateScene, alpha: Float) {
    val handle = scene.handle
    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(handleLight, handleDark)),
        topLeft = handle.topLeft,
        size = handle.size,
        cornerRadius = CornerRadius(x = handle.width / 2f),
        alpha = alpha,
    )

    val collarWidth = scene.handle.width * 1.9f
    val collarHeight = scene.rimWidth * 1.8f
    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(rimLight, rimDark)),
        topLeft =
        Offset(
            x = scene.glassCenter.x - collarWidth / 2f,
            y = scene.handle.top - collarHeight * 0.5f,
        ),
        size = Size(width = collarWidth, height = collarHeight),
        cornerRadius = CornerRadius(x = collarHeight / 2f),
        alpha = alpha,
    )
}

private fun DrawScope.drawMirrorGlass(scene: ImpersonateScene, alpha: Float) {
    clipPath(path = ovalPath(scene = scene)) {
        drawRect(
            brush =
            Brush.radialGradient(
                colors = listOf(glassCore, glassEdge),
                center = scene.glassCenter,
                radius = scene.glassRadiusY,
            ),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawReflections(
    scene: ImpersonateScene,
    selfImage: ImageBitmap,
    impersonatedImage: ImageBitmap,
    selfReveal: Float,
    morph: Float,
    alpha: Float,
) {
    if (selfReveal <= 0.01f) return

    val blurPx = sin(x = (morph * PI).toFloat()) * scene.glassRadiusX * 0.14f
    clipPath(path = ovalPath(scene = scene)) {
        drawFace(
            image = selfImage,
            scene = scene,
            alpha = alpha * selfReveal * (1f - morph),
            blurPx = blurPx,
        )
        drawFace(
            image = impersonatedImage,
            scene = scene,
            alpha = alpha * selfReveal * morph,
            blurPx = blurPx,
        )
    }
}

private fun DrawScope.drawFace(
    image: ImageBitmap,
    scene: ImpersonateScene,
    alpha: Float,
    blurPx: Float,
) {
    if (alpha <= 0.01f) return

    val topLeft =
        Offset(
            x = scene.glassCenter.x - scene.glassRadiusX,
            y = scene.glassCenter.y - scene.glassRadiusY,
        )
    val size =
        IntSize(
            width = (scene.glassRadiusX * 2f).roundToInt(),
            height = (scene.glassRadiusY * 2f).roundToInt(),
        )

    if (blurPx <= 0.5f) {
        drawFaceAt(image = image, topLeft = topLeft, size = size, alpha = alpha)
        return
    }

    val copyAlpha = (alpha * 1.5f / BLUR_COPY_COUNT).coerceAtMost(maximumValue = alpha)
    for (index in 0 until BLUR_COPY_COUNT) {
        val angle = (index.toFloat() / BLUR_COPY_COUNT) * 2f * PI.toFloat()
        val jitter = Offset(x = cos(x = angle) * blurPx, y = sin(x = angle) * blurPx)
        drawFaceAt(image = image, topLeft = topLeft + jitter, size = size, alpha = copyAlpha)
    }
}

private fun DrawScope.drawFaceAt(image: ImageBitmap, topLeft: Offset, size: IntSize, alpha: Float) {
    drawImage(
        image = image,
        dstOffset = IntOffset(x = topLeft.x.roundToInt(), y = topLeft.y.roundToInt()),
        dstSize = size,
        alpha = alpha,
    )
}

private fun DrawScope.drawGlassSheen(scene: ImpersonateScene, alpha: Float) {
    clipPath(path = ovalPath(scene = scene)) {
        val highlightCenter =
            Offset(
                x = scene.glassCenter.x - scene.glassRadiusX * 0.35f,
                y = scene.glassCenter.y - scene.glassRadiusY * 0.4f,
            )
        drawCircle(
            brush =
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                center = highlightCenter,
                radius = scene.glassRadiusX * 0.9f,
            ),
            radius = scene.glassRadiusX * 0.9f,
            center = highlightCenter,
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawMirrorRim(scene: ImpersonateScene, alpha: Float) {
    val rimRadiusX = scene.glassRadiusX + scene.rimWidth / 2f
    val rimRadiusY = scene.glassRadiusY + scene.rimWidth / 2f
    val topLeft = Offset(x = scene.glassCenter.x - rimRadiusX, y = scene.glassCenter.y - rimRadiusY)
    val size = Size(width = rimRadiusX * 2f, height = rimRadiusY * 2f)

    drawOval(
        brush = Brush.sweepGradient(colors = listOf(rimDark, rimLight, rimMid, rimLight, rimDark)),
        topLeft = topLeft,
        size = size,
        style = Stroke(width = scene.rimWidth),
        alpha = alpha,
    )
    drawOval(
        color = rimLight,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = scene.rimWidth * 0.25f),
        alpha = alpha * 0.6f,
    )
}

private fun ovalPath(scene: ImpersonateScene): Path = Path().apply {
    addOval(
        oval =
        Rect(
            offset =
            Offset(
                x = scene.glassCenter.x - scene.glassRadiusX,
                y = scene.glassCenter.y - scene.glassRadiusY,
            ),
            size = Size(width = scene.glassRadiusX * 2f, height = scene.glassRadiusY * 2f),
        ),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun ImpersonateRoguePreview() {
    ImpersonatePreview(impersonatedClassType = CharacterClassType.Rogue)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun ImpersonateNecromancerPreview() {
    ImpersonatePreview(impersonatedClassType = CharacterClassType.Necromancer)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun ImpersonateDuelistPreview() {
    ImpersonatePreview(impersonatedClassType = CharacterClassType.Duelist)
}

@Composable
private fun ImpersonatePreview(impersonatedClassType: CharacterClassType) {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = CharacterClassType.Illusionist,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Impersonate(
                id = id,
                illusionistUserId = PREVIEW_ACTOR_ID,
                illusionistName = "Illusionist",
                impersonatedClassType = impersonatedClassType,
                impersonatedAbilityName = impersonatedClassType.name,
            )
        },
    ) { message ->
        ImpersonateOverlay(
            animatingMessage = message,
            avatarBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
