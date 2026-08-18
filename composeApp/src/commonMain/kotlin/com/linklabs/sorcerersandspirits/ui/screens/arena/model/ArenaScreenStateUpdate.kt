package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerUserId
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.toState

internal data class ArenaScreenStateUpdate(
    val screenState: ArenaScreenState,
    val timerAction: TimerAction,
    val messageQueueUpdate: ArenaMatch.Combat? = null,
)

internal fun ArenaMatch.toUpdatedScreenState(
    userId: String,
    currentScreenState: ArenaScreenState,
): ArenaScreenStateUpdate =
    when (val currentArenaMatchState = currentScreenState.arenaMatchState) {
        is ArenaMatchState.WaitingForAllPlayers ->
            when (this) {
                is ArenaMatch.WaitingForAllPlayers ->
                    updateWaitingForAllPlayers(currentScreenState = currentScreenState)

                is ArenaMatch.ChoosingClasses ->
                    transitionWaitingToChoosingClasses(
                        userId = userId,
                        currentScreenState = currentScreenState,
                    )

                is ArenaMatch.Combat ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)

                is ArenaMatch.Finished ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)
            }

        is ArenaMatchState.ChoosingClasses ->
            when (this) {
                is ArenaMatch.ChoosingClasses ->
                    updateChoosingClasses(
                        userId = userId,
                        currentScreenState = currentScreenState,
                        currentArenaMatchState = currentArenaMatchState,
                    )

                is ArenaMatch.Combat ->
                    transitionChoosingClassesToCombat(
                        userId = userId,
                        currentScreenState = currentScreenState,
                    )

                is ArenaMatch.Finished ->
                    transitionChoosingClassesToFinished(currentScreenState = currentScreenState)

                is ArenaMatch.WaitingForAllPlayers ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)
            }

        is ArenaMatchState.Combat ->
            when (this) {
                is ArenaMatch.Combat ->
                    updateCombat(
                        currentScreenState = currentScreenState,
                        currentArenaMatchState = currentArenaMatchState,
                    )

                is ArenaMatch.Finished ->
                    transitionCombatToFinished(currentScreenState = currentScreenState)

                is ArenaMatch.ChoosingClasses ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)

                is ArenaMatch.WaitingForAllPlayers ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)
            }

        is ArenaMatchState.Finished ->
            when (this) {
                is ArenaMatch.ChoosingClasses ->
                    transitionFinishedToChoosingClasses(
                        userId = userId,
                        currentScreenState = currentScreenState,
                    )

                is ArenaMatch.Finished -> updateFinished(currentScreenState = currentScreenState)

                is ArenaMatch.Combat ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)

                is ArenaMatch.WaitingForAllPlayers ->
                    handleImpossibleScreenUpdate(currentScreenState = currentScreenState)
            }
    }

private fun ArenaMatch.WaitingForAllPlayers.updateWaitingForAllPlayers(
    currentScreenState: ArenaScreenState
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages)
            ),
        timerAction = TimerAction.None,
    )

private fun ArenaMatch.ChoosingClasses.transitionWaitingToChoosingClasses(
    userId: String,
    currentScreenState: ArenaScreenState,
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = toChoosingClassesState(userPlayerId = userId),
                secondsRemaining = CHOOSING_CLASSES_TIMER_DURATION_SECONDS,
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages),
            ),
        timerAction = TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS),
    )

private fun ArenaMatch.ChoosingClasses.updateChoosingClasses(
    userId: String,
    currentScreenState: ArenaScreenState,
    currentArenaMatchState: ArenaMatchState.ChoosingClasses,
): ArenaScreenStateUpdate {
    val (unavailableClasses, classSelections) = playerMap.prepareClassSelectionInfo(userId = userId)

    val updatedArenaMatchState =
        currentArenaMatchState.copy(
            playerMap = playerMap.mapValues { it.value.toState() },
            activePlayerUserId = activePlayerUserId,
            unavailableClasses = unavailableClasses,
            classSelections = classSelections,
        )

    val hasActivePlayerChanged = currentArenaMatchState.activePlayerUserId != activePlayerUserId
    val timerAction =
        if (hasActivePlayerChanged) {
            TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS)
        } else {
            TimerAction.None
        }

    return ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = updatedArenaMatchState,
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages),
            ),
        timerAction = timerAction,
    )
}

private fun ArenaMatch.Combat.transitionChoosingClassesToCombat(
    userId: String,
    currentScreenState: ArenaScreenState,
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = toCombatState(userId = userId),
                secondsRemaining = COMBAT_TIMER_DURATION_SECONDS,
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages),
            ),
        timerAction = TimerAction.Start(timerDuration = COMBAT_TIMER_DURATION_SECONDS),
    )

private fun ArenaMatch.Combat.updateCombat(
    currentScreenState: ArenaScreenState,
    currentArenaMatchState: ArenaMatchState.Combat,
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = currentArenaMatchState.copy(diceRollState = diceRoll?.toState())
            ),
        timerAction = TimerAction.None,
        messageQueueUpdate = this,
    )

private fun ArenaMatch.Finished.transitionCombatToFinished(
    currentScreenState: ArenaScreenState
): ArenaScreenStateUpdate = // TODO: MATCH_FINISHED vs GAUNTLET_FINISHED timer depends on whether
    // this is the final match
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = toFinishedState(),
                secondsRemaining = MATCH_FINISHED_TIMER_DURATION_SECONDS,
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages),
            ),
        timerAction = TimerAction.Start(timerDuration = MATCH_FINISHED_TIMER_DURATION_SECONDS),
    )

// TODO: Can happen if someone leaves during choosing. Decide whether to kick everyone out.
private fun ArenaMatch.Finished.transitionChoosingClassesToFinished(
    currentScreenState: ArenaScreenState
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = toFinishedState(),
                secondsRemaining = MATCH_FINISHED_TIMER_DURATION_SECONDS,
                messageGroups =
                    currentScreenState.messageGroups.appendNewMessages(incomingMessages = messages),
            ),
        timerAction = TimerAction.Start(timerDuration = MATCH_FINISHED_TIMER_DURATION_SECONDS),
    )

private fun ArenaMatch.ChoosingClasses.transitionFinishedToChoosingClasses(
    userId: String,
    currentScreenState: ArenaScreenState,
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState =
            currentScreenState.copy(
                arenaMatchState = toChoosingClassesState(userPlayerId = userId),
                secondsRemaining = CHOOSING_CLASSES_TIMER_DURATION_SECONDS,
                hasReadiedForNextMatch = false,
                messageGroups =
                    emptyList<ArenaMessageGroupState>()
                        .appendNewMessages(incomingMessages = messages),
                gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet),
            ),
        timerAction = TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS),
    )

private fun ArenaMatch.Finished.updateFinished(
    currentScreenState: ArenaScreenState
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(
        screenState = currentScreenState.copy(arenaMatchState = toFinishedState()),
        timerAction = TimerAction.None,
    )

// TODO: Reserved for ArenaMatch/ArenaScreenState combinations that should never occur. Likely an
//       error state; behavior to be decided.
private fun ArenaMatch.handleImpossibleScreenUpdate(
    currentScreenState: ArenaScreenState
): ArenaScreenStateUpdate =
    ArenaScreenStateUpdate(screenState = currentScreenState, timerAction = TimerAction.None)

private fun ArenaMatch.Combat.toCombatState(userId: String) =
    ArenaMatchState.Combat(
        playerMap = playerMap.mapValues { it.value.toSelectedState() },
        userPlayerId = userId,
        combatActionState = nextCombatAction.toCombatActionState(),
        roundNumber = roundNumber,
        isNewRound = isNewRound,
        isMatchOver = isMatchOver,
        turnOwnerUserId = nextCombatAction.turnOwnerUserId(),
        lockedTargetUserId = nextCombatAction.lockedTargetUserId(userId = userId),
        diceRollState = diceRoll?.toState(),
        isActionInFlight = false,
    )

private fun ArenaMatch.Finished.toFinishedState() =
    ArenaMatchState.Finished(
        players = playerMap.values.map { it.toState() }.filterIsInstance<PlayerState.Selected>(),
        hasAnyPlayerReadied = playerMap.values.any { it.isReadyForNextMatch },
    )

private fun Map<String, ArenaPlayer>.prepareClassSelectionInfo(
    userId: String
): Pair<Set<CharacterClassType>, Map<CharacterClassType, String>> {
    val selectedPlayers = values.filterIsInstance<ArenaPlayer.Selected>()
    val unavailableClasses =
        selectedPlayers
            .filter { player -> player.userId != userId }
            .map { player -> player.characterClass.type }
            .toSet()

    val classSelections =
        selectedPlayers.associate { player -> player.characterClass.type to player.displayName }

    return unavailableClasses to classSelections
}
