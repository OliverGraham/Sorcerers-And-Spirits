package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.TavernRepositoryDataSource
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.GetTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import org.koin.dsl.module

val commonModule = module {

    single { SignInAnonymouslyUseCase(authenticationRepository = get()) }
    factory { AppViewModel(signInAnonymouslyUseCase = get()) }

    single<TavernRepository> { TavernRepositoryDataSource() }
    single<GetTavernUseCase> { GetTavernUseCase(tavernRepository = get()) }
    factory {
        TavernScreenViewModel(
            getTavernUseCase = get(),
        )
    }
}
