package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF283593), // Richer Indigo
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8EAF6),
        onPrimaryContainer = Color(0xFF1A237E),
        secondary = Color(0xFFE65100), // Richer Orange/Amber
        onSecondary = Color.White,
        background = Color(0xFFFFFBE6), // Warm Parchment
        onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFF3E5AB), // Vanilla/Light Gold for contrast
        onSurfaceVariant = Color(0xFF4E342E),
        outline = Color(0xFF795548),
        outlineVariant = Color(0xFFD7CCC8),
    )

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
