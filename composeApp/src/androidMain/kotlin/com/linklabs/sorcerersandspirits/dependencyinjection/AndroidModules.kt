package com.linklabs.sorcerersandspirits.dependencyinjection

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.linklabs.sorcerersandspirits.data.AndroidAuthenticationRepository
import com.linklabs.sorcerersandspirits.data.arena.AndroidArenaRepository
import com.linklabs.sorcerersandspirits.data.profile.AndroidProfileRepository
import com.linklabs.sorcerersandspirits.data.tavern.AndroidTavernRepository
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    initializeDependencies()
    initializeRepositories()
}

private fun Module.initializeDependencies() {
    single { FirebaseFirestore.getInstance() }
    single { FirebaseDatabase.getInstance() }
    single { CoroutineScope(context = Dispatchers.IO) }
}

private fun Module.initializeRepositories() {
    singleOf(::AndroidArenaRepository) { bind<ArenaRepository>() }
    singleOf(::AndroidAuthenticationRepository) { bind<AuthenticationRepository>() }
    singleOf(::AndroidProfileRepository) { bind<ProfileRepository>() }
    singleOf(::AndroidTavernRepository) { bind<TavernRepository>() }
}
