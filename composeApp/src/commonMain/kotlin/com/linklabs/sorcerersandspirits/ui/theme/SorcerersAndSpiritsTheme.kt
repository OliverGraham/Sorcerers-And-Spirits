package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
internal fun SorcerersAndSpiritsTheme(content: @Composable () -> Unit) {
    val screenSizeClass = rememberScreenSizeClass()

    CompositionLocalProvider(LocalScreenSizeClass provides screenSizeClass) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = MaterialTheme.typography.scaledBy(factor = screenSizeClass.textScale()),
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
