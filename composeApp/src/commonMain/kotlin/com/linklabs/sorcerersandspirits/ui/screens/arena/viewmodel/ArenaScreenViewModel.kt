package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.attackerUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerClassType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.DisconnectArenaPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.FinishMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.GetArenaPresenceFlowUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.RemoveDisconnectedPlayerUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffectPlayer
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isUserTheRoller
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
    private val disconnectArenaPresenceUseCase: DisconnectArenaPresenceUseCase,
    private val exitArenaUseCase: ExitArenaUseCase,
    private val finishMatchUseCase: FinishMatchUseCase,
    private val getArenaPresenceFlowUseCase: GetArenaPresenceFlowUseCase,
    private val removeDisconnectedPlayerUseCase: RemoveDisconnectedPlayerUseCase,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getCharacterClassCopyUseCase: GetCharacterClassCopyUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val updateProfileStatsUseCase: UpdateProfileStatsUseCase,
    private val pendingStatsStore: PendingStatsStore,
    private val soundEffectPlayer: SoundEffectPlayer,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _timerStateFlow = MutableStateFlow(value = ArenaTimerState(secondsRemaining = 0))
    val timerStateFlow = _timerStateFlow.asStateFlow()

    private val _selectedOpponentUserIdFlow = MutableStateFlow<String?>(value = null)
    val selectedOpponentUserIdFlow = _selectedOpponentUserIdFlow.asStateFlow()

    private val arenaMatchCombatQueue =
        ArenaMatchCombatQueue(
            triggerAnimation = ::triggerAnimation,
            playSoundEffect = soundEffectPlayer::play,
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
    private var arenaPresenceFlowJob: Job? = null

    // TODO: Purpose of this? What "ghost false entry"?
    private val exitedDisconnectedPlayerIds = mutableSetOf<String>()

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
        _selectedOpponentUserIdFlow.value = opponentUserId
    }

    // TODO: What arguments can be passed to reduce the madness of these early returns
    fun onChooseCharmTarget(targetUserId: String) {
        val combatState =
            _uiStateFlow.getLoadedStateOrNull()?.arenaMatchState as? ArenaMatchState.Combat ?: return
        val abilityRoll =
            combatState.combatActionState as? CombatActionState.AbilityRoll ?: return
        val charm =
            abilityRoll.action as? AbilityActionState.Passive.Persuade.Charm ?: return
        if (targetUserId !in charm.targetableUserIds) return

        onUseAbility(
            useAbilityArguments =
            UseAbilityArguments(
                abilityActionState = charm,
                targetedOpponentUserId = targetUserId,
                userPlayerId = combatState.userPlayerId,
                combatActionState = abilityRoll,
            ),
        )
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
        arenaPresenceFlowJob?.cancel()

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

                // TODO: Should this be nested in screenStateFlowJob?
                observeArenaPresence(userId = userId)

                coordinateArenaMatchUseCase(
                    arenaId = arguments.arenaId,
                    expectedNumberOfPlayers = arguments.expectedNumberOfPlayers,
                )
                    .catch { _uiStateFlow.value = UiState.Error }
                    .collect { updatedArenaMatch ->
                        updatedArenaMatch.updateScreenState(userId = userId)
                    }
            }
    }

    private fun observeArenaPresence(userId: String) {
        arenaPresenceFlowJob?.cancel()
        arenaPresenceFlowJob =
            viewModelScope.launch {
                getArenaPresenceFlowUseCase(arenaId = arguments.arenaId).collect { presenceByUserId ->
                    removeDisconnectedPlayers(presenceByUserId = presenceByUserId, userId = userId)
                }
            }
    }

    private suspend fun removeDisconnectedPlayers(
        presenceByUserId: Map<String, Boolean>,
        userId: String,
    ) {
        val newlyDisconnectedPlayerIds =
            presenceByUserId
                .filter { (playerId, isConnected) ->
                    val isDisconnected = !isConnected
                    val isSelf = playerId == userId
                    val isNotYetRemoved = playerId !in exitedDisconnectedPlayerIds
                    isDisconnected && !isSelf && isNotYetRemoved
                }
                .keys

        newlyDisconnectedPlayerIds.forEach { disconnectedPlayerId ->
            exitedDisconnectedPlayerIds += disconnectedPlayerId
            removeDisconnectedPlayerUseCase(
                arenaId = arguments.arenaId,
                userId = disconnectedPlayerId,
            )
        }
    }

    private suspend fun ArenaMatch?.updateScreenState(userId: String) {
        val currentScreenState = _uiStateFlow.getLoadedStateOrNull()

        if (this == null) {
            handleScreenStateUpdateError()
            return
        }

        val isUserRemovedFromMatch = userId !in playerMap
        if (isUserRemovedFromMatch) {
            handleRemovedFromMatch()
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
        _selectedOpponentUserIdFlow.value = null

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
        val doesTurnContinueForActor =
            currentArenaCombatState.attackerUserId in nextCombatAction.activeUserIds
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
                currentTargetUserId =
                arenaMatchCombatUpdate.currentTargetUserId.takeIf { doesTurnContinueForActor },
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

    private fun handleRemovedFromMatch() {
        screenStateFlowJob?.cancel()
        arenaPresenceFlowJob?.cancel()

        viewModelScope.launch {
            disconnectArenaPresenceUseCase()
            setCurrentTravelerAvailableUseCase()
            flushSessionStats()
            _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
        }
    }

    // TODO(SS-260): if there's one match error, just kill the whole thing? Or determine what to do
    // if
    //       happens in the middle of a match? For now, probably just show an error then leave.
    private suspend fun handleScreenStateUpdateError() {
        screenStateFlowJob?.cancel()
        arenaPresenceFlowJob?.cancel()

        disconnectArenaPresenceUseCase()
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

            is TimerAction.Stop -> {
                arenaTimer.stopTimer()
                _timerStateFlow.value = ArenaTimerState(secondsRemaining = 0)
            }

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

            is ArenaMatchState.Finished -> {
                val isGauntletComplete = loadedState.gauntletProgressState.isFinalMatch
                val shouldAutoReady = !isGauntletComplete && !loadedState.hasReadiedForNextMatch
                if (shouldAutoReady) onStayForAnotherMatch()
            }
        }
    }

    private suspend fun leaveTimedOutMatch() {
        screenStateFlowJob?.cancel()
        arenaPresenceFlowJob?.cancel()

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

            is CombatActionState.AbilityRoll -> {
                val isUserStillOwedThisRoll =
                    actionState.action.isUserTheRoller(userPlayerId = userPlayerId)
                if (!isUserStillOwedThisRoll) return

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
