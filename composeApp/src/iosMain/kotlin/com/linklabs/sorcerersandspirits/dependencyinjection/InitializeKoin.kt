package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initializeKoin(
    authenticationRepository: AuthenticationRepository,
) {
    val iosModule = module {
        single<AuthenticationRepository> { authenticationRepository }
    }

    startKoin {
        modules(commonModule, iosModule)
    }
}
