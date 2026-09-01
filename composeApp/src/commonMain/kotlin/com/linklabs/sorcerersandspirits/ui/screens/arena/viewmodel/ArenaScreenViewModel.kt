package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.attackerUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerClassType
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaEvent
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaSessionStats
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenStateUpdate
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CHOOSING_CLASSES_TIMER_DURATION_SECONDS
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgressState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.WAITING_TIMER_DURATION_SECONDS
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.addNewMessageDuringCombat
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.appendNewMessages
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.players
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toChoosingClassesState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toCombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toSelectedState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toUpdatedScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.ArenaMatchCombatQueue
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.ChooseCharacterClassHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.CombatHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers.TeleportRollsArguments
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

    private val _timerStateFlow = MutableStateFlow(value = ArenaTimerState(secondsRemaining = 0))
    val timerStateFlow = _timerStateFlow.asStateFlow()

    private val arenaMatchCombatQueue =
        ArenaMatchCombatQueue(
            triggerAnimation = ::triggerAnimation,
            addMessageToScreenState = ::addMessageToScreenState,
            onAnimationsComplete = ::onAnimationsComplete,
            onTeleportRollOffComplete = ::onTeleportRollOffComplete,
            onHeaderAnimationsComplete = ::onHeaderAnimationsComplete,
            finishMatch = { finishMatchUseCase(arenaId = arguments.arenaId) },
        )

    // TODO(SS-260): Probably a handler for this
    private val sessionStats = ArenaSessionStats()

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }
    private val classCopy by lazy { getCharacterClassCopyUseCase() }

    // TODO: Reinstate computerOpponentHandler later
    /*
    private val computerOpponentHandler =
        ComputerOpponentHandler(
            arenaId = arguments.arenaId,
            computerOpponentIds = arguments.computerOpponents.map { it.userId }.toSet(),
            coroutineScope = viewModelScope,
            chooseCharacterClassUseCase = chooseCharacterClassUseCase,
            performCombatActionUseCase = performCombatActionUseCase,
            stayForAnotherMatchUseCase = stayForAnotherMatchUseCase,
        )
     */

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

    // TODO(SS-260): Handler for all timer-related operations
    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onTimerExpiration,
        )

    private var screenStateFlowJob: Job? = null

    init {
        loadArenaMatch()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadArenaMatch()
    }

    internal fun onAnimationComplete(arguments: AnimationCompleteArguments) {
        arenaMatchCombatQueue.onAnimationComplete(arguments = arguments)
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
        // computerOpponentHandler.cancel()

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
            val currentScreenState = _uiStateFlow.getLoadedStateOrNull() ?: return@launch

            val didChooseClass =
                chooseCharacterClassHandler.chooseCharacterClass(
                    screenState = currentScreenState,
                    playerState = playerState,
                    characterClassState = characterClassState,
                )
            if (!didChooseClass) return@launch

            _uiStateFlow.updateLoaded { screenState ->
                chooseCharacterClassHandler.clearSelectedClass(screenState = screenState)
            }
        }
    }

    fun onDiceSettled(userId: String) {
        viewModelScope.launch { arenaMatchCombatQueue.onCombatActionResolved(userId = userId) }
    }

    fun onDismissForfeitDialog() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
    }

    fun onStayForAnotherMatch() {
        viewModelScope.launch {
            stayForAnotherMatchUseCase(arenaId = arguments.arenaId)
            // computerOpponentHandler.readyUpForNextMatch()

            _uiStateFlow.updateLoaded { screenState ->
                screenState.copy(hasReadiedForNextMatch = true)
            }
        }
    }

    private fun loadArenaMatch() {
        screenStateFlowJob?.cancel()
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

    private suspend fun ArenaMatch?.updateScreenState(userId: String) {
        val currentScreenState = _uiStateFlow.getLoadedStateOrNull()

        if (this == null) {
            handleScreenStateUpdateError()
            return
        }

        // TODO(SS-260): Refactor sessionStats
        val recordedStat = sessionStats.onMatchUpdate(match = this, userId = userId)
        if (recordedStat) {
            persistPendingStats()
        }

        if (currentScreenState == null) {
            handleNewArenaScreen(userId = userId)
        } else {
            handleScreenUpdate(userId = userId, currentScreenState = currentScreenState)
        }

        // coordinateComputerOpponents()
    }

    private suspend fun ArenaMatch.handleNewArenaScreen(userId: String) {
        val newScreenStateUpdate =
            when (this) {
                is ArenaMatch.WaitingForAllPlayers ->
                    getNewArenaScreenState(
                        arenaMatchState = ArenaMatchState.WaitingForAllPlayers,
                        timerDuration = WAITING_TIMER_DURATION_SECONDS,
                    )

                is ArenaMatch.ChoosingClasses ->
                    getNewArenaScreenState(
                        arenaMatchState =
                        toChoosingClassesState(
                            userPlayerId = userId,
                            defaultSelectedClass = characterClasses.firstOrNull(),
                        ),
                        timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS,
                    )

                is ArenaMatch.Combat,
                is ArenaMatch.Finished,
                -> null
            }

        if (newScreenStateUpdate == null) {
            handleScreenStateUpdateError()
            return
        }

        handleTimerAction(timerAction = newScreenStateUpdate.timerAction)
        _uiStateFlow.value = UiState.Loaded(data = newScreenStateUpdate.screenState)
    }

    private fun ArenaMatch.getNewArenaScreenState(
        arenaMatchState: ArenaMatchState,
        timerDuration: Int,
    ): ArenaScreenStateUpdate = ArenaScreenStateUpdate(
        screenState =
        ArenaScreenState(
            arenaMatchState = arenaMatchState,
            showForfeitDialog = false,
            isUserPlayerKnockedOut = false,
            hasReadiedForNextMatch = false,
            messageGroups =
            emptyList<ArenaMessageGroupState>()
                .appendNewMessages(incomingMessages = messages),
            gauntletProgressState = GauntletProgressState(currentMatch = matchNumberInGauntlet),
            characterClasses = characterClasses,
            classCopy = classCopy,
            animatingMessage = null,
        ),
        timerAction = TimerAction.Start(timerDuration = timerDuration),
        messageQueueUpdate = null,
    )

    private fun ArenaMatch.handleScreenUpdate(
        userId: String,
        currentScreenState: ArenaScreenState,
    ) {
        notifyDepartedOpponents(userId = userId, currentScreenState = currentScreenState)

        val screenStateUpdate =
            toUpdatedScreenState(userId = userId, currentScreenState = currentScreenState)

        applyScreenStateUpdate(screenStateUpdate = screenStateUpdate, userId = userId)
    }

    private fun applyScreenStateUpdate(screenStateUpdate: ArenaScreenStateUpdate, userId: String) {
        handleTimerAction(timerAction = screenStateUpdate.timerAction)

        val messageQueueUpdate = screenStateUpdate.messageQueueUpdate
        if (messageQueueUpdate != null) {
            arenaMatchCombatQueue.enqueue(arenaMatchUpdate = messageQueueUpdate)

            // TODO: Update this to be better than enqueueing then immediately dequeue-ing
            val hasNoDiceToSettle = messageQueueUpdate.diceRoll == null
            if (hasNoDiceToSettle) {
                viewModelScope.launch {
                    arenaMatchCombatQueue.onCombatActionResolved(userId = userId)
                }
            }
        }

        _uiStateFlow.updateLoaded { screenStateUpdate.screenState }
    }

    private fun onAnimationsComplete(arenaMatchCombatUpdate: ArenaMatch.Combat) {
        _uiStateFlow.updateLoaded { screenState ->
            updateCombatState(
                currentScreenState = screenState,
                arenaMatchCombatUpdate = arenaMatchCombatUpdate,
            )
        }
    }

    private fun onTeleportRollOffComplete(teleportRollsArguments: TeleportRollsArguments) {
        viewModelScope.launch {
            combatHandler.resolveTeleportRollOff(teleportRollsArguments = teleportRollsArguments)
        }
    }

    private fun onHeaderAnimationsComplete() {
        _uiStateFlow.updateLoaded { screenState ->
            val arenaMatchState = screenState.arenaMatchState as? ArenaMatchState.Combat ?: return@updateLoaded screenState

            screenState.copy(
                animatingMessage = null,
                arenaMatchState = arenaMatchState.copy(
                    isActionInFlight = false,

                ),
            )
        }
    }

    private fun updateCombatState(
        currentScreenState: ArenaScreenState,
        arenaMatchCombatUpdate: ArenaMatch.Combat,
    ): ArenaScreenState {
        val currentArenaCombatState =
            currentScreenState.arenaMatchState as? ArenaMatchState.Combat
                ?: return currentScreenState

        val nextCombatAction = arenaMatchCombatUpdate.nextCombatAction
        val finalArenaMatchUpdate =
            currentArenaCombatState.copy(
                playerMap =
                arenaMatchCombatUpdate.playerMap.mapValues { it.value.toSelectedState() },
                combatActionState = nextCombatAction.toCombatActionState(),
                roundNumber = arenaMatchCombatUpdate.roundNumber,
                isNewRound = arenaMatchCombatUpdate.isNewRound,
                isMatchOver = arenaMatchCombatUpdate.isMatchOver,
                attackerUserId = nextCombatAction.attackerUserId(),
                turnOwnerClassType = arenaMatchCombatUpdate.turnOwnerClassType(),
                lockedTargetUserId =
                nextCombatAction.lockedTargetUserId(
                    userId = currentArenaCombatState.userPlayerId,
                ),
                selectedOpponentUserId =
                currentArenaCombatState.selectedOpponentUserId, // TODO(SS-260): Correct?
                isActionInFlight = false,
            )

        val continuouslyAnimatingMessage =
            currentScreenState.animatingMessage?.takeIf { it.isContinuous() }

        return currentScreenState.copy(
            arenaMatchState = finalArenaMatchUpdate,
            animatingMessage = continuouslyAnimatingMessage,
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

    /*
    private fun coordinateComputerOpponents() {
        val updatedScreenState = _uiStateFlow.getLoadedStateOrNull() ?: return
        computerOpponentHandler.onArenaState(
            arenaMatchState = updatedScreenState.arenaMatchState,
            characterClasses = updatedScreenState.characterClasses,
        )
    }
     */

    // TODO(SS-260): if there's one match error, just kill the whole thing? Or determine what to do
    // if
    //       happens in the middle of a match? For now, probably just show an error then leave.
    private suspend fun handleScreenStateUpdateError() {
        screenStateFlowJob?.cancel()

        setCurrentTravelerAvailableUseCase()
        flushSessionStats()
        _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
    }

    private fun triggerAnimation(message: ArenaMessageState?) {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(animatingMessage = message) }
    }

    private fun addMessageToScreenState(message: ArenaMessageState) {
        _uiStateFlow.updateLoaded { screenState ->
            screenState.copy(
                messageGroups =
                screenState.messageGroups.addNewMessageDuringCombat(message = message),
            )
        }
    }

    // TODO(SS-260): Maybe the actual arenaMatchState can be passed in during a branching update?
    private fun ArenaMatch.notifyDepartedOpponents(
        userId: String,
        currentScreenState: ArenaScreenState,
    ) {
        val currentPlayerIds = playerMap.keys
        currentScreenState.arenaMatchState
            .players()
            .filter { it.userId != userId && it.userId !in currentPlayerIds }
            .forEach { departedOpponent ->
                viewModelScope.launch {
                    _eventChannelFlow.send(
                        element =
                        ArenaEvent.OpponentLeft(displayName = departedOpponent.displayName),
                    )
                }
            }
    }

    private fun handleTimerAction(timerAction: TimerAction) {
        when (timerAction) {
            is TimerAction.Start -> {
                _timerStateFlow.value =
                    ArenaTimerState(secondsRemaining = timerAction.timerDuration)
                arenaTimer.startTimer(timerDuration = timerAction.timerDuration)
            }

            is TimerAction.Stop -> arenaTimer.stopTimer()

            is TimerAction.None -> {
                /* No changes needed */
            }
        }
    }

    private fun updateArenaStateSeconds(secondsRemaining: Int) {
        _timerStateFlow.value = ArenaTimerState(secondsRemaining = secondsRemaining)
    }

    private suspend fun onTimerExpiration() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        when (val arenaState = loadedState.arenaMatchState) {
            is ArenaMatchState.WaitingForAllPlayers -> leaveTimedOutMatch()

            is ArenaMatchState.ChoosingClasses -> {
                val didChooseClass =
                    chooseCharacterClassHandler.autoChooseCharacterClassForUser(
                        screenState = loadedState,
                        arenaMatchState = arenaState,
                    )
                if (!didChooseClass) return

                _uiStateFlow.updateLoaded { screenState ->
                    chooseCharacterClassHandler.clearSelectedClass(screenState = screenState)
                }
            }

            is ArenaMatchState.Combat -> autoResolveCombatForUser(arenaMatchState = arenaState)

            is ArenaMatchState.Finished ->
                if (!loadedState.hasReadiedForNextMatch) onStayForAnotherMatch()
        }
    }

    private suspend fun leaveTimedOutMatch() {
        screenStateFlowJob?.cancel()
        // computerOpponentHandler.cancel()

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
                autoAttackRandomOpponent(
                    arenaMatchState = arenaMatchState,
                    attackerId = userPlayerId,
                )

            is CombatActionState.NormalAttack ->
                onAttack(
                    attackArguments =
                    AttackArguments(
                        targetedOpponentUserId = actionState.defenderUserId,
                        userPlayerId = userPlayerId,
                        combatActionState = actionState,
                    ),
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
                    ),
                )
        }
    }

    private fun autoAttackRandomOpponent(
        arenaMatchState: ArenaMatchState.Combat,
        attackerId: String,
    ) {
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
            ),
        )
    }
}
