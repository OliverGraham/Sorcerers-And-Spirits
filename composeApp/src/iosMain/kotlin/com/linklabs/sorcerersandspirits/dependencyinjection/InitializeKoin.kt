package com.linklabs.sorcerersandspirits.dependencyinjection

import org.koin.core.context.startKoin

/** Called in iOSApp.swift */
fun initializeKoin() {
    startKoin { modules(commonModule) }
}
