package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.GetTravelerUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.SaveNewTravelerUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// TODO: Get random names from remote config
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

class AppViewModel(
    private val getTravelerUseCase: GetTravelerUseCase,
    private val isUserSignedInUseCase: IsUserSignedInUseCase,
    private val saveNewTravelerUseCase: SaveNewTravelerUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AppState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            if (isUserSignedInUseCase()) {
                initializeAppState()
            } else {
                showSignInDialog()
            }
        }
    }

    fun onUpdateDisplayName(newDisplayName: String) {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(displayName = newDisplayName)
            initializeAppState()
        }
    }

    fun saveNewTraveler(newDisplayName: String) {
        viewModelScope.launch {
            val newTraveler = saveNewTravelerUseCase(displayName = newDisplayName)
            initializeAppState(newTraveler = newTraveler)
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

    private suspend fun initializeAppState(newTraveler: Traveler? = null) {
        val traveler = newTraveler ?: getTravelerUseCase()
        val appState =
            if (traveler == null) {
                UiState.Error
            } else {
                UiState.Loaded(data = AppState(displayName = traveler.displayName))
            }

        _uiStateFlow.value = appState
    }
}
