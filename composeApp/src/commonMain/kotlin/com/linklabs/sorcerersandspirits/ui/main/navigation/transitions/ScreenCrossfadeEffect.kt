package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal const val CROSSFADE_DURATION_MILLIS = 220f

@Composable
fun ScreenCrossfadeEffect(
    snapshot: ImageBitmap,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    (elapsedMillis / CROSSFADE_DURATION_MILLIS).coerceIn(
                        minimumValue = 0f,
                        maximumValue = 1f,
                    )
            }
        } while (progress < 1f)

        onComplete()
    }

    Canvas(modifier = modifier) {
        drawImage(
            image = snapshot,
            srcOffset = IntOffset(x = 0, y = 0),
            srcSize = IntSize(width = snapshot.width, height = snapshot.height),
            dstOffset = IntOffset(x = 0, y = 0),
            dstSize = IntSize(width = size.width.toInt(), height = size.height.toInt()),
            alpha = 1f - progress,
        )
    }
}
