package com.linklabs.ui.screens.tavern

import androidx.lifecycle.ViewModel
import com.linklabs.domain.tavern.GetTavernUseCase

class TavernScreenViewModel(
    private val getTavernUseCase: GetTavernUseCase,
) : ViewModel()
