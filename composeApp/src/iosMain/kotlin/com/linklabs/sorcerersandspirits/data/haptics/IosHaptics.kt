package com.linklabs.sorcerersandspirits.data.haptics

import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

class IosHaptics : Haptics {

    override fun vibrate(strength: HapticStrength) {
        // A single impact is one smooth tap (no multi-pulse pattern).
        val generator = UIImpactFeedbackGenerator(style = strength.toStyle())
        generator.prepare()
        generator.impactOccurredWithIntensity(intensity = 1.0)
    }

    // A smooth, sustained continuous buzz needs CoreHaptics; not yet supported on iOS (SS-077/078).
    override fun startContinuous(strength: HapticStrength) = Unit

    override fun stopContinuous() = Unit

    private fun HapticStrength.toStyle(): UIImpactFeedbackStyle = when (this) {
        HapticStrength.LIGHT -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        HapticStrength.MEDIUM -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
        HapticStrength.HEAVY -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
    }
}
