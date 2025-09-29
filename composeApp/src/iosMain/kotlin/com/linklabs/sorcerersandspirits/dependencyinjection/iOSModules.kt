package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import org.koin.dsl.module

val iosModule = module {
    single<AuthenticationRepository> { IOSAuthenticationRepository() }
}