package com.linklabs.dependencyinjection

import com.linklabs.data.TavernRepositoryDataSource
import com.linklabs.domain.tavern.GetTavernUseCase
import com.linklabs.domain.tavern.TavernRepository
import com.linklabs.ui.main.AppViewModel
import com.linklabs.ui.screens.tavern.TavernScreenViewModel
import org.koin.dsl.module

val commonModule = module {

    factory { AppViewModel() }

    single<TavernRepository> { TavernRepositoryDataSource() }
    single<GetTavernUseCase> { GetTavernUseCase(tavernRepository = get()) }
    factory {
        TavernScreenViewModel(
            getTavernUseCase = get(),
        )
    }
}
