package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.AndroidAuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import org.koin.dsl.module

val androidModule = module {
    single<AuthenticationRepository> { AndroidAuthenticationRepository() }
}
