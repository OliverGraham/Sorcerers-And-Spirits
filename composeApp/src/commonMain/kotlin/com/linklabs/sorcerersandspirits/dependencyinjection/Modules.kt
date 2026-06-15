package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.arena.FirebaseArenaRepository
import com.linklabs.sorcerersandspirits.data.authentication.FirebaseAuthenticationRepository
import com.linklabs.sorcerersandspirits.data.profile.FirebaseProfileRepository
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.TavernDataSource
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.RemoveArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// TODO(SS-055): Someday, scope these instead of single for everything

val commonModule = module {
    includes(platformModule())
    initializeParameters()
    initializeRepositories()
    initializeUseCases()
    initializeViewModels()
}

// TODO(SS-056): What to name this?
private fun Module.initializeParameters() {
    single { Firebase.database }
    single { Firebase.firestore }
    single { CoroutineScope(context = Dispatchers.IO) }
}

private fun Module.initializeRepositories() {
    singleOf(::FirebaseArenaRepository) { bind<ArenaRepository>() }
    singleOf(::FirebaseAuthenticationRepository) { bind<AuthenticationRepository>() }
    singleOf(::FirebaseProfileRepository) { bind<ProfileRepository>() }
    singleOf(::FirebaseTavernDataSource) { bind<TavernDataSource>() }
    singleOf(::FirebaseTavernRepository) { bind<TavernRepository>() }
}

private fun Module.initializeUseCases() {
    singleOf(::ChooseCharacterClassUseCase)
    singleOf(::CoordinateArenaMatchUseCase)
    singleOf(::ConnectTravelerPresenceUseCase)
    singleOf(::CreateProfileUseCase)
    singleOf(::ExitArenaUseCase)
    singleOf(::GetProfileFlowUseCase)
    singleOf(::GetTavernDataFlowUseCase)
    singleOf(::GetUserIdUseCase)
    singleOf(::IsUserSignedInUseCase)
    singleOf(::PerformCombatActionUseCase)
    singleOf(::RemoveArenaMatchUseCase)
    singleOf(::SetCurrentTravelerAvailableUseCase)
    singleOf(::SendChallengeUseCase)
    singleOf(::StayForAnotherMatchUseCase)
    singleOf(::UpdateChallengerStatusUseCase)
    singleOf(::UpdateProfileDisplayNameUseCase)
}

private fun Module.initializeViewModels() {
    viewModelOf(::AppViewModel)
    viewModelOf(::ArenaScreenViewModel)
    viewModelOf(::TavernScreenViewModel)
}
