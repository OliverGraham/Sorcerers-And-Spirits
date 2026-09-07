package com.linklabs.sorcerersandspirits.ui.screens.credits.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.MenuTopBar
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsSectionState
import com.linklabs.sorcerersandspirits.ui.screens.credits.CreditsState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.credits_logo

private const val LOGO_SWING_LABEL = "credits_logo_swing"
private const val LOGO_SWING_ROTATION_LABEL = "credits_logo_swing_rotation"
private const val LOGO_SWING_DEGREES = 8f
private const val LOGO_SWING_DURATION_MILLIS = 1600
private const val LOGO_DISPLAY_MILLIS = 20_000L
private const val SCROLL_MILLIS_PER_DP = 12

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
        if (isShowingLogo) {
            SwingingLogo(modifier = Modifier.align(alignment = Alignment.Center))
        } else {
            CreditsRoll(
                state = state,
                scrollOffsetY = scrollOffset.value,
                onContentHeightChange = { contentHeight = it },
            )
        }
    }
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
        painter = painterResource(resource = Res.drawable.credits_logo),
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
