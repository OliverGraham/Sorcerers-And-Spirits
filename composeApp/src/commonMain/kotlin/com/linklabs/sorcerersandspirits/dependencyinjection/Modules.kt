package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.LeaveTavernUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import org.koin.dsl.module

val commonModule = module {

    single { SignInAnonymouslyUseCase(authenticationRepository = get()) }
    single {
        UpdateTravelerDisplayNameUseCase(
            authenticationRepository = get(),
            travelerRepository = get(),
        )
    }
    factory {
        AppViewModel(
            signInAnonymouslyUseCase = get(),
            updateTravelerDisplayNameUseCase = get(),
        )
    }

    single { GetTavernFlowUseCase(tavernRepository = get()) }
    single {
        EnterTavernUseCase(
            authenticationRepository = get(),
            tavernRepository = get(),
            travelerRepository = get(),
        )
    }
    single { LeaveTavernUseCase(authenticationRepository = get(), tavernRepository = get()) }

    factory {
        TavernScreenViewModel(
            enterTavernUseCase = get(),
            leaveTavernUseCase = get(),
            updateTravelerDisplayNameUseCase = get(),
            getTavernFlowUseCase = get(),
        )
    }
}
