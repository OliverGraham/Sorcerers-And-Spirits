package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/**
 * Returns a copy of this [Typography] with every text style's font size and line height multiplied
 * by [factor], so scaling all Material text app-wide is a single theme-level change. A [factor] of
 * 1 returns the original typography untouched.
 */
internal fun Typography.scaledBy(factor: Float): Typography {
    if (factor == 1f) return this

    return copy(
        displayLarge = displayLarge.scaledBy(factor = factor),
        displayMedium = displayMedium.scaledBy(factor = factor),
        displaySmall = displaySmall.scaledBy(factor = factor),
        headlineLarge = headlineLarge.scaledBy(factor = factor),
        headlineMedium = headlineMedium.scaledBy(factor = factor),
        headlineSmall = headlineSmall.scaledBy(factor = factor),
        titleLarge = titleLarge.scaledBy(factor = factor),
        titleMedium = titleMedium.scaledBy(factor = factor),
        titleSmall = titleSmall.scaledBy(factor = factor),
        bodyLarge = bodyLarge.scaledBy(factor = factor),
        bodyMedium = bodyMedium.scaledBy(factor = factor),
        bodySmall = bodySmall.scaledBy(factor = factor),
        labelLarge = labelLarge.scaledBy(factor = factor),
        labelMedium = labelMedium.scaledBy(factor = factor),
        labelSmall = labelSmall.scaledBy(factor = factor),
    )
}

private fun TextUnit.scaledBy(factor: Float): TextUnit =
    if (type == TextUnitType.Sp) (value * factor).sp else this

private fun TextStyle.scaledBy(factor: Float): TextStyle =
    copy(
        fontSize = fontSize.scaledBy(factor = factor),
        lineHeight = lineHeight.scaledBy(factor = factor),
    )
