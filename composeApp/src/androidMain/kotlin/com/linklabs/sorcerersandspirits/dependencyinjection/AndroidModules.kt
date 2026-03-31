package com.linklabs.sorcerersandspirits.dependencyinjection

import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.AndroidAuthenticationRepository
import com.linklabs.sorcerersandspirits.data.AndroidTravelerRepository
import com.linklabs.sorcerersandspirits.data.tavern.AndroidTavernRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository
import org.koin.dsl.module

val androidModule = module {
    single<AuthenticationRepository> { AndroidAuthenticationRepository() }
    single { FirebaseDatabase.getInstance() }
    single<TavernRepository> { AndroidTavernRepository(database = get()) }
    single<TravelerRepository> { AndroidTravelerRepository() }
}
