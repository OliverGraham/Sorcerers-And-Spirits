package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Scales the entire app from one place. Bending the ambient [Density] by the screen size's
 * [dpScale] makes every dp — and every sp, since sp resolves through density — shrink or grow
 * together, so no call site needs its own scaling. [fontScale] is divided back out and re-applied
 * at the stronger [textScale] so copy still scales harder than dp chrome. The window bucket is read
 * before the override, from the real density, so the scaled density can never feed back into it.
 */
@Composable
internal fun SorcerersAndSpiritsTheme(content: @Composable () -> Unit) {
    val screenSizeClass = rememberScreenSizeClass()
    val baseDensity = LocalDensity.current

    val scaledDensity =
        remember(baseDensity, screenSizeClass) {
            val dpScale = screenSizeClass.dpScale()
            Density(
                density = baseDensity.density * dpScale,
                fontScale = baseDensity.fontScale * screenSizeClass.textScale() / dpScale,
            )
        }

    CompositionLocalProvider(
        LocalScreenSizeClass provides screenSizeClass,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
