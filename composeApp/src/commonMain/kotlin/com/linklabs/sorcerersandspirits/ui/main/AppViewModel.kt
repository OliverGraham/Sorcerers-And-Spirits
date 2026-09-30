package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.usecase.SyncArenaMembershipOnConnectUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignInWithSocialUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignOutUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileForCurrentUserUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.FlushPendingStatsUseCase
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
    private val flushPendingStatsUseCase: FlushPendingStatsUseCase,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase,
    private val initializeGameConfigUseCase: InitializeGameConfigUseCase,
    private val isUserSignedInUseCase: IsUserSignedInUseCase,
    private val signInWithSocialUseCase: SignInWithSocialUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val syncArenaMembershipOnConnectUseCase: SyncArenaMembershipOnConnectUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _appUpgradeStatusFlow = MutableStateFlow(value = AppUpgradeStatus.UP_TO_DATE)
    val appUpgradeStatusFlow = _appUpgradeStatusFlow.asStateFlow()

    private var screenStateJob: Job? = null
    private var hasCreatedProfile = false
    private var hasSyncedOrphanedArena = false

    init {
        viewModelScope.launch {
            initializeGameConfigUseCase()
            _appUpgradeStatusFlow.value = checkAppVersionUseCase()
        }

        viewModelScope.launch { flushPendingStatsUseCase() }

        loadScreenState()
    }

    fun onDismissUpdateNudge() {
        val isDismissibleNudge = _appUpgradeStatusFlow.value == AppUpgradeStatus.UPDATE_AVAILABLE
        if (isDismissibleNudge) {
            _appUpgradeStatusFlow.value = AppUpgradeStatus.UP_TO_DATE
        }
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadScreenState()
    }

    fun onSignOut() {
        viewModelScope.launch {
            screenStateJob?.cancel()
            _uiStateFlow.value = UiState.Loading

            signOutUseCase()

            hasCreatedProfile = false
            hasSyncedOrphanedArena = false

            showSignInDialog()
        }
    }

    private fun loadScreenState() {
        screenStateJob?.cancel()
        screenStateJob =
            viewModelScope.launch {
                if (!isUserSignedInUseCase()) {
                    showSignInDialog()
                    return@launch
                }

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
            if (hasCreatedProfile) {
                collectScreenState()
                return@launch
            }

            if (!connectivityObserver.isOnline.first()) {
                // TODO: Offline experience
                _uiStateFlow.value =
                    UiState.Loaded(
                        data =
                        AppState(
                            displayName = newDisplayName,
                            showSignInDialog = true,
                            isOffline = true,
                        ),
                    )
                return@launch
            }

            _uiStateFlow.value = UiState.Loading

            // TODO: Functions (for readability)
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
                            ),
                        )

                DisplayNameResult.ERROR ->
                    _uiStateFlow.value =
                        UiState.Loaded(
                            data = AppState(displayName = newDisplayName, showSignInDialog = true),
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
                        ),
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
                        ),
                    )
            }
        }
    }

    // TODO: Looks like same things are happening - make functions
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
                        ),
                    )

            DisplayNameResult.ERROR ->
                _uiStateFlow.value =
                    UiState.Loaded(
                        data = AppState(displayName = displayName, showSignInDialog = true),
                    )
        }
    }

    fun getRandomName() {
        _uiStateFlow.updateLoaded { it.copy(displayName = getRandomTravelerNameUseCase()) }
    }

    private fun cleanUpOrphanedArenaOnce() {
        if (hasSyncedOrphanedArena) return
        hasSyncedOrphanedArena = true
        viewModelScope.launch { syncArenaMembershipOnConnectUseCase() }
    }

    private fun showSignInDialog() {
        _uiStateFlow.value =
            UiState.Loaded(
                data =
                AppState(displayName = getRandomTravelerNameUseCase(), showSignInDialog = true),
            )
    }

    private suspend fun collectScreenState() {
        val displayNameWhenOffline = getRandomTravelerNameUseCase()

        combine(flow = getProfileFlowUseCase(), flow2 = connectivityObserver.isOnline) {
                profile,
                isOnline,
            ->
            profile to isOnline
        }
            .catch { _uiStateFlow.value = UiState.Error }
            .collectLatest { (profile, isOnline) ->
                when {
                    profile != null -> {
                        if (isOnline) {
                            connectTravelerPresenceUseCase(profile = profile)
                            cleanUpOrphanedArenaOnce()
                        }

                        _uiStateFlow.value =
                            UiState.Loaded(
                                data =
                                AppState(
                                    displayName = profile.displayName,
                                    isOffline = !isOnline,
                                ),
                            )
                    }

                    !isOnline ->
                        _uiStateFlow.value =
                            UiState.Loaded(
                                data =
                                AppState(displayName = displayNameWhenOffline, isOffline = true),
                            )
                }
            }
    }
}
