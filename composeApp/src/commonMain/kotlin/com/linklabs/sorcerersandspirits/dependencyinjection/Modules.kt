package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetActiveChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernTravelersFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
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
        AppViewModel(signInAnonymouslyUseCase = get(), updateTravelerDisplayNameUseCase = get())
    }

    single {
        GetActiveChallengeFlowUseCase(authenticationRepository = get(), tavernRepository = get())
    }
    single { GetTavernTravelersFlowUseCase(tavernRepository = get()) }
    single {
        EnterTavernUseCase(
            authenticationRepository = get(),
            tavernRepository = get(),
            travelerRepository = get(),
        )
    }
    single { GetUserIdUseCase(authenticationRepository = get()) }
    single { SendChallengeUseCase(tavernRepository = get()) }
    single { UpdateChallengerStatusUseCase(tavernRepository = get()) }
    single {
        SetCurrentTravelerAvailableUseCase(
            authenticationRepository = get(),
            tavernRepository = get(),
        )
    }

    factory {
        TavernScreenViewModel(
            enterTavernUseCase = get(),
            updateTravelerDisplayNameUseCase = get(),
            getActiveChallengeFlowUseCase = get(),
            getTavernTravelersFlowUseCase = get(),
            getUserIdUseCase = get(),
            sendChallengeUseCase = get(),
            updateChallengerStatusUseCase = get(),
            setCurrentTravelerAvailableUseCase = get(),
        )
    }
}
