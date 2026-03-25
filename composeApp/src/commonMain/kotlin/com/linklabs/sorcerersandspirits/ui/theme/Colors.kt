package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors =
    lightColorScheme(
        primary = Color(color = 0xFF1565C0),
        onPrimary = Color.White,
        background = Color(color = 0xFFFFF8E1),
        onBackground = Color(color = 0xFF0D47A1),
        surface = Color(color = 0xFFFFD54F),
        onSurface = Color.Black,
    )

internal val DarkColors =
    darkColorScheme(
        primary = Color(color = 0xFFD32F2F),
        onPrimary = Color.Black,
        background = Color(color = 0xFF000000),
        onBackground = Color.White,
        surface = Color(color = 0xFF212121),
        onSurface = Color.White,
    )
