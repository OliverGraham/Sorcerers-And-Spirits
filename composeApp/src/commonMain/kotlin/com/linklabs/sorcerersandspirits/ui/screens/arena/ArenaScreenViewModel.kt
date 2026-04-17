package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ConnectToArenaUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// TODO: Upon entering, everyone needs to connect to the Arena using the ArenaId
//  Needs to be created via a transaction done by the first person who gets there
//      So, create (or try to) and get the flow of data
//  If everyone's there, then need to choose classes, in order
//      Need to pass in the expected number of players to view model, and once the the document
//          has that number of players, can move into the next phase
//      On first entering, random player goes first. Subsequent matches, the winner chooses first,
//          "clockwise" order (hmm, order of who entered right after that person, or random, doesn't
// matter)
//      Timer for choosing (15 seconds), otherwise randomly chosen

class ArenaScreenViewModel(
    private val arguments: ArenaScreenViewModelArguments,
    private val connectToArenaUseCase: ConnectToArenaUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            connectToArenaUseCase(arenaId = arguments.arenaId)

            // TODO: get result to determine UI state
            //  If
        }
    }
}
