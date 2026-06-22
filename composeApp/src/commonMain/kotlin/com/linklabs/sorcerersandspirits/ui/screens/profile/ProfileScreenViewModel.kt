package com.linklabs.sorcerersandspirits.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.LinkAccountUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.DeleteProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ProfileScreenViewModel(
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase,
    private val linkAccountUseCase: LinkAccountUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<ProfileState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    // Kept off ProfileState so a profile re-emission (e.g. a name update or connectivity change)
    // can't wipe the link feedback mid-view.
    private val _linkResultFlow = MutableStateFlow<LinkAccountResult?>(value = null)
    val linkResultFlow = _linkResultFlow.asStateFlow()

    private val _eventChannelFlow = Channel<ProfileEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private var profileJob: Job? = null

    init {
        collectProfile()
    }

    fun onUpdateDisplayName(newDisplayName: String) {
        viewModelScope.launch {
            val result = updateProfileDisplayNameUseCase(displayName = newDisplayName)
            _eventChannelFlow.send(element = ProfileEvent.DisplayNameUpdated(result = result))
        }
    }

    fun onDeleteProfile() {
        viewModelScope.launch {
            // Stop collecting the about-to-be-deleted profile so its null emission can't surface as
            // a transient blank name before we pop the screen.
            profileJob?.cancel()
            _uiStateFlow.value = UiState.Loading

            deleteProfileUseCase()

            // The anonymous identity persists, so silently provision a fresh profile with a random
            // name rather than stranding the player. A deliberate profile-creation / sign-in flow
            // is
            // future work (SS-061).
            createProfileUseCase(displayName = getRandomTravelerNameUseCase())

            _eventChannelFlow.send(element = ProfileEvent.ProfileDeleted)
        }
    }

    fun onLinkWithSocialMethod(method: SignInMethod) {
        linkCurrentAccount { linkAccountUseCase(method = method) }
    }

    fun onLinkWithEmail(email: String, password: String) {
        linkCurrentAccount { linkAccountUseCase.withEmail(email = email, password = password) }
    }

    /**
     * A profile (and anonymous account) already exists here, so linking is a direct in-place
     * upgrade — no profile creation, just surface the outcome.
     */
    private fun linkCurrentAccount(link: suspend () -> LinkAccountResult) {
        viewModelScope.launch { _linkResultFlow.value = link() }
    }

    private fun collectProfile() {
        profileJob?.cancel()
        profileJob =
            viewModelScope.launch {
                getProfileFlowUseCase()
                    .filterNotNull()
                    .catch { _uiStateFlow.value = UiState.Error }
                    .collect { profile ->
                        _uiStateFlow.value = UiState.Loaded(data = profile.toProfileState())
                    }
            }
    }
}
