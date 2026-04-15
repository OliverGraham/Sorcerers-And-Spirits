package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.domain.arena.usecase.ConnectToArenaUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val commonModule = module {
    initializeUseCases()
    initializeViewModels()
}

private fun Module.initializeUseCases() {
    singleOf(::ConnectToArenaUseCase)
    singleOf(::ConnectTravelerPresenceUseCase)
    singleOf(::CreateProfileUseCase)
    singleOf(::GetProfileFlowUseCase)
    singleOf(::GetTavernDataFlowUseCase)
    singleOf(::GetUserIdUseCase)
    singleOf(::IsUserSignedInUseCase)
    singleOf(::SetCurrentTravelerAvailableUseCase)
    singleOf(::SendChallengeUseCase)
    singleOf(::UpdateChallengerStatusUseCase)
    singleOf(::UpdateProfileDisplayNameUseCase)
}

private fun Module.initializeViewModels() {
    factoryOf(::AppViewModel)

    // todo: arguments data class
    factory { (arenaId: String) ->
        ArenaScreenViewModel(arenaId = arenaId, connectToArenaUseCase = get())
    }
    factoryOf(::TavernScreenViewModel)
}
