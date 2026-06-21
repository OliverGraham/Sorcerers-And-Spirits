package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.arena.FirebaseArenaRepository
import com.linklabs.sorcerersandspirits.data.authentication.FirebaseAuthenticationRepository
import com.linklabs.sorcerersandspirits.data.config.FirebaseRemoteConfigDataSource
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigCharacterClassProvider
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigDataSource
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigRandomNameProvider
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigSupportedAppVersionProvider
import com.linklabs.sorcerersandspirits.data.profile.FirebaseProfileRepository
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.TavernDataSource
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.LinkAccountUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignInWithSocialUseCase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassProvider
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.leaderboard.usecase.GetLeaderboardUseCase
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.RandomNameProvider
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileForCurrentUserUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.DeleteProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetActiveChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.version.SupportedAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.version.usecase.CheckAppVersionUseCase
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.main.ChallengeViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.GameRulesScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.LeaderboardScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    includes(platformModule())
    initializeExternalDependencies()
    initializeRepositories()
    initializeUseCases()
    initializeViewModels()
}

private fun Module.initializeExternalDependencies() {
    single { Firebase.database }
    single { Firebase.firestore }
    single { Firebase.remoteConfig }
    single { CoroutineScope(context = Dispatchers.IO) }
}

private fun Module.initializeRepositories() {
    singleOf(::FirebaseArenaRepository) { bind<ArenaRepository>() }
    singleOf(::FirebaseAuthenticationRepository) { bind<AuthenticationRepository>() }
    singleOf(::FirebaseRemoteConfigDataSource) { bind<RemoteConfigDataSource>() }
    singleOf(::RemoteConfigCharacterClassProvider) { bind<CharacterClassProvider>() }
    singleOf(::RemoteConfigRandomNameProvider) { bind<RandomNameProvider>() }
    singleOf(::RemoteConfigSupportedAppVersionProvider) { bind<SupportedAppVersionProvider>() }
    singleOf(::FirebaseProfileRepository) { bind<ProfileRepository>() }
    singleOf(::FirebaseTavernDataSource) { bind<TavernDataSource>() }
    singleOf(::FirebaseTavernRepository) { bind<TavernRepository>() }
}

private fun Module.initializeUseCases() {
    factoryOf(::ChooseCharacterClassUseCase)
    factoryOf(::CheckAppVersionUseCase)
    factoryOf(::CoordinateArenaMatchUseCase)
    factoryOf(::ConnectTravelerPresenceUseCase)
    factoryOf(::CreateProfileForCurrentUserUseCase)
    factoryOf(::CreateProfileUseCase)
    factoryOf(::DeleteProfileUseCase)
    factoryOf(::ExitArenaUseCase)
    factoryOf(::GetCharacterClassesUseCase)
    factoryOf(::GetLeaderboardUseCase)
    factoryOf(::GetActiveChallengeFlowUseCase)
    factoryOf(::GetProfileFlowUseCase)
    factoryOf(::GetProfileUseCase)
    factoryOf(::GetRandomTravelerNameUseCase)
    factoryOf(::InitializeGameConfigUseCase)
    factoryOf(::GetTavernDataFlowUseCase)
    factoryOf(::GetUserIdUseCase)
    factoryOf(::IsUserSignedInUseCase)
    factoryOf(::LinkAccountUseCase)
    factoryOf(::PerformCombatActionUseCase)
    factoryOf(::SetCurrentTravelerAvailableUseCase)
    factoryOf(::SendChallengeUseCase)
    factoryOf(::SignInWithSocialUseCase)
    factoryOf(::StayForAnotherMatchUseCase)
    factoryOf(::UpdateChallengerStatusUseCase)
    factoryOf(::UpdateProfileDisplayNameUseCase)
    factoryOf(::UpdateProfileStatsUseCase)
}

private fun Module.initializeViewModels() {
    viewModelOf(::AppViewModel)
    viewModelOf(::ArenaScreenViewModel)
    viewModelOf(::ChallengeViewModel)
    viewModelOf(::GameRulesScreenViewModel)
    viewModelOf(::LeaderboardScreenViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::TavernScreenViewModel)
}
