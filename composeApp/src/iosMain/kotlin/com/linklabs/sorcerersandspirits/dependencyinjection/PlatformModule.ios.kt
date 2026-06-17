package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.connectivity.IosConnectivityObserver
import com.linklabs.sorcerersandspirits.data.haptics.IosHaptics
import com.linklabs.sorcerersandspirits.data.sensor.IosShakeDetector
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ConnectivityObserver> { IosConnectivityObserver() }
    single<ShakeDetector> { IosShakeDetector() }
    single<Haptics> { IosHaptics() }
}
