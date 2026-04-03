package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.GetTravelerUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable data class AppState(val displayName: String)

class AppViewModel(
    private val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
    private val getTravelerUseCase: GetTravelerUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            signInAnonymouslyUseCase()
            initializeAppState()
        }
    }

    fun onUpdateDisplayName(newDisplayName: String) {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(displayName = newDisplayName)
            initializeAppState()
        }
    }

    private suspend fun initializeAppState() {
        val traveler = getTravelerUseCase()
        val appState =
            if (traveler == null) {
                UiState.Error
            } else {
                UiState.Loaded(data = AppState(displayName = traveler.displayName))
            }

        _uiStateFlow.value = appState
    }
}
