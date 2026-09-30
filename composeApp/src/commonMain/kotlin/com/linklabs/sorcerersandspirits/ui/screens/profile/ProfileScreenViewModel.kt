package com.linklabs.sorcerersandspirits.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserAnonymousUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.LinkAccountUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.DeleteProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileByUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
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
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ProfileScreenViewModel(
    @InjectedParam private val arguments: ProfileScreenViewModelArguments,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
    private val getProfileByUserIdUseCase: GetProfileByUserIdUseCase,
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val linkAccountUseCase: LinkAccountUseCase,
    isUserAnonymousUseCase: IsUserAnonymousUseCase,
) : ViewModel() {

    val isReadOnly: Boolean = arguments.targetUserId != null

    private val _canSignOutFlow = MutableStateFlow(value = false)
    val canSignOutFlow = _canSignOutFlow.asStateFlow()

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
        _canSignOutFlow.value = !isReadOnly && !isUserAnonymousUseCase()
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
            profileJob?.cancel()
            _uiStateFlow.value = UiState.Loading

            deleteProfileUseCase()

            _eventChannelFlow.send(element = ProfileEvent.ProfileDeleted)
        }
    }

    fun onLinkWithSocialMethod(method: SignInMethod) {
        linkCurrentAccount { linkAccountUseCase(method = method) }
    }

    fun onLinkWithEmail(email: String, password: String) {
        linkCurrentAccount { linkAccountUseCase.withEmail(email = email, password = password) }
    }

    private fun linkCurrentAccount(link: suspend () -> LinkAccountResult) {
        viewModelScope.launch {
            val result = link()
            _linkResultFlow.value = result
            if (result == LinkAccountResult.SUCCESS) {
                _canSignOutFlow.value = true
            }
        }
    }

    private fun collectProfile() {
        val targetUserId = arguments.targetUserId
        if (targetUserId != null) {
            loadProfile(userId = targetUserId)
            return
        }

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

    private fun loadProfile(userId: String) {
        profileJob?.cancel()
        profileJob =
            viewModelScope.launch {
                val profile = getProfileByUserIdUseCase(userId = userId)
                _uiStateFlow.value =
                    if (profile != null) {
                        UiState.Loaded(data = profile.toProfileState())
                    } else {
                        UiState.Error
                    }
            }
    }
}
