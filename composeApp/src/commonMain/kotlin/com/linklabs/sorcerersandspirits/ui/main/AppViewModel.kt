package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import kotlinx.coroutines.launch

class AppViewModel(
    private val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch { signInAnonymouslyUseCase() }
    }
}
