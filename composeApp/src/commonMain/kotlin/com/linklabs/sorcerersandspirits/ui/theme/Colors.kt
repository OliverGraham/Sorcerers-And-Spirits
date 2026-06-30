package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFC62828), // Deep Crimson
        onPrimary = Color.White,
        primaryContainer = Color(0xFF8E0000),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFFB300), // Rich Gold
        onSecondary = Color.Black,
        background = Color(0xFF121212), // Deep Grey
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF1E1E1E), // Slightly lighter grey
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF37474F), // Blue-Grey for depth
        onSurfaceVariant = Color(0xFFB0BEC5),
        outline = Color(0xFF455A64),
        outlineVariant = Color(0xFF263238),
    )
