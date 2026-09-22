package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

@Composable
fun <T> DissolveStateContent(
    targetState: T,
    contentKey: (T) -> Any,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val contentLayer = rememberGraphicsLayer()
    var displayedState by remember { mutableStateOf(targetState) }
    var isCapturingContent by remember { mutableStateOf(value = false) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(value = null) }

    LaunchedEffect(key1 = targetState) {
        val isNewContent = contentKey(targetState) != contentKey(displayedState)
        if (isNewContent) {
            isCapturingContent = true
            withFrameNanos {}
            withFrameNanos {}
            snapshot = contentLayer.toImageBitmap()
            isCapturingContent = false
        }
        displayedState = targetState
    }

    Box(modifier = modifier) {
        Box(
            modifier =
            Modifier.fillMaxSize().drawWithContent {
                val isCapturing = isCapturingContent
                if (isCapturing) {
                    contentLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer = contentLayer)
                } else {
                    drawContent()
                }
            },
        ) {
            content(displayedState)
        }

        val currentSnapshot = snapshot
        if (currentSnapshot != null) {
            ScreenDissolveEffect(
                snapshot = currentSnapshot,
                onComplete = { snapshot = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
