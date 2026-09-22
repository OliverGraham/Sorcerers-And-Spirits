package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class ScreenDissolveController(
    private val scope: CoroutineScope,
    val screenLayer: GraphicsLayer,
) {
    var snapshot: ImageBitmap? by mutableStateOf(value = null)
        private set

    var isCapturingScreen: Boolean by mutableStateOf(value = false)
        private set

    fun captureThenNavigate(navigate: () -> Unit) {
        scope.launch {
            isCapturingScreen = true
            withFrameNanos {}
            withFrameNanos {}
            snapshot = screenLayer.toImageBitmap()
            isCapturingScreen = false
            navigate()
        }
    }

    fun onDissolveComplete() {
        snapshot = null
    }
}

@Composable
internal fun rememberScreenDissolveController(): ScreenDissolveController {
    val scope = rememberCoroutineScope()
    val screenLayer = rememberGraphicsLayer()
    return remember { ScreenDissolveController(scope = scope, screenLayer = screenLayer) }
}
