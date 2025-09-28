package com.linklabs.ui.uistate

import androidx.compose.runtime.Immutable

@Immutable
sealed interface UiState<out T> {

    @Immutable
    data object Loading : UiState<Nothing>

    @Immutable
    data object Error : UiState<Nothing>

    @Immutable
    data class Loaded<T>(val data: T) : UiState<T>
}
