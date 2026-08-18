package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaSessionStats
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain

/** To be used when the [ArenaMatchState] is [ArenaMatchState.ChoosingClasses] */
internal class ChooseCharacterClassHandler(
    private val arenaId: String,
    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase,
    private val sessionStats: ArenaSessionStats,
    private val pendingStatsStore: PendingStatsStore,
) {
    fun selectCharacterClass(
        screenState: ArenaScreenState,
        characterClassState: CharacterClassState,
    ): ArenaScreenState {
        val arenaState = screenState.arenaMatchState.asChoosingClassesOrNull() ?: return screenState

        return screenState.copy(
            arenaMatchState = arenaState.copy(selectedClass = characterClassState)
        )
    }

    suspend fun autoChooseCharacterClassForUser(
        screenState: ArenaScreenState,
        arenaMatchState: ArenaMatchState.ChoosingClasses,
    ): ArenaScreenState? {
        if (!arenaMatchState.isItThisUsersTurn) return null

        val availableClasses =
            screenState.characterClasses.filterNot { it.type in arenaMatchState.unavailableClasses }
        val chosenClass =
            arenaMatchState.selectedClass ?: availableClasses.randomOrNull() ?: return null

        val userPlayer = arenaMatchState.playerMap.getValue(key = arenaMatchState.userPlayerId)

        return chooseCharacterClass(
            screenState = screenState,
            playerState = userPlayer,
            characterClassState = chosenClass,
        )
    }

    suspend fun chooseCharacterClass(
        screenState: ArenaScreenState,
        playerState: PlayerState,
        characterClassState: CharacterClassState,
    ): ArenaScreenState? {
        val arenaState = screenState.arenaMatchState.asChoosingClassesOrNull() ?: return null

        val selectedArenaPlayer =
            ArenaPlayer.Selected(
                userId = playerState.userId,
                displayName = playerState.displayName,
                characterClass = characterClassState.toDomain(),
            )

        chooseCharacterClassUseCase(
            arenaId = arenaId,
            selectedArenaPlayer = selectedArenaPlayer,
            allPlayers = arenaState.otherPlayers.values.map { it.toDomain() } + selectedArenaPlayer,
        )

        val recordedStat =
            sessionStats.onClassSelected(classType = selectedArenaPlayer.characterClass.type)
        if (recordedStat) {
            pendingStatsStore.save(delta = sessionStats.peekDelta())
        }

        return screenState.copy(arenaMatchState = arenaState.copy(selectedClass = null))
    }

    private fun ArenaMatchState.asChoosingClassesOrNull() = this as? ArenaMatchState.ChoosingClasses
}
