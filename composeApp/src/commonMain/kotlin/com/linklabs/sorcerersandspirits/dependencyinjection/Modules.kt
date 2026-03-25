package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
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
        GetArenaChallengeFlowUseCase(authenticationRepository = get(), tavernRepository = get())
    }
    single { GetTavernFlowUseCase(tavernRepository = get()) }
    single {
        EnterTavernUseCase(
            authenticationRepository = get(),
            tavernRepository = get(),
            travelerRepository = get(),
        )
    }
    single { GetUserIdUseCase(authenticationRepository = get()) }
    single { SendArenaChallengesUseCase(tavernRepository = get()) }
    single { UpdateArenaChallengeStatusUseCase(tavernRepository = get()) }

    factory {
        TavernScreenViewModel(
            enterTavernUseCase = get(),
            updateTravelerDisplayNameUseCase = get(),
            getArenaChallengeFlowUseCase = get(),
            getTavernFlowUseCase = get(),
            getUserIdUseCase = get(),
            sendArenaChallengesUseCase = get(),
            updateArenaChallengeStatusUseCase = get(),
        )
    }
}
