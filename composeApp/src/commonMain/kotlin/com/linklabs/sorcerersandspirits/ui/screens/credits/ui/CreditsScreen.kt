package com.linklabs.sorcerersandspirits.ui.screens.credits.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.MenuTopBar
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyGradients
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsSectionState
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.credits_legal_copyright
import sorcerersandspirits.composeapp.generated.resources.credits_legal_trademarks
import sorcerersandspirits.composeapp.generated.resources.credits_legal_wordmark
import sorcerersandspirits.composeapp.generated.resources.sorcerers_and_spirits_small_logo

private const val LOGO_SWING_LABEL = "credits_logo_swing"
private const val LOGO_SWING_ROTATION_LABEL = "credits_logo_swing_rotation"
private const val LOGO_SWING_DEGREES = 8f
private const val LOGO_SWING_DURATION_MILLIS = 1600
private const val LOGO_DISPLAY_MILLIS = 20_000L
private const val SCROLL_MILLIS_PER_DP = 12
private const val PHASE_CROSSFADE_LABEL = "credits_phase_crossfade"
private const val PHASE_CROSSFADE_MILLIS = 1200
private const val GLOW_LABEL = "credits_glow"
private const val GLOW_COLOR_LABEL = "credits_glow_color"
private const val GLOW_PULSE_LABEL = "credits_glow_pulse"
private const val GLOW_VISIBILITY_LABEL = "credits_glow_visibility"
private const val GLOW_CYCLE_MILLIS = 24_000
private const val GLOW_PULSE_MILLIS = 3200
private const val GLOW_MIN_ALPHA = 0.35f
private const val GLOW_MAX_ALPHA = 0.7f

@Composable
internal fun CreditsScreen(
    viewModel: CreditsScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()

    Scaffold(modifier = modifier, topBar = { MenuTopBar(title = "Credits") }) { paddingValues ->
        CreditsScreen(
            uiState = uiState,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(paddingValues = paddingValues),
        )
    }
}

@Composable
private fun CreditsScreen(
    uiState: UiState<CreditsState>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)
        is UiState.Loaded -> CreditsContent(state = uiState.data, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@Composable
private fun CreditsContent(state: CreditsState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    var viewportHeight by remember { mutableStateOf(value = 0) }
    var contentHeight by remember { mutableStateOf(value = 0) }
    val scrollOffset = remember { Animatable(initialValue = 0f) }
    var isShowingLogo by remember { mutableStateOf(value = false) }

    LaunchedEffect(key1 = viewportHeight, key2 = contentHeight) {
        val hasMeasured = viewportHeight > 0 && contentHeight > 0
        if (!hasMeasured) return@LaunchedEffect

        val distanceDp = (viewportHeight + contentHeight) / density
        val scrollDurationMillis = (distanceDp * SCROLL_MILLIS_PER_DP).toInt()
        while (true) {
            isShowingLogo = false
            scrollOffset.snapTo(targetValue = viewportHeight.toFloat())
            scrollOffset.animateTo(
                targetValue = -contentHeight.toFloat(),
                animationSpec = tween(durationMillis = scrollDurationMillis, easing = LinearEasing),
            )
            isShowingLogo = true
            delay(timeMillis = LOGO_DISPLAY_MILLIS)
        }
    }

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewportHeight = it.height },
    ) {
        ClassColorGlowBackground(isVisible = isShowingLogo)
        Crossfade(
            targetState = isShowingLogo,
            animationSpec = tween(durationMillis = PHASE_CROSSFADE_MILLIS),
            label = PHASE_CROSSFADE_LABEL,
            modifier = Modifier.fillMaxSize(),
        ) { showingLogo ->
            if (showingLogo) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SwingingLogo()
                }
            } else {
                CreditsRoll(
                    state = state,
                    scrollOffsetY = scrollOffset.value,
                    onContentHeightChange = { contentHeight = it },
                )
            }
        }
    }
}

@Composable
private fun ClassColorGlowBackground(isVisible: Boolean, modifier: Modifier = Modifier) {
    val gradients = remember { classFamilyGradients() }
    val transition = rememberInfiniteTransition(label = GLOW_LABEL)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = gradients.size.toFloat(),
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = GLOW_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = GLOW_COLOR_LABEL,
    )
    val pulse by transition.animateFloat(
        initialValue = GLOW_MIN_ALPHA,
        targetValue = GLOW_MAX_ALPHA,
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = GLOW_PULSE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = GLOW_PULSE_LABEL,
    )
    val visibility by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = PHASE_CROSSFADE_MILLIS),
        label = GLOW_VISIBILITY_LABEL,
    )

    val segment = phase.toInt() % gradients.size
    val fraction = phase - phase.toInt()
    val from = gradients[segment]
    val to = gradients[(segment + 1) % gradients.size]
    val innerColor = lerp(start = from.topStop, stop = to.topStop, fraction = fraction)
    val outerColor = lerp(start = from.bottomStop, stop = to.bottomStop, fraction = fraction)

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = visibility * pulse }
            .background(brush = Brush.radialGradient(colors = listOf(innerColor, outerColor))),
    )
}

@Composable
private fun CreditsRoll(
    state: CreditsState,
    scrollOffsetY: Float,
    onContentHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .onSizeChanged { onContentHeightChange(it.height) }
            .graphicsLayer { translationY = scrollOffsetY }
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 40.dp),
    ) {
        state.sections.forEach { section -> CreditsSection(section = section) }
        LegalFooter()
    }
}

@Composable
private fun LegalFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Text(
            text = stringResource(resource = Res.string.credits_legal_wordmark),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(resource = Res.string.credits_legal_copyright),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(resource = Res.string.credits_legal_trademarks),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreditsSection(section: CreditsSectionState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        section.entries.forEach { entry ->
            Text(
                text = entry,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SwingingLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = LOGO_SWING_LABEL)
    val rotation by transition.animateFloat(
        initialValue = -LOGO_SWING_DEGREES,
        targetValue = LOGO_SWING_DEGREES,
        animationSpec =
        infiniteRepeatable(
            animation =
            tween(durationMillis = LOGO_SWING_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = LOGO_SWING_ROTATION_LABEL,
    )

    Image(
        painter = painterResource(resource = Res.drawable.sorcerers_and_spirits_small_logo),
        contentDescription = "Sorcerers and Spirits logo",
        modifier =
        modifier
            .size(size = 200.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                rotationZ = rotation
            },
    )
}

@Preview
@Composable
private fun CreditsScreenPreview() {
    SorcerersAndSpiritsTheme {
        CreditsScreen(
            uiState =
            UiState.Loaded(
                data =
                CreditsState(
                    sections =
                    listOf(
                        CreditsSectionState(
                            title = "Sorcerers & Spirits",
                            entries = listOf("A dice-combat game"),
                        ),
                        CreditsSectionState(
                            title = "Design & Development",
                            entries = listOf("Oliver Graham"),
                        ),
                        CreditsSectionState(
                            title = "Special Thanks",
                            entries = listOf("The Playtesters", "Family & Friends"),
                        ),
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
