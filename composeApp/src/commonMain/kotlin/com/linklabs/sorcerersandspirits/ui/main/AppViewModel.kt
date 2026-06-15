package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

// TODO(SS-035): Get random names from remote config
private val randomNames =
    listOf(
        "a mysterious traveler",
        "a nameless traveler",
        "a shadowed traveler",
        "a wandering stranger",
        "an unknown wanderer",
        "a cloaked drifter",
        "a faceless traveler",
        "a lone wayfarer",
        "a silent wanderer",
        "a hooded stranger",
        "an enigmatic traveler",
        "a forgotten wanderer",
        "a weathered traveler",
        "a restless drifter",
        "a veiled traveler",
        "a duskbound wanderer",
        "a road-worn stranger",
        "a secretive traveler",
        "a grim wanderer",
        "a dust-covered drifter",
        "a hidden wayfarer",
        "a pale wanderer",
        "a quiet traveler",
        "a haunted drifter",
        "an aimless stranger",
        "a hooded wayfarer",
        "a ragged traveler",
        "a grim-faced wanderer",
        "a roadbound stranger",
        "a wandering soul",
        "a cryptic traveler",
        "a forlorn wanderer",
        "a dark-cloaked stranger",
        "a moonlit traveler",
        "an ash-covered drifter",
    )

@KoinViewModel
class AppViewModel(
    private val connectivityObserver: ConnectivityObserver,
    private val connectTravelerPresenceUseCase: ConnectTravelerPresenceUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val isUserSignedInUseCase: IsUserSignedInUseCase,
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            // TODO(SS-036): if user isSignedIn but their profile doesn't exist, then the app will
            // hang
            if (isUserSignedInUseCase()) {
                collectScreenState()
            } else {
                showSignInDialog()
            }
        }
    }

    fun onUpdateDisplayName(newDisplayName: String) {
        viewModelScope.launch { updateProfileDisplayNameUseCase(displayName = newDisplayName) }
    }

    fun createProfile(newDisplayName: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isOnline.first()) {
                // Signing in requires the network. Keep the dialog up with an offline notice
                // instead of hanging on the loading indicator.
                _uiStateFlow.value =
                    UiState.Loaded(
                        data =
                            AppState(
                                displayName = newDisplayName,
                                showSignInDialog = true,
                                isOffline = true,
                            )
                    )
                return@launch
            }

            _uiStateFlow.value = UiState.Loading
            createProfileUseCase(displayName = newDisplayName)
            collectScreenState()
        }
    }

    fun getRandomName() {
        _uiStateFlow.updateLoaded { it.copy(displayName = randomNames.random()) }
    }

    private fun showSignInDialog() {
        _uiStateFlow.value =
            UiState.Loaded(
                data = AppState(displayName = randomNames.random(), showSignInDialog = true)
            )
    }

    private suspend fun collectScreenState() {
        // Reused if we show the offline shell before a profile is ever loaded, so the name stays
        // stable across connectivity changes.
        val displayNameWhenOffline = randomNames.random()

        combine(getProfileFlowUseCase(), connectivityObserver.isOnline) { profile, isOnline ->
                profile to isOnline
            }
            .collectLatest { (profile, isOnline) ->
                when {
                    profile != null -> {
                        if (isOnline) connectTravelerPresenceUseCase(profile = profile)

                        _uiStateFlow.value =
                            UiState.Loaded(
                                data =
                                    AppState(
                                        displayName = profile.displayName,
                                        isOffline = !isOnline,
                                    )
                            )
                    }

                    // No profile yet and offline: show the shell so the app stays usable instead
                    // of hanging on the loading indicator. Recovers once back online.
                    !isOnline ->
                        _uiStateFlow.value =
                            UiState.Loaded(
                                data =
                                    AppState(displayName = displayNameWhenOffline, isOffline = true)
                            )

                // Online but the profile hasn't arrived yet: keep showing the loading indicator.
                }
            }
    }
}
