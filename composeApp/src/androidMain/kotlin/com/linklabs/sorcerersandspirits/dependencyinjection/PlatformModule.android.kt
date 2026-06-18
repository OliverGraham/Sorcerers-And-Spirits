package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.authentication.AndroidSocialSignInProvider
import com.linklabs.sorcerersandspirits.data.connectivity.AndroidConnectivityObserver
import com.linklabs.sorcerersandspirits.data.haptics.AndroidHaptics
import com.linklabs.sorcerersandspirits.data.sensor.AndroidShakeDetector
import com.linklabs.sorcerersandspirits.data.version.AndroidCurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ConnectivityObserver> { AndroidConnectivityObserver(context = get()) }
    single<ShakeDetector> { AndroidShakeDetector(context = get()) }
    single<Haptics> { AndroidHaptics(context = get()) }
    single<CurrentAppVersionProvider> { AndroidCurrentAppVersionProvider(context = get()) }
    single<SocialSignInProvider> { AndroidSocialSignInProvider() }
}
