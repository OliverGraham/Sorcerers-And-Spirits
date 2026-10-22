package com.linklabs.sorcerersandspirits.ui.main.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.linklabs.sorcerersandspirits.ui.common.loading.ClassColorGlowBackground
import com.linklabs.sorcerersandspirits.ui.common.loading.SwingingLogoStyle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.link_labs_logo
import sorcerersandspirits.composeapp.generated.resources.sorcerers_and_spirits_full_logo
import sorcerersandspirits.composeapp.generated.resources.splash_link_labs_wordmark
import kotlin.math.PI
import kotlin.math.sin

enum class SplashPhase { LinkLabs, Game }

private const val BUBBLE_RISE_LABEL = "linkLabsBeakerBubbles"
private const val BUBBLE_RISE_PROGRESS_LABEL = "linkLabsBeakerBubbleProgress"
private const val BUBBLE_RISE_DURATION_MILLIS = 2600
private const val BUBBLE_START_Y_FRACTION = 0.62f
private const val BUBBLE_END_Y_FRACTION = 0.25f
private const val BUBBLE_SWAY_CYCLES = 2f
private const val BUBBLE_FADE_IN_FRACTION = 0.12f
private const val BUBBLE_FADE_OUT_FRACTION = 0.25f

private class BeakerBubble(
    val xFraction: Float,
    val radiusFraction: Float,
    val phaseOffset: Float,
    val swayFraction: Float,
)

private val beakerBubbles = listOf(
    BeakerBubble(xFraction = 0.44f, radiusFraction = 0.020f, phaseOffset = 0.00f, swayFraction = 0.010f),
    BeakerBubble(xFraction = 0.47f, radiusFraction = 0.013f, phaseOffset = 0.22f, swayFraction = 0.014f),
    BeakerBubble(xFraction = 0.45f, radiusFraction = 0.024f, phaseOffset = 0.45f, swayFraction = 0.008f),
    BeakerBubble(xFraction = 0.48f, radiusFraction = 0.015f, phaseOffset = 0.63f, swayFraction = 0.012f),
    BeakerBubble(xFraction = 0.46f, radiusFraction = 0.011f, phaseOffset = 0.82f, swayFraction = 0.016f),
)

@Composable
internal fun SplashScreen(phase: SplashPhase, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(color = Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            SplashPhase.LinkLabs -> LinkLabsBeaker()

            SplashPhase.Game -> {
                ClassColorGlowBackground(style = SwingingLogoStyle.Energetic, isVisible = true)
                Image(
                    painter = painterResource(resource = Res.drawable.sorcerers_and_spirits_full_logo),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LinkLabsBeaker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio = 1f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(resource = Res.drawable.link_labs_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            BeakerBubbles(modifier = Modifier.fillMaxSize())
        }
        Text(
            text = stringResource(resource = Res.string.splash_link_labs_wordmark),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeakerBubbles(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = BUBBLE_RISE_LABEL)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BUBBLE_RISE_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = BUBBLE_RISE_PROGRESS_LABEL,
    )

    Canvas(modifier = modifier) {
        beakerBubbles.forEach { bubble ->
            val riseProgress = (progress + bubble.phaseOffset).mod(other = 1f)
            val centerY = lerp(
                start = BUBBLE_START_Y_FRACTION,
                stop = BUBBLE_END_Y_FRACTION,
                fraction = riseProgress,
            )
            val sway = bubble.swayFraction * sin(x = riseProgress * BUBBLE_SWAY_CYCLES * 2f * PI.toFloat())
            val centerX = bubble.xFraction + sway
            val fadeIn = (riseProgress / BUBBLE_FADE_IN_FRACTION).coerceAtMost(maximumValue = 1f)
            val fadeOut = ((1f - riseProgress) / BUBBLE_FADE_OUT_FRACTION).coerceAtMost(maximumValue = 1f)
            val alpha = fadeIn * fadeOut
            val radius = bubble.radiusFraction * (0.7f + 0.3f * riseProgress) * size.width
            val center = Offset(x = centerX * size.width, y = centerY * size.height)

            drawCircle(
                color = Color(color = 0xFFEAF4FB).copy(alpha = alpha * 0.5f),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = radius,
                center = center,
                style = Stroke(width = radius * 0.25f),
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenLinkLabsPreview() {
    SplashScreen(phase = SplashPhase.LinkLabs)
}

@Preview
@Composable
private fun SplashScreenGamePreview() {
    SplashScreen(phase = SplashPhase.Game)
}
