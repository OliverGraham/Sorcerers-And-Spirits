package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.connectivity.IosConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ConnectivityObserver> { IosConnectivityObserver() }
}
