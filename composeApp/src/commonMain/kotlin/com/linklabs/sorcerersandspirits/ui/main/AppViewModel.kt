package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.SignInAnonymouslyUseCase
import kotlinx.coroutines.launch

class AppViewModel(
    private val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            signInAnonymouslyUseCase()
        }
    }
}
