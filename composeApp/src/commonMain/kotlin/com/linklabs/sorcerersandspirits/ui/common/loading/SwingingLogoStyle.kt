package com.linklabs.sorcerersandspirits.ui.common.loading

sealed interface SwingingLogoStyle {
    val swingDurationMillis: Int
    val swingDegrees: Float
    val glowPulseDurationMillis: Int
    val glowMinAlpha: Float
    val glowMaxAlpha: Float
    val hasLightning: Boolean

    data object Energetic : SwingingLogoStyle {
        override val swingDurationMillis = 900
        override val swingDegrees = 11f
        override val glowPulseDurationMillis = 1400
        override val glowMinAlpha = 0.55f
        override val glowMaxAlpha = 1f
        override val hasLightning = true
    }

    data object Mellow : SwingingLogoStyle {
        override val swingDurationMillis = 1600
        override val swingDegrees = 8f
        override val glowPulseDurationMillis = 3200
        override val glowMinAlpha = 0.35f
        override val glowMaxAlpha = 0.7f
        override val hasLightning = false
    }
}
