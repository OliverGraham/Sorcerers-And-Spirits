package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.random.Random

internal const val DISSOLVE_DURATION_MILLIS = 900f
internal const val DISSOLVE_NANOS_PER_MILLI = 1_000_000f

sealed interface ScreenDissolveStyle {
    data object Disintegrate : ScreenDissolveStyle
    data object Shatter : ScreenDissolveStyle
    data object Smoke : ScreenDissolveStyle
    data object Slash : ScreenDissolveStyle

    companion object {
        val all: List<ScreenDissolveStyle> = listOf(Disintegrate, Shatter, Smoke, Slash)
    }
}

@Composable
fun ScreenDissolveEffect(
    snapshot: ImageBitmap,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = remember(key1 = snapshot) { ScreenDissolveStyle.all.random() }
    var progress by remember(key1 = snapshot) { mutableFloatStateOf(value = 0f) }

    LaunchedEffect(key1 = snapshot) {
        var startNanos = 0L
        do {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) {
                    startNanos = frameNanos
                }
                val elapsedMillis = (frameNanos - startNanos) / DISSOLVE_NANOS_PER_MILLI
                progress =
                    (elapsedMillis / DISSOLVE_DURATION_MILLIS).coerceIn(
                        minimumValue = 0f,
                        maximumValue = 1f,
                    )
            }
        } while (progress < 1f)

        onComplete()
    }

    when (style) {
        is ScreenDissolveStyle.Disintegrate ->
            DisintegrateDissolve(snapshot = snapshot, progress = progress, modifier = modifier)

        is ScreenDissolveStyle.Shatter ->
            ShatterDissolve(snapshot = snapshot, progress = progress, modifier = modifier)

        is ScreenDissolveStyle.Smoke ->
            SmokeDissolve(snapshot = snapshot, progress = progress, modifier = modifier)

        is ScreenDissolveStyle.Slash ->
            SlashDissolve(snapshot = snapshot, progress = progress, modifier = modifier)
    }
}

internal fun randomBetween(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)
