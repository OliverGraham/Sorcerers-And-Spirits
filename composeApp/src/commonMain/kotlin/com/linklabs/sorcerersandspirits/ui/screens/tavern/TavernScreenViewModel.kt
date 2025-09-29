package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import com.linklabs.sorcerersandspirits.domain.tavern.GetTavernUseCase

class TavernScreenViewModel(
    private val getTavernUseCase: GetTavernUseCase,
) : ViewModel()
