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

// TODO: What events are needed?
sealed interface ArenaEvent

data class ArenaState(val arenaId: String)

// TODO: What's needed in the state?
sealed interface ArenaPhase {
    data object WaitingForAllPlayers : ArenaPhase

    data class ChoosingClasses(val fighters: List<Any>) : ArenaPhase

    data class Combat(val fighters: List<Any>) : ArenaPhase
}

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
//      When all classes are chosen, the game starts
//      Gameplay itself will be the next set of work

// TODO: Gameplay Architecture
//  Everyone is subscribed to the Arena document/flow
//

// TODO: How to convey information about the gameplay?
//   Text log can be BottomSheet
//      Preview can contain 3 lines
//          RoundMessage  // "Round 1"
//          ActionMessage // "Rogue rolled 6 against Paladin's AC"
//          ResultMessage // "Rogue is now rolling against Paladin's health!"

// CHatGPT idea
sealed interface ArenaLogEntry {
    val id: String
    val timestampMs: Long

    data class PlayerJoined(
        override val id: String,
        override val timestampMs: Long,
        val playerName: String,
    ) : ArenaLogEntry

    data class ClassChosen(
        override val id: String,
        override val timestampMs: Long,
        val playerName: String,
        val className: String,
    ) : ArenaLogEntry

    data class AttackRolled(
        override val id: String,
        override val timestampMs: Long,
        val attackerName: String,
        val defenderName: String,
        val roll: Int,
        val targetArmorClass: Int,
    ) : ArenaLogEntry

    data class DamageApplied(
        override val id: String,
        override val timestampMs: Long,
        val sourceName: String,
        val targetName: String,
        val damage: Int,
        val remainingHp: Int,
    ) : ArenaLogEntry
}

class ArenaScreenViewModel(
    private val arenaId: String,
    private val connectToArenaUseCase: ConnectToArenaUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch { connectToArenaUseCase(arenaId = arenaId) }
    }
}
