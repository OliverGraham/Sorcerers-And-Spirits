package com.linklabs.sorcerersandspirits.ui.uistate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

fun <T> MutableStateFlow<UiState<T>>.updateLoaded(transform: (T) -> T) {
    update { state ->
        if (state !is UiState.Loaded<T>) return@update state

        state.copy(data = transform(state.data))
    }
}
