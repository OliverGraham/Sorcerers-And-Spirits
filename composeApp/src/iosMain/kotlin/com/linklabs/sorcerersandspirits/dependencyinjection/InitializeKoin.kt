package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Called in iOSApp.swift */
fun initializeKoin(
    authenticationRepository: AuthenticationRepository,
    tavernRepository: TavernRepository,
    travelerRepository: TravelerRepository,
) {
    val iosModule = module {
        single<AuthenticationRepository> { authenticationRepository }
        single<TavernRepository> { tavernRepository }
        single<TravelerRepository> { travelerRepository }
    }

    startKoin { modules(commonModule, iosModule) }
}
