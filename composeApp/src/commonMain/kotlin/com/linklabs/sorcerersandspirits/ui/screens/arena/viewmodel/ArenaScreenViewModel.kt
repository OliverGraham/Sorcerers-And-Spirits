package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerUserId
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.FinishMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassCopyUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaEvent
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaSessionStats
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.computer.ComputerOpponentHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CHOOSING_CLASSES_TIMER_DURATION_SECONDS
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgress
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.WAITING_TIMER_DURATION_SECONDS
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.addNewMessageDuringCombat
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.prepareClassSelectionInfo
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.resolveSelectedClass
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toChoosingClassesState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toCombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toMessageStates
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toSelectedState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.MessageQueue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.ChooseCharacterClassHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.CombatHandler
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.getLoadedStateOrNull
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ArenaScreenViewModel(
    @InjectedParam private val arguments: ArenaScreenViewModelArguments,
    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase,
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase,
    private val exitArenaUseCase: ExitArenaUseCase,
    private val finishMatchUseCase: FinishMatchUseCase,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getCharacterClassCopyUseCase: GetCharacterClassCopyUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val updateProfileStatsUseCase: UpdateProfileStatsUseCase,
    private val pendingStatsStore: PendingStatsStore,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val messageQueue =
        MessageQueue(
            triggerAnimation = ::triggerAnimation,
            addMessageToScreenState = ::addMessageToScreenState,
            onAnimationsComplete = ::onAnimationsComplete,
        )

    // TODO: Probably a handler for this
    private val sessionStats = ArenaSessionStats()

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }
    private val classCopy by lazy { getCharacterClassCopyUseCase() }

    private val computerOpponentHandler =
        ComputerOpponentHandler(
            arenaId = arguments.arenaId,
            computerOpponentIds = arguments.computerOpponents.map { it.userId }.toSet(),
            coroutineScope = viewModelScope,
            chooseCharacterClassUseCase = chooseCharacterClassUseCase,
            performCombatActionUseCase = performCombatActionUseCase,
            stayForAnotherMatchUseCase = stayForAnotherMatchUseCase,
        )

    private val chooseCharacterClassHandler =
        ChooseCharacterClassHandler(
            arenaId = arguments.arenaId,
            chooseCharacterClassUseCase = chooseCharacterClassUseCase,
            sessionStats = sessionStats,
            pendingStatsStore = pendingStatsStore,
        )

    private val combatHandler =
        CombatHandler(
            arenaId = arguments.arenaId,
            performCombatActionUseCase = performCombatActionUseCase,
        )

    // TODO: Handler for all timer-related operations
    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onTimerExpiration,
        )

    private var screenStateFlowJob: Job? = null

    private var finishWriteJob: Job? = null

    private var previousPlayerNames: Map<String, String> = emptyMap()

    init {
        loadArenaMatch()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadArenaMatch()
    }

    fun onFlashComplete() {
        messageQueue.onFlashComplete()
    }

    fun onAvatarComplete() {
        messageQueue.onAvatarComplete()
    }

    fun onTargetOpponent(opponentUserId: String) {
        _uiStateFlow.updateLoaded { screenState ->
            combatHandler.targetOpponent(screenState = screenState, opponentUserId = opponentUserId)
        }
    }

    fun onAttack(attackArguments: AttackArguments) {
        _uiStateFlow.updateLoaded { screenState ->
            combatHandler.updateLockedTargetUserId(
                screenState = screenState,
                lockedTargetUserId = attackArguments.targetedOpponentUserId,
            )
        }

        viewModelScope.launch {
            combatHandler.onNormalAttack(
                attackArguments = attackArguments,
                arenaMatchState = _uiStateFlow.getLoadedStateOrNull()?.arenaMatchState,
            )
        }
    }

    fun onUseAbility(useAbilityArguments: UseAbilityArguments) {
        _uiStateFlow.updateLoaded { screenState ->
            combatHandler.updateIsActionInFlight(screenState = screenState)
        }

        viewModelScope.launch {
            combatHandler.onUseAbility(
                useAbilityArguments = useAbilityArguments,
                arenaMatchState = _uiStateFlow.getLoadedStateOrNull()?.arenaMatchState,
            )
        }
    }

    fun onTryLeaveArena() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = true) }
    }

    fun onConfirmLeaveArena() {
        screenStateFlowJob?.cancel()
        finishWriteJob?.cancel()
        computerOpponentHandler.cancel()
        // playbackSequencer.cancel()

        val userKnockedOut = _uiStateFlow.getLoadedStateOrNull()?.isUserPlayerKnockedOut ?: false
        sessionStats.onLeave(userKnockedOut = userKnockedOut)

        viewModelScope.launch {
            exitArenaUseCase(arenaId = arguments.arenaId)
            setCurrentTravelerAvailableUseCase()
            flushSessionStats()
            _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
            _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
        }
    }

    fun onSelectCharacterClass(characterClassState: CharacterClassState) {
        _uiStateFlow.updateLoaded { screenState ->
            chooseCharacterClassHandler.selectCharacterClass(
                screenState = screenState,
                characterClassState = characterClassState,
            )
        }
    }

    fun onChooseCharacterClass(playerState: PlayerState, characterClassState: CharacterClassState) {
        viewModelScope.launch {
            val beforeClassSelectedArenaScreenState =
                _uiStateFlow.getLoadedStateOrNull() ?: return@launch

            val afterClassSelectedArenaScreenState =
                chooseCharacterClassHandler.chooseCharacterClass(
                    screenState = beforeClassSelectedArenaScreenState,
                    playerState = playerState,
                    characterClassState = characterClassState,
                ) ?: return@launch

            _uiStateFlow.updateLoaded { afterClassSelectedArenaScreenState }
        }
    }

    fun onDiceSettled() {
        viewModelScope.launch { messageQueue.onCombatActionResolved() }
    }

    fun onDismissForfeitDialog() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
    }

    fun onStayForAnotherMatch() {
        viewModelScope.launch {
            stayForAnotherMatchUseCase(arenaId = arguments.arenaId)
            computerOpponentHandler.readyUpForNextMatch()

            _uiStateFlow.updateLoaded { screenState ->
                screenState.copy(hasReadiedForNextMatch = true)
            }
        }
    }

    private fun loadArenaMatch() {
        screenStateFlowJob?.cancel()
        finishWriteJob?.cancel()
        screenStateFlowJob =
            viewModelScope.launch {
                val userId = getUserIdUseCase()
                if (userId == null) {
                    _uiStateFlow.value = UiState.Error
                    return@launch
                }

                coordinateArenaMatchUseCase(
                    arenaId = arguments.arenaId,
                    expectedNumberOfPlayers = arguments.expectedNumberOfPlayers,
                    computerOpponents = arguments.computerOpponents,
                )
                    .catch { _uiStateFlow.value = UiState.Error }
                    .collect { updatedArenaMatch ->
                        updatedArenaMatch.updateScreenState(userId = userId)
                    }
            }
    }

    private fun onAnimationsComplete(arenaMatchCombatUpdate: ArenaMatch.Combat) {
        _uiStateFlow.updateLoaded { screenState ->
            updateCombatState(
                currentScreenState = screenState,
                arenaMatchCombatUpdate = arenaMatchCombatUpdate,
            )
        }

        // TODO: Should be done automatically, when MessageQueue is working. Somehow
        val target = _uiStateFlow.getLoadedStateOrNull()?.arenaMatchState
        if (target is ArenaMatchState.Finished) finishMatchAfterPlayback()
    }

    // TODO: Should be done automatically, when MessageQueue is working
    private fun finishMatchAfterPlayback() {
        if (finishWriteJob?.isActive == true) return
        finishWriteJob = viewModelScope.launch { finishMatchUseCase(arenaId = arguments.arenaId) }
    }

    private fun updateCombatState(
        currentScreenState: ArenaScreenState,
        arenaMatchCombatUpdate: ArenaMatch.Combat,
    ): ArenaScreenState {
        val currentArenaCombatState = currentScreenState.arenaMatchState as? ArenaMatchState.Combat ?: return currentScreenState

        val userId = currentArenaCombatState.userPlayerId
        val finalArenaMatchUpdate = currentArenaCombatState.copy(
            playerMap = arenaMatchCombatUpdate.playerMap.mapValues { it.value.toSelectedState() },
            combatActionState = arenaMatchCombatUpdate.nextCombatAction.toCombatActionState(),
            roundNumber = arenaMatchCombatUpdate.roundNumber,
            isNewRound = arenaMatchCombatUpdate.isNewRound,
            isMatchOver = arenaMatchCombatUpdate.isMatchOver,
            turnOwnerUserId = arenaMatchCombatUpdate.nextCombatAction.turnOwnerUserId(),
            lockedTargetUserId = arenaMatchCombatUpdate.nextCombatAction.lockedTargetUserId(userId = userId),
            selectedOpponentUserId = currentArenaCombatState.selectedOpponentUserId, // TODO: Correct?
            isActionInFlight = false,
        )

        return currentScreenState.copy(
            arenaMatchState = finalArenaMatchUpdate,
            animatingMessage = null,
        )
    }


    private suspend fun flushSessionStats() {
        val delta = sessionStats.consumeDelta()
        val wasWritten = updateProfileStatsUseCase(delta = delta)
        if (wasWritten || delta.isEmpty) {
            pendingStatsStore.clear()
        } else {
            pendingStatsStore.save(delta)
        }
    }

    private suspend fun persistPendingStats() {
        pendingStatsStore.save(delta = sessionStats.peekDelta())
    }

    // TODO August 14th:
    // TODO: I think it would be best to ALWAYS use copy(). So, this means that the currentScreenState
    //       and the update coming from the flow must always be compared, like Combat to Combat, etc.
    //       This might result in return null or something for combinations that would never exist
    //       but those can just be considered an error state I guess. The big benefit will be that
    //       only things that are really changing will be changed, instead brand-new object creation
    //       all the time.
    private suspend fun ArenaMatch.handleScreenUpdate(
        userId: String,
        currentScreenState: ArenaScreenState,
    ) {
        // TODO: See where this fits in new branching organization
        val recordedStat = sessionStats.onMatchUpdate(match = this, userId = userId)
        if (recordedStat) {
            persistPendingStats()
        }

        notifyDepartedOpponents(userId = userId)

        val currentArenaState = currentScreenState.arenaMatchState
        if (currentArenaState is ArenaMatchState.WaitingForAllPlayers && this is ArenaMatch.ChoosingClasses) {
            // TODO: This is a transition for all people joining, so no copy() of ArenaState
            val newArenaState = toChoosingClassesState(userPlayerId = userId)
            currentScreenState.copy(

            )
        }

        // TODO: ONLY COMBAT TO COMBAT USES THE QUEUE

        val updatedArenaMatchState =     when (this) {
            is ArenaMatch.ChoosingClasses -> {
                // TODO: if currentScreenState is finished, clear messages
                val (unavailable, selections) = playerMap.prepareClassSelectionInfo(userId = userId)
                val previousClassSelection = (currentScreenState.arenaMatchState as? ArenaMatchState.ChoosingClasses)?.selectedClass


                ArenaMatchState.ChoosingClasses(
                    playerMap = playerMap.mapValues { it.value.toState() },
                    activePlayerUserId = activePlayer.userId,
                    userPlayerId = userId,
                    unavailableClasses = unavailable,
                    classSelections = selections,
                    selectedClass =
                        characterClasses.resolveSelectedClass(
                            previousSelection = previousClassSelection,
                            unavailableClasses = unavailable,
                        ),
                )
            }
            is ArenaMatch.Combat -> {
                // TODO: Function
                messageQueue.enqueue(arenaMatchUpdate = this)

                _uiStateFlow.updateLoaded { screenState ->
                    val arenaCombatState =
                        screenState.arenaMatchState as? ArenaMatchState.Combat ?: return@updateLoaded screenState

                    screenState.copy(
                        arenaMatchState = arenaCombatState.copy(
                            diceRollState = this.diceRoll?.toState()
                        )
                    )
                }
            }
            is ArenaMatch.Finished -> {
                //
            }
            is ArenaMatch.WaitingForAllPlayers -> {
                // TODO: if currentScreenState is finished, clear messages
            }
        }

        if (this is ArenaMatch.Combat) {

        }
    }

    // TODO: Refactor:
    //       If previous ScreenState is null
    private suspend fun ArenaMatch?.updateScreenState(userId: String) {
        val currentScreenState = _uiStateFlow.getLoadedStateOrNull()

        when {
            this == null -> handleScreenStateUpdateError()
            currentScreenState == null -> handleNewArenaScreen(userId = userId)
            else -> handleScreenUpdate(userId = userId, currentScreenState = currentScreenState)
        }


        val previousState = _uiStateFlow.getLoadedStateOrNull()

        val updatedArenaStateUpdate =
            toUpdatedState(
                previousState = previousState,
                userId = userId,
                newMessages = messageUpdate.newMessages,
                characterClasses = characterClasses,
                classCopy = classCopy,
            )

        handleTimerAction(timerAction = updatedArenaStateUpdate.timerAction)

        // TODO: Adjust computer as necessary
        val nextState = updatedArenaStateUpdate.nextState
        computerOpponentHandler.onArenaState(
            arenaMatchState = nextState.arenaMatchState,
            characterClasses = nextState.characterClasses,
        )
    }

    // TODO: if there's one match error, just kill the whole thing? Or determine what to do if
    //       happens in the middle of a match? For now, probably just show an error then leave.
    private suspend fun handleScreenStateUpdateError() {
        screenStateFlowJob?.cancel()
        finishWriteJob?.cancel()

        setCurrentTravelerAvailableUseCase()
        flushSessionStats()
        _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
    }

    private suspend fun ArenaMatch.handleNewArenaScreen(userId: String) {
        // TODO: See where this fits in new branching organization
        val recordedStat = sessionStats.onMatchUpdate(match = this, userId = userId)
        if (recordedStat) {
            persistPendingStats()
        }

        val (arenaState, timerAction) = when (this) {
            is ArenaMatch.WaitingForAllPlayers ->
                ArenaMatchState.WaitingForAllPlayers to TimerAction.Start(timerDuration = WAITING_TIMER_DURATION_SECONDS)
            is ArenaMatch.ChoosingClasses ->
                ArenaMatchState.ChoosingClasses(
                    playerMap = playerMap.mapValues { it.value.toState() },
                    userPlayerId = userId,
                    activePlayerUserId = activePlayerUserId,
                    selectedClass = null,
                    unavailableClasses = emptySet(),
                    classSelections = emptyMap(),
                ) to TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS)
            is ArenaMatch.Combat,
            is ArenaMatch.Finished -> {
                handleScreenStateUpdateError()
                return
            }
        }


        // TODO: determine timer better
        val newArenaScreenState = ArenaScreenState(
            arenaMatchState = arenaState,
            secondsRemaining = timerAction.timerDuration,
            messages = messages.toMessageStates(), // TODO: unnecessary? Maybe just for now. Wasn't used before
            showForfeitDialog = false,
            isUserPlayerKnockedOut = false,
            hasReadiedForNextMatch = false,
            gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet),
            characterClasses = characterClasses,
            classCopy = classCopy
        )

        _uiStateFlow.value = UiState.Loaded(
            data = newArenaScreenState
        )
    }

    private fun triggerAnimation(message: ArenaMessageState?) {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(animatingMessage = message) }
    }

    private fun addMessageToScreenState(message: ArenaMessageState) {
        _uiStateFlow.updateLoaded { screenState ->
            screenState.copy(
                messageGroups = screenState.messageGroups.addNewMessageDuringCombat(message = message)
            )
        }
    }

    private fun ArenaMatch.clearMessagesIfApplicable() {
        if (this !is ArenaMatch.Combat) return

        // TODO: Think this is wrong. Needs to be emptied when going from Finished to Choosing
        if (isMatchOver) {
            // TODO: signal that messages should be emptied
        }
    }

    // TODO: Use previous state. If not a match, then someone left...?
    private fun ArenaMatch.notifyDepartedOpponents(userId: String) {
        val currentPlayerIds = playerMap.keys
        val departedOpponentNames =
            previousPlayerNames.filterKeys { id -> id != userId && id !in currentPlayerIds }.values

        departedOpponentNames.forEach { displayName ->
            viewModelScope.launch {
                _eventChannelFlow.send(element = ArenaEvent.OpponentLeft(displayName = displayName))
            }
        }

        previousPlayerNames = playerMap.mapValues { (_, player) -> player.displayName }
    }

    private fun handleTimerAction(timerAction: TimerAction) {
        when (timerAction) {
            is TimerAction.Start -> arenaTimer.startTimer(timerDuration = timerAction.timerDuration)

            is TimerAction.Stop -> arenaTimer.stopTimer()

            is TimerAction.None -> {
                /* No changes needed */
            }
        }
    }

    private fun updateArenaStateSeconds(secondsRemaining: Int) {
        _uiStateFlow.updateLoaded { screenState ->
            screenState.copy(secondsRemaining = secondsRemaining)
        }
    }

    private suspend fun onTimerExpiration() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        when (val arenaState = loadedState.arenaMatchState) {
            is ArenaMatchState.WaitingForAllPlayers -> leaveTimedOutMatch()

            is ArenaMatchState.ChoosingClasses -> {
                val afterClassSelectedArenaScreenState =
                    chooseCharacterClassHandler.autoChooseCharacterClassForUser(
                        screenState = loadedState,
                        arenaMatchState = arenaState,
                    ) ?: return

                _uiStateFlow.updateLoaded { afterClassSelectedArenaScreenState }
            }

            is ArenaMatchState.Combat -> autoResolveCombatForUser(arenaMatchState = arenaState)

            is ArenaMatchState.Finished ->
                if (!loadedState.hasReadiedForNextMatch) onStayForAnotherMatch()
        }
    }

    private suspend fun leaveTimedOutMatch() {
        screenStateFlowJob?.cancel()
        finishWriteJob?.cancel()
        computerOpponentHandler.cancel()
        // playbackSequencer.cancel()

        exitArenaUseCase(arenaId = arguments.arenaId)
        setCurrentTravelerAvailableUseCase()
        flushSessionStats()
        _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
    }

    private fun autoResolveCombatForUser(arenaMatchState: ArenaMatchState.Combat) {
        val userPlayerId = arenaMatchState.userPlayerId
        if (userPlayerId !in arenaMatchState.combatActionState.activeUserIds) return

        when (val actionState = arenaMatchState.combatActionState) {
            is CombatActionState.Idle ->
                autoAttackRandomOpponent(arenaMatchState = arenaMatchState, attackerId = userPlayerId)

            is CombatActionState.NormalAttack ->
                onAttack(
                    attackArguments =
                        AttackArguments(
                            targetedOpponentUserId = actionState.defenderUserId,
                            userPlayerId = userPlayerId,
                            combatActionState = actionState,
                        )
                )

            is CombatActionState.AbilityRoll ->
                onUseAbility(
                    useAbilityArguments =
                        UseAbilityArguments(
                            abilityActionState = actionState.action,
                            targetedOpponentUserId =
                                (actionState.action as? AbilityActionState.Passive.Persuade.Charm)
                                    ?.targetableUserIds
                                    ?.randomOrNull()
                                    .orEmpty(),
                            userPlayerId = userPlayerId,
                            combatActionState = actionState,
                        )
                )
        }
    }

    private fun autoAttackRandomOpponent(arenaMatchState: ArenaMatchState.Combat, attackerId: String) {
        val target =
            arenaMatchState.playerMap.values
                .filter { player ->
                    player.userId != attackerId && player.characterClass.hitPoints > 0
                }
                .randomOrNull() ?: return

        onAttack(
            attackArguments =
                AttackArguments(
                    targetedOpponentUserId = target.userId,
                    userPlayerId = attackerId,
                    combatActionState = arenaMatchState.combatActionState,
                )
        )
    }
}
