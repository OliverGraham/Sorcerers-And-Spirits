package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.authentication.IosSocialSignInBridge
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Called in iOSApp.swift. The [socialSignInBridge] is implemented in Swift (GoogleSignIn +
 * AuthenticationServices) and registered here so `IosSocialSignInProvider` can resolve it.
 */
fun initializeKoin(socialSignInBridge: IosSocialSignInBridge) {
    startKoin { modules(module { single { socialSignInBridge } }, commonModule) }
}
