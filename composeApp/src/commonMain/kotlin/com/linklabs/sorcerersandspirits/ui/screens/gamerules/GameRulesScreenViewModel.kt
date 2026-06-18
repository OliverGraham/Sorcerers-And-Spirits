package com.linklabs.sorcerersandspirits.ui.screens.gamerules

import androidx.lifecycle.ViewModel
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

// TODO(SS-081): Rules copy is assembled here from static literals. This ViewModel is the single
//  seam to swap it for RemoteConfig — route the copy through GetCharacterClassesUseCase (or a
//  parallel copy provider), surfacing UiState.Loading while it fetches and UiState.Error (retry())
//  on failure.
@KoinViewModel
class GameRulesScreenViewModel(private val getCharacterClassesUseCase: GetCharacterClassesUseCase) :
    ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<GameRulesState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        loadGameRules()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadGameRules()
    }

    private fun loadGameRules() {
        val classes = getCharacterClassesUseCase().map { it.toState().toRulesState() }

        _uiStateFlow.value =
            UiState.Loaded(
                data =
                    GameRulesState(
                        combatRules = COMBAT_RULES,
                        gameTerms = GAME_TERMS,
                        classes = classes,
                    )
            )
    }
}

private val COMBAT_RULES =
    listOf(
        "An Arena match has 2 to 8 players. Each player picks a unique class — no duplicates.",
        "On your turn you choose a target and make a normal attack with 2d6. The first roll is " +
            "against the target's Armor Class: meet or beat it to break their armor.",
        "If armor breaks, you roll again against the target's Health. Meet or beat it and the " +
            "target is knocked out. Either way, play passes to the next living player.",
        "Knocked-out players are skipped. The match ends when one or zero players remain standing.",
        "Rolling doubles (both dice equal) grants +1 to your next consecutive roll. The bonus is " +
            "lost if the dice pass to another player or another ability before that next roll.",
    )

private val GAME_TERMS =
    listOf(
        "Turn — one player attacking or using an ability.",
        "Round — every living player taking one turn.",
        "Match — a full Arena battle from start until one or zero players remain.",
        "Gauntlet — a run of 3 consecutive matches in the same Arena. Stats are recorded per " +
            "completed Gauntlet, not per match.",
    )
