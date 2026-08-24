package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_oil_barrel

internal const val ALL_IN_TOSS_MILLIS = 1100
internal const val ALL_IN_SETTLE_MILLIS = 300
private const val ALL_IN_EXPLODE_MILLIS = 680
private const val ALL_IN_GRAY_MILLIS = 500
private const val ALL_IN_HOLD_MILLIS = 480L
private const val ALL_IN_CASH_MILLIS = 1300
private const val ALL_IN_FADE_MILLIS = 360

private const val COIN_COUNT = 8
private const val BILL_COUNT = 4
private const val GEM_COUNT = 3
private const val PAINTING_COUNT = 2
private const val BARREL_COUNT = 1
private const val DOLLAR_SIGN_COUNT = 6
private const val DOLLAR_MEASURE_SP = 24

private val coinFace = Color(color = 0xFFF4C752)
private val coinRim = Color(color = 0xFF9A6B1E)
private val coinShine = Color(color = 0xFFFBE9B0)
private val grayFace = Color(color = 0xFF9C9C9C)
private val grayRim = Color(color = 0xFF5C5C5C)
private val grayShine = Color(color = 0xFFCFCFCF)
private val gemColors =
    listOf(
        Color(color = 0xFF4FC3F7),
        Color(color = 0xFFE573B5),
        Color(color = 0xFF7BE38B),
        Color(color = 0xFFE05B5B),
    )
private val billBody = Color(color = 0xFF6BAE83)
private val billInk = Color(color = 0xFF2F5E42)
private val frameGold = Color(color = 0xFFC9972F)
private val canvasColors =
    listOf(
        Color(color = 0xFF6A8FC0),
        Color(color = 0xFFB5623F),
        Color(color = 0xFF6FAE7A),
        Color(color = 0xFFB59B4A),
    )
private val barrelBody = Color(color = 0xFF3A3A42)
private val barrelBand = Color(color = 0xFF8A8F98)
private val grayBarrelBody = Color(color = 0xFFBDBDBD)
private val grayBarrelBand = Color(color = 0xFFE0E0E0)
private val dollarSignColor = Color(color = 0xFFF4C752)
private val blastCore = Color(color = 0xFFFFF3D0)

@Composable
internal fun AllInOverlay(
    animatingMessage: ArenaMessageState.AllIn,
    avatarBounds: Rect,
    modifier: Modifier = Modifier,
    accentColor: Color = animatingMessage.actingClassType.toColor(),
) {
    val state = remember { AllInEffectState() }
    val density = LocalDensity.current
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        state.play(
            scene =
                allInScene(
                    bounds = avatarBounds,
                    accentColor = accentColor,
                    density = density,
                    seed = animatingMessage.id.hashCode(),
                ),
            outcome = animatingMessage.outcome,
        )

        onAvatarAnimationComplete()
    }

    val dollarLayout = getRememberedDollarLayout()
    val barrelPainter = painterResource(resource = Res.drawable.ic_oil_barrel)
    Canvas(modifier = modifier) {
        drawAllIn(state = state, dollarLayout = dollarLayout, barrelPainter = barrelPainter)
    }
}

private fun allInScene(bounds: Rect, accentColor: Color, density: Density, seed: Int): AllInScene {
    val random = Random(seed = seed)
    val width = bounds.width
    val height = bounds.height
    val center = bounds.center
    val pileCenter = Offset(x = center.x, y = bounds.bottom - height * 0.08f)
    val pileHalfWidth = width * 0.3f

    val explodeDistance = with(density) { 96.dp.toPx() }

    val valuables =
        valuableKinds()
            .shuffled(random = random)
            .map { kind ->
                val angle = random.nextFloat(min = 0f, max = (2f * PI).toFloat())
                val (color, accent) = valuableColors(kind = kind, random = random)
                Valuable(
                    kind = kind,
                    color = color,
                    accent = accent,
                    launchStart =
                        center +
                            Offset(
                                x = random.nextFloat(min = -0.16f, max = 0.16f) * width,
                                y = random.nextFloat(min = -0.06f, max = 0.14f) * height,
                            ),
                    apex =
                        Offset(
                            x = center.x + random.nextFloat(min = -0.42f, max = 0.42f) * width,
                            y = bounds.top + random.nextFloat(min = 0f, max = 0.18f) * height,
                        ),
                    rest =
                        Offset(
                            x =
                                pileCenter.x +
                                    random.nextFloat(min = -1f, max = 1f) * pileHalfWidth,
                            y = pileCenter.y - random.nextFloat(min = 0f, max = 0.1f) * height,
                        ),
                    explodeDir = Offset(x = cos(angle), y = sin(angle)),
                    radiusPx = with(density) { valuableRadiusDp(kind = kind).dp.toPx() },
                    spinTurns = random.nextInt(from = 2, until = 4).toFloat(),
                )
            }
            .sortedBy { it.kind }

    val dollarSigns =
        (0 until DOLLAR_SIGN_COUNT).map { index ->
            DollarSign(
                x = pileCenter.x + random.nextFloat(min = -0.9f, max = 0.9f) * pileHalfWidth,
                baseY = pileCenter.y,
                risePx = height * random.nextFloat(min = 0.3f, max = 0.5f),
                bobPx = with(density) { random.nextFloat(min = 5f, max = 11f).dp.toPx() },
                sizePx = with(density) { random.nextFloat(min = 18f, max = 28f).dp.toPx() },
                phase = index.toFloat() / DOLLAR_SIGN_COUNT,
            )
        }

    return AllInScene(
        valuables = valuables,
        dollarSigns = dollarSigns,
        pileCenter = pileCenter,
        accentColor = accentColor,
        explodeDistancePx = explodeDistance,
    )
}

private suspend fun AllInEffectState.play(scene: AllInScene, outcome: AllInOutcome) {
    toss.snapTo(targetValue = 0f)
    explode.snapTo(targetValue = 0f)
    gray.snapTo(targetValue = 0f)
    cash.snapTo(targetValue = 0f)
    fade.snapTo(targetValue = 1f)
    this.scene = scene

    toss.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = ALL_IN_TOSS_MILLIS, easing = LinearEasing),
    )
    delay(timeMillis = ALL_IN_SETTLE_MILLIS.toLong())

    when (outcome) {
        AllInOutcome.SelfKnockout ->
            explode.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = ALL_IN_EXPLODE_MILLIS, easing = FastOutLinearInEasing),
            )

        AllInOutcome.NoEffect -> {
            gray.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ALL_IN_GRAY_MILLIS),
            )
            delay(timeMillis = ALL_IN_HOLD_MILLIS)
            fade.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = ALL_IN_FADE_MILLIS),
            )
        }

        AllInOutcome.DoubleAttack -> {
            cash.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ALL_IN_CASH_MILLIS),
            )
            fade.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = ALL_IN_FADE_MILLIS),
            )
        }
    }

    this.scene = null
}

@Composable
private fun getRememberedDollarLayout(): TextLayoutResult {
    val textMeasurer = rememberTextMeasurer()
    return remember(key1 = textMeasurer) {
        textMeasurer.measure(
            text = "$",
            style = TextStyle(fontSize = DOLLAR_MEASURE_SP.sp, fontWeight = FontWeight.Black),
        )
    }
}

private fun DrawScope.drawAllIn(
    state: AllInEffectState,
    dollarLayout: TextLayoutResult,
    barrelPainter: Painter,
) {
    val scene = state.scene ?: return
    val toss = state.toss.value
    val explode = state.explode.value
    val gray = state.gray.value
    val cash = state.cash.value
    val fade = state.fade.value

    val valuableAlpha = fade * (1f - explode)
    if (valuableAlpha > 0.01f) {
        scene.valuables.forEach { valuable ->
            val arc =
                quadraticBezier(
                    start = valuable.launchStart,
                    control = valuable.apex,
                    end = valuable.rest,
                    fraction = toss,
                )
            val position = arc + valuable.explodeDir * (explode * scene.explodeDistancePx)
            val spinDegrees = (toss * valuable.spinTurns + explode * 1.5f) * 360f
            drawValuable(
                valuable = valuable,
                center = position,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = valuableAlpha,
                barrelPainter = barrelPainter,
            )
        }
    }

    if (explode > 0f) {
        drawBlast(
            center = scene.pileCenter,
            explode = explode,
            accentColor = scene.accentColor,
            reach = scene.explodeDistancePx,
        )
    }

    if (cash > 0f) {
        scene.dollarSigns.forEach { sign ->
            drawDollarSign(sign = sign, cash = cash, layout = dollarLayout)
        }
    }
}

private fun DrawScope.drawValuable(
    valuable: Valuable,
    center: Offset,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
    barrelPainter: Painter,
) {
    when (valuable.kind) {
        ValuableKind.Coin ->
            drawCoin(
                center = center,
                radius = valuable.radiusPx,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = alpha,
            )

        ValuableKind.Gem ->
            drawGem(
                center = center,
                radius = valuable.radiusPx,
                baseColor = valuable.color,
                accent = valuable.accent,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = alpha,
            )

        ValuableKind.Bill ->
            drawBill(
                center = center,
                radius = valuable.radiusPx,
                body = valuable.color,
                ink = valuable.accent,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = alpha,
            )

        ValuableKind.Painting ->
            drawPainting(
                center = center,
                radius = valuable.radiusPx,
                frame = valuable.color,
                canvas = valuable.accent,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = alpha,
            )

        ValuableKind.Barrel ->
            drawBarrel(
                center = center,
                radius = valuable.radiusPx,
                barrelPainter = barrelPainter,
                gray = gray,
                spinDegrees = spinDegrees,
                alpha = alpha,
            )
    }
}

private fun DrawScope.drawCoin(
    center: Offset,
    radius: Float,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
) {
    val face = lerp(start = coinFace, stop = grayFace, fraction = gray)
    val rim = lerp(start = coinRim, stop = grayRim, fraction = gray)
    val shine = lerp(start = coinShine, stop = grayShine, fraction = gray)

    val flip = abs(cos(spinDegrees * PI.toFloat() / 180f))
    val halfWidth = radius * (0.2f + 0.8f * flip)
    val topLeft = Offset(x = center.x - halfWidth, y = center.y - radius)
    val ovalSize = Size(width = halfWidth * 2f, height = radius * 2f)

    drawOval(color = face, topLeft = topLeft, size = ovalSize, alpha = alpha)
    drawOval(
        color = rim,
        topLeft = topLeft,
        size = ovalSize,
        style = Stroke(width = radius * 0.18f),
        alpha = alpha,
    )
    drawOval(
        color = shine,
        topLeft = Offset(x = center.x - halfWidth * 0.45f, y = center.y - radius * 0.55f),
        size = Size(width = halfWidth * 0.55f, height = radius * 0.4f),
        alpha = alpha * 0.8f,
    )
}

private fun DrawScope.drawGem(
    center: Offset,
    radius: Float,
    baseColor: Color,
    accent: Color,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
) {
    val color = lerp(start = baseColor, stop = grayFace, fraction = gray)
    rotate(degrees = spinDegrees, pivot = center) {
        val diamond =
            Path().apply {
                moveTo(x = center.x, y = center.y - radius)
                lineTo(x = center.x + radius * 0.7f, y = center.y)
                lineTo(x = center.x, y = center.y + radius)
                lineTo(x = center.x - radius * 0.7f, y = center.y)
                close()
            }
        drawPath(path = diamond, color = color, alpha = alpha)
        drawPath(
            path = diamond,
            color = lerp(start = accent, stop = grayShine, fraction = gray),
            style = Stroke(width = radius * 0.14f),
            alpha = alpha * 0.5f,
        )
    }
}

private fun DrawScope.drawBill(
    center: Offset,
    radius: Float,
    body: Color,
    ink: Color,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
) {
    val bodyColor = lerp(start = body, stop = grayFace, fraction = gray)
    val inkColor = lerp(start = ink, stop = grayRim, fraction = gray)
    val flip = 0.35f + 0.65f * abs(cos(spinDegrees * PI.toFloat() / 180f))

    rotate(degrees = spinDegrees * 0.25f, pivot = center) {
        val halfWidth = radius * flip
        val halfHeight = radius * 0.5f
        val topLeft = Offset(x = center.x - halfWidth, y = center.y - halfHeight)
        val billSize = Size(width = halfWidth * 2f, height = halfHeight * 2f)
        drawRect(color = bodyColor, topLeft = topLeft, size = billSize, alpha = alpha)
        drawRect(
            color = inkColor,
            topLeft = topLeft,
            size = billSize,
            style = Stroke(width = radius * 0.1f),
            alpha = alpha,
        )
        drawCircle(color = inkColor, radius = halfHeight * 0.42f, center = center, alpha = alpha)
    }
}

private fun DrawScope.drawPainting(
    center: Offset,
    radius: Float,
    frame: Color,
    canvas: Color,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
) {
    val frameColor = lerp(start = frame, stop = grayRim, fraction = gray)
    val canvasColor = lerp(start = canvas, stop = grayFace, fraction = gray)

    rotate(degrees = spinDegrees, pivot = center) {
        val halfWidth = radius * 0.85f
        drawRect(
            color = frameColor,
            topLeft = Offset(x = center.x - halfWidth, y = center.y - radius),
            size = Size(width = halfWidth * 2f, height = radius * 2f),
            alpha = alpha,
        )
        val inset = radius * 0.24f
        drawRect(
            color = canvasColor,
            topLeft = Offset(x = center.x - halfWidth + inset, y = center.y - radius + inset),
            size = Size(width = (halfWidth - inset) * 2f, height = (radius - inset) * 2f),
            alpha = alpha,
        )
        drawLine(
            color = frameColor,
            start = Offset(x = center.x - halfWidth * 0.4f, y = center.y + radius * 0.3f),
            end = Offset(x = center.x + halfWidth * 0.4f, y = center.y - radius * 0.1f),
            strokeWidth = radius * 0.12f,
            alpha = alpha * 0.7f,
        )
    }
}

private fun DrawScope.drawBarrel(
    center: Offset,
    radius: Float,
    barrelPainter: Painter,
    gray: Float,
    spinDegrees: Float,
    alpha: Float,
) {
    val currentBlack = lerp(start = barrelBody, stop = grayBarrelBody, fraction = gray)
    val currentWhite = lerp(start = barrelBand, stop = grayBarrelBand, fraction = gray)

    val matrix =
        ColorMatrix(
            floatArrayOf(
                currentWhite.red - currentBlack.red,
                0f,
                0f,
                0f,
                currentBlack.red,
                0f,
                currentWhite.green - currentBlack.green,
                0f,
                0f,
                currentBlack.green,
                0f,
                0f,
                currentWhite.blue - currentBlack.blue,
                0f,
                currentBlack.blue,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
        )
    val colorFilter = ColorFilter.colorMatrix(colorMatrix = matrix)

    rotate(degrees = spinDegrees, pivot = center) {
        val halfWidth = radius * 0.7f
        val size = Size(width = halfWidth * 2f, height = radius * 2f)
        translate(left = center.x - halfWidth, top = center.y - radius) {
            with(barrelPainter) { draw(size = size, alpha = alpha, colorFilter = colorFilter) }
        }
    }
}

private fun DrawScope.drawBlast(center: Offset, explode: Float, accentColor: Color, reach: Float) {
    val radius = reach * (0.45f + explode)
    val fade = 1f - explode
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(blastCore, accentColor.copy(alpha = 0.75f), Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = fade,
    )
    drawCircle(
        color = accentColor,
        radius = reach * explode * 1.15f,
        center = center,
        style = Stroke(width = 3.dp.toPx()),
        alpha = fade * 0.8f,
    )
}

private fun DrawScope.drawDollarSign(sign: DollarSign, cash: Float, layout: TextLayoutResult) {
    val rise = (cash / 0.3f).coerceAtMost(maximumValue = 1f)
    val bob = sin((cash * 3f + sign.phase) * 2f * PI.toFloat()) * sign.bobPx * rise
    val fadeOut = 1f - ((cash - 0.8f) / 0.2f).coerceIn(minimumValue = 0f, maximumValue = 1f)
    val alpha = rise * fadeOut
    if (alpha <= 0.01f) return

    val x = sign.x
    val y = sign.baseY - sign.risePx * rise + bob
    val glyphScale = sign.sizePx / layout.size.height
    scale(scale = glyphScale, pivot = Offset(x = x, y = y)) {
        drawText(
            textLayoutResult = layout,
            color = dollarSignColor,
            topLeft = Offset(x = x - layout.size.width / 2f, y = y - layout.size.height / 2f),
            alpha = alpha,
        )
    }
}

private fun quadraticBezier(start: Offset, control: Offset, end: Offset, fraction: Float): Offset {
    val inverse = 1f - fraction
    return start * (inverse * inverse) +
        control * (2f * inverse * fraction) +
        end * (fraction * fraction)
}

private fun Random.nextFloat(min: Float, max: Float): Float = nextFloat() * (max - min) + min

private fun valuableKinds(): List<ValuableKind> = buildList {
    repeat(times = COIN_COUNT) { add(element = ValuableKind.Coin) }
    repeat(times = BILL_COUNT) { add(element = ValuableKind.Bill) }
    repeat(times = GEM_COUNT) { add(element = ValuableKind.Gem) }
    repeat(times = PAINTING_COUNT) { add(element = ValuableKind.Painting) }
    repeat(times = BARREL_COUNT) { add(element = ValuableKind.Barrel) }
}

private fun valuableColors(kind: ValuableKind, random: Random): Pair<Color, Color> =
    when (kind) {
        ValuableKind.Coin -> coinFace to coinRim
        ValuableKind.Gem -> gemColors.random(random = random) to coinShine
        ValuableKind.Bill -> billBody to billInk
        ValuableKind.Painting -> frameGold to canvasColors.random(random = random)
        ValuableKind.Barrel -> barrelBody to barrelBand
    }

private fun valuableRadiusDp(kind: ValuableKind): Float =
    when (kind) {
        ValuableKind.Coin -> 8f
        ValuableKind.Gem -> 7f
        ValuableKind.Bill -> 10f
        ValuableKind.Painting -> 12f
        ValuableKind.Barrel -> 17f
    }

enum class ValuableKind {
    Coin,
    Gem,
    Bill,
    Painting,
    Barrel,
}

@Immutable
internal data class Valuable(
    val kind: ValuableKind,
    val color: Color,
    val accent: Color,
    val launchStart: Offset,
    val apex: Offset,
    val rest: Offset,
    val explodeDir: Offset,
    val radiusPx: Float,
    val spinTurns: Float,
)

@Immutable
internal data class DollarSign(
    val x: Float,
    val baseY: Float,
    val risePx: Float,
    val bobPx: Float,
    val sizePx: Float,
    val phase: Float,
)

@Immutable
internal data class AllInScene(
    val valuables: List<Valuable>,
    val dollarSigns: List<DollarSign>,
    val pileCenter: Offset,
    val accentColor: Color,
    val explodeDistancePx: Float,
)

@Stable
internal class AllInEffectState {
    val toss = Animatable(initialValue = 0f)
    val explode = Animatable(initialValue = 0f)
    val gray = Animatable(initialValue = 0f)
    val cash = Animatable(initialValue = 0f)
    val fade = Animatable(initialValue = 1f)

    var scene by mutableStateOf<AllInScene?>(value = null)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerAllInDoubleAttackPreview() {
    GamblerAllInPreview(outcome = AllInOutcome.DoubleAttack)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerAllInSelfKnockoutPreview() {
    GamblerAllInPreview(outcome = AllInOutcome.SelfKnockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerAllInNoEffectPreview() {
    GamblerAllInPreview(outcome = AllInOutcome.NoEffect)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistAllInPreview() {
    GamblerAllInPreview(
        outcome = AllInOutcome.DoubleAttack,
        accentClass = CharacterClassType.Illusionist,
    )
}

@Composable
private fun GamblerAllInPreview(
    outcome: AllInOutcome,
    accentClass: CharacterClassType = CharacterClassType.Gambler,
) {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = accentClass,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.AllIn(
                id = id,
                actingClassType = accentClass,
                gamblerUserId = PREVIEW_ACTOR_ID,
                gamblerName = "Gambler",
                singleDie =
                    when (outcome) {
                        AllInOutcome.SelfKnockout -> 1
                        AllInOutcome.NoEffect -> 3
                        AllInOutcome.DoubleAttack -> 6
                    },
                outcome = outcome,
                animation = ArenaAnimationState.FlashAndAvatar(),
                gestures = emptyMap(),
            )
        },
    ) { message ->
        AllInOverlay(
            animatingMessage = message,
            avatarBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            accentColor = accentClass.toColor(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
