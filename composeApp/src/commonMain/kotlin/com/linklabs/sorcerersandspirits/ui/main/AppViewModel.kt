package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.DeleteProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.domain.version.usecase.CheckAppVersionUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AppViewModel(
    private val checkAppVersionUseCase: CheckAppVersionUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val connectTravelerPresenceUseCase: ConnectTravelerPresenceUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase,
    private val initializeGameConfigUseCase: InitializeGameConfigUseCase,
    private val isUserSignedInUseCase: IsUserSignedInUseCase,
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _appUpgradeStatusFlow = MutableStateFlow(value = AppUpgradeStatus.UP_TO_DATE)
    val appUpgradeStatusFlow = _appUpgradeStatusFlow.asStateFlow()

    private var screenStateJob: Job? = null

    init {
        viewModelScope.launch {
            // The version thresholds live in RemoteConfig, so the gate can only be evaluated once
            // the config has been fetched/activated.
            initializeGameConfigUseCase()
            _appUpgradeStatusFlow.value = checkAppVersionUseCase()
        }

        loadScreenState()
    }

    fun onDismissUpdateNudge() {
        // Only the soft "update available" nudge is dismissible; a force-upgrade block stays put.
        val isDismissibleNudge = _appUpgradeStatusFlow.value == AppUpgradeStatus.UPDATE_AVAILABLE
        if (isDismissibleNudge) {
            _appUpgradeStatusFlow.value = AppUpgradeStatus.UP_TO_DATE
        }
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadScreenState()
    }

    private fun loadScreenState() {
        screenStateJob?.cancel()
        screenStateJob =
            viewModelScope.launch {
                // TODO(SS-036): if user isSignedIn but their profile doesn't exist, then the app
                // will hang
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

    fun onDeleteProfile() {
        viewModelScope.launch {
            // Stop collecting the now-deleted profile before its node is removed.
            screenStateJob?.cancel()
            _uiStateFlow.value = UiState.Loading

            deleteProfileUseCase()

            // The anonymous identity persists, so silently start a fresh profile with a random name
            // rather than forcing the sign-in dialog. A deliberate profile-creation / sign-in flow
            // (and converting the anonymous account) is future work (SS-061).
            val newProfileResult =
                createProfileUseCase(displayName = getRandomTravelerNameUseCase())

            if (newProfileResult == DisplayNameResult.SUCCESS) {
                loadScreenState()
            } else {
                // Fallback so a failed silent creation (e.g. a rare random-name collision) can't
                // strand the user without a profile.
                showSignInDialog()
            }
        }
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

            when (createProfileUseCase(displayName = newDisplayName)) {
                DisplayNameResult.SUCCESS -> collectScreenState()

                DisplayNameResult.NAME_TAKEN ->
                    _uiStateFlow.value =
                        UiState.Loaded(
                            data =
                                AppState(
                                    displayName = newDisplayName,
                                    showSignInDialog = true,
                                    isNameTaken = true,
                                )
                        )

                DisplayNameResult.ERROR ->
                    _uiStateFlow.value =
                        UiState.Loaded(
                            data = AppState(displayName = newDisplayName, showSignInDialog = true)
                        )
            }
        }
    }

    fun getRandomName() {
        _uiStateFlow.updateLoaded { it.copy(displayName = getRandomTravelerNameUseCase()) }
    }

    private fun showSignInDialog() {
        _uiStateFlow.value =
            UiState.Loaded(
                data =
                    AppState(displayName = getRandomTravelerNameUseCase(), showSignInDialog = true)
            )
    }

    private suspend fun collectScreenState() {
        // Reused if we show the offline shell before a profile is ever loaded, so the name stays
        // stable across connectivity changes.
        val displayNameWhenOffline = getRandomTravelerNameUseCase()

        combine(getProfileFlowUseCase(), connectivityObserver.isOnline) { profile, isOnline ->
                profile to isOnline
            }
            .catch { _uiStateFlow.value = UiState.Error }
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
