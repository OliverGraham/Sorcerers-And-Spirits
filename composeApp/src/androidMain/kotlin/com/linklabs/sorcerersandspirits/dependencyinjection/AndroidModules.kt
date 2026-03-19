package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.AndroidAuthenticationRepository
import com.linklabs.sorcerersandspirits.data.AndroidTravelerRepository
import com.linklabs.sorcerersandspirits.data.tavern.AndroidTavernRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository
import org.koin.dsl.module

val androidModule = module {
    single<AuthenticationRepository> { AndroidAuthenticationRepository() }
    single<TavernRepository> { AndroidTavernRepository() }
    single<TravelerRepository> { AndroidTravelerRepository() }
}
