package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignInWithSocialUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileForCurrentUserUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
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
    private val createProfileForCurrentUserUseCase: CreateProfileForCurrentUserUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase,
    private val initializeGameConfigUseCase: InitializeGameConfigUseCase,
    private val isUserSignedInUseCase: IsUserSignedInUseCase,
    private val signInWithSocialUseCase: SignInWithSocialUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _appUpgradeStatusFlow = MutableStateFlow(value = AppUpgradeStatus.UP_TO_DATE)
    val appUpgradeStatusFlow = _appUpgradeStatusFlow.asStateFlow()

    private var screenStateJob: Job? = null

    // Once a profile exists (anonymously) this session, the dialog's actions only need to enter the
    // app rather than create another profile — re-creating would collide with the reserved name.
    private var hasCreatedProfile = false

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
                if (!isUserSignedInUseCase()) {
                    showSignInDialog()
                    return@launch
                }

                // A signed-in account with no profile (e.g. sign-in succeeded but profile creation
                // never did) would otherwise hang forever on the loading indicator, since the
                // profile flow just keeps emitting null. Route back to the dialog to create one for
                // the current account. Only checked while online: offline, the one-shot read can't
                // tell "no profile" from "unreachable", so defer to collectScreenState's offline
                // shell, which recovers once back online.
                val isOnline = connectivityObserver.isOnline.first()
                if (isOnline && getProfileUseCase() == null) {
                    showSignInDialog()
                } else {
                    collectScreenState()
                }
            }
    }

    fun createProfile(newDisplayName: String) {
        viewModelScope.launch {
            // A link attempt may have already created the anonymous profile; just enter the app.
            if (hasCreatedProfile) {
                collectScreenState()
                return@launch
            }

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

            // The dialog also appears for an already-signed-in account that is missing its profile
            // (see loadScreenState). There, create the profile for the current account rather than
            // signing in anonymously again, which would orphan the existing one.
            val createResult =
                if (isUserSignedInUseCase()) {
                    createProfileForCurrentUserUseCase(displayName = newDisplayName)
                } else {
                    createProfileUseCase(displayName = newDisplayName)
                }

            when (createResult) {
                DisplayNameResult.SUCCESS -> {
                    hasCreatedProfile = true
                    collectScreenState()
                }

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

    fun onSignInWithSocialMethod(displayName: String, method: SignInMethod) {
        signInFromDialog(displayName = displayName) { signInWithSocialUseCase(method = method) }
    }

    fun onSignInWithEmail(displayName: String, email: String, password: String) {
        signInFromDialog(displayName = displayName) {
            signInWithSocialUseCase.withEmail(email = email, password = password)
        }
    }

    /**
     * From the first-launch dialog a provider sign-in either recovers an existing account (e.g. a
     * returning player who reinstalled) or provisions a new one — there is no anonymous data to
     * preserve, so we sign in rather than link. On success we make sure the account has a profile
     * (recovered accounts already do; brand-new ones get one with the typed name) and enter the
     * app; on failure the dialog stays up with the reason.
     */
    private fun signInFromDialog(displayName: String, signIn: suspend () -> LinkAccountResult) {
        viewModelScope.launch {
            if (!connectivityObserver.isOnline.first()) {
                _uiStateFlow.value =
                    UiState.Loaded(
                        data =
                            AppState(
                                displayName = displayName,
                                showSignInDialog = true,
                                isOffline = true,
                            )
                    )
                return@launch
            }

            _uiStateFlow.value = UiState.Loading

            val signInResult = signIn()
            if (signInResult == LinkAccountResult.SUCCESS) {
                enterAppEnsuringProfile(displayName = displayName)
            } else {
                _uiStateFlow.value =
                    UiState.Loaded(
                        data =
                            AppState(
                                displayName = displayName,
                                showSignInDialog = true,
                                linkResult = signInResult,
                            )
                    )
            }
        }
    }

    /**
     * A recovered account already has a profile; a brand-new provider account does not, so create
     * one with the typed name before entering (otherwise the screen would hang waiting for a
     * profile).
     */
    private suspend fun enterAppEnsuringProfile(displayName: String) {
        if (getProfileUseCase() != null) {
            hasCreatedProfile = true
            collectScreenState()
            return
        }

        when (createProfileForCurrentUserUseCase(displayName = displayName)) {
            DisplayNameResult.SUCCESS -> {
                hasCreatedProfile = true
                collectScreenState()
            }

            DisplayNameResult.NAME_TAKEN ->
                _uiStateFlow.value =
                    UiState.Loaded(
                        data =
                            AppState(
                                displayName = displayName,
                                showSignInDialog = true,
                                isNameTaken = true,
                            )
                    )

            DisplayNameResult.ERROR ->
                _uiStateFlow.value =
                    UiState.Loaded(
                        data = AppState(displayName = displayName, showSignInDialog = true)
                    )
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
