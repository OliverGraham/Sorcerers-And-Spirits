package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.FinishMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassCopyUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.computer.ComputerOpponentHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.plusMessage
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toUpdatedState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.MessageQueue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain
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

    private val sessionStats = ArenaSessionStats()

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val messageQueue =
        MessageQueue(addAnimatingMessageToScreenState = ::addAnimatingMessageToScreenState)

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

    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onTimerExpiration,
        )

    private var screenStateFlowJob: Job? = null

    private var finishWriteJob: Job? = null

    private var previousPlayerNames: Map<String, String> = emptyMap()

    private val seenMessageIds = mutableSetOf<String>()
    private var lastMatchNumber: Int? = null

   /* private val playbackSequencer =
        CombatPlaybackSequencer(
            scope = viewModelScope,
            emit = { target, compose ->
                _uiStateFlow.update { state ->
                    when (state) {
                        is UiState.Loaded -> UiState.Loaded(data = compose(state.data))
                        is UiState.Loading -> UiState.Loaded(data = compose(target))
                        is UiState.Error -> state
                    }
                }
            },
            appendMessage = ::addMessage,
            onMatchPlaybackComplete = ::finishMatchAfterPlayback,
        )
*/
    init {
        // playbackSequencer.start()
        loadArenaMatch()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadArenaMatch()
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

    fun onSelectCharacterClass(characterClassState: CharacterClassState) {
        _uiStateFlow.updateLoaded { screenState ->
            val arenaState =
                screenState.arenaState as? ArenaState.ChoosingClasses
                    ?: return@updateLoaded screenState

            screenState.copy(arenaState = arenaState.copy(selectedClass = characterClassState))
        }
    }

    fun onChooseCharacterClass(playerState: PlayerState, characterClassState: CharacterClassState) {
        viewModelScope.launch {
            val arenaState =
                _uiStateFlow.getLoadedStateOrNull()?.arenaState as? ArenaState.ChoosingClasses
                    ?: return@launch

            val selectedArenaPlayer =
                ArenaPlayer.Selected(
                    userId = playerState.userId,
                    displayName = playerState.displayName,
                    characterClass = characterClassState.toDomain(),
                )

            chooseCharacterClassUseCase(
                arenaId = arguments.arenaId,
                selectedArenaPlayer = selectedArenaPlayer,
                allPlayers =
                    arenaState.otherPlayers.values.map { it.toDomain() } + selectedArenaPlayer,
            )

            val recordedStat =
                sessionStats.onClassSelected(classType = selectedArenaPlayer.characterClass.type)
            if (recordedStat) persistPendingStats()

            _uiStateFlow.updateLoaded { screenState ->
                val nextArenaState =
                    if (screenState.arenaState is ArenaState.ChoosingClasses) {
                        screenState.arenaState.copy(selectedClass = null)
                    } else {
                        screenState.arenaState
                    }
                screenState.copy(arenaState = nextArenaState)
            }
        }
    }

    private fun finishMatchAfterPlayback() {
        if (finishWriteJob?.isActive == true) return
        finishWriteJob = viewModelScope.launch { finishMatchUseCase(arenaId = arguments.arenaId) }
    }

    fun onDiceSettled() {
        viewModelScope.launch { messageQueue.onDiceSettled() }
    }

    fun onAnimationComplete() {
        messageQueue.onAnimationComplete()
    }

    fun onAttack(attackArguments: AttackArguments) {
        viewModelScope.launch { attackArguments.onAttack() }
    }

    private suspend fun AttackArguments.onAttack() {
        val arenaState = currentCombatState() ?: return
        val normalAttack = toDomain(combatActionState = arenaState.combatActionState) ?: return

        arenaState.performCombatAction(currentAction = normalAttack)
    }

    fun onUseAbility(useAbilityArguments: UseAbilityArguments) {
        viewModelScope.launch { useAbilityArguments.onUseAbility() }
    }

    private suspend fun UseAbilityArguments.onUseAbility() {
        val arenaState = currentCombatState() ?: return
        val resolvedArguments = withImpersonatedTarget(arenaState = arenaState)
        val currentAction =
            resolvedArguments.toDomain(combatActionState = arenaState.combatActionState) ?: return

        arenaState.performCombatAction(currentAction = currentAction)
    }

    /**
     * Impersonate copies the offensive ability of the opponent the Illusionist targeted, so the
     * copied ability is resolved here from the live [arenaState] rather than baked into the control
     * (which is built before a target is chosen). Every other ability is returned unchanged.
     */
    private fun UseAbilityArguments.withImpersonatedTarget(
        arenaState: ArenaState.Combat
    ): UseAbilityArguments {
        if (abilityActionState !is AbilityActionState.Activated.Impersonate) return this

        val target = arenaState.playerMap[targetedOpponentUserId] ?: return this
        val copiedAbility = target.characterClass.offensiveAbility.toDomain()

        return copy(
            abilityActionState =
                AbilityActionState.Activated.Impersonate(
                    impersonateAbility =
                        OffensiveAbility.Impersonate(impersonatedAbility = copiedAbility)
                )
        )
    }

    private fun currentCombatState(): ArenaState.Combat? =
        _uiStateFlow.getLoadedStateOrNull()?.arenaState as? ArenaState.Combat

    private suspend fun ArenaState.Combat.performCombatAction(currentAction: CurrentCombatAction) {
        // TODO: CombatActionArguments needs a refactor - likely currentAction should be part of it
        val combatActionArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = playerMap.mapValues { it.value.toDomain() },
                roundNumber = roundNumber,
                doublesBonus = diceRollState?.doublesBonus?.toDomain(),
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
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

    private suspend fun flushSessionStats() {
        val delta = sessionStats.consumeDelta()
        val wasWritten = updateProfileStatsUseCase(delta = delta)
        if (wasWritten || delta.isEmpty) {
            pendingStatsStore.clear()
        } else {
            pendingStatsStore.save(delta)
        }
    }

    /**
     * Mirrors the in-memory session delta to durable storage as it grows, so a crash or kill
     * mid-match loses at most the current in-progress match rather than the whole session (SS-076,
     * shrinking the SS-075 loss window). Awaited inline with the state stream so it cannot race the
     * clear in [flushSessionStats], which only runs after that stream is cancelled.
     */
    private suspend fun persistPendingStats() {
        pendingStatsStore.save(delta = sessionStats.peekDelta())
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

    private suspend fun ArenaMatch?.updateScreenState(userId: String) {
        if (this == null) {
            screenStateFlowJob?.cancel()
            finishWriteJob?.cancel()
            // playbackSequencer.cancel()

            viewModelScope.launch {
                setCurrentTravelerAvailableUseCase()
                flushSessionStats()
                _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
            }
            return
        }

        val recordedStat = sessionStats.onMatchUpdate(match = this, userId = userId)
        if (recordedStat) persistPendingStats()
        notifyDepartedOpponents(userId = userId)

        val messageUpdate = collectNewMessages(match = this)
        if (messageUpdate.isNewMatch) clearMessages()

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

        val nextState = updatedArenaStateUpdate.nextState
        /*playbackSequencer.submit(
            target = nextState,
            freshMessages = updatedArenaStateUpdate.freshMessages,
        )*/

        computerOpponentHandler.onArenaState(
            arenaState = nextState.arenaState,
            characterClasses = nextState.characterClasses,
        )
    }

    private fun addMessage(message: ArenaMessageState) {
        _uiStateFlow.updateLoaded { screenState ->
            val isAlreadyShown = screenState.messages.any { it.id == message.id }
            if (isAlreadyShown) {
                screenState
            } else {
                screenState.copy(
                    messages = screenState.messages + message,
                    messageGroups = screenState.messageGroups.plusMessage(message = message),
                )
            }
        }
    }

    private fun clearMessages() {
        _uiStateFlow.updateLoaded { screenState ->
            screenState.copy(messages = emptyList(), messageGroups = emptyList())
        }
    }

    private fun collectNewMessages(match: ArenaMatch): MessageUpdate {
        val previousMatchNumber = lastMatchNumber
        val isNewMatch =
            previousMatchNumber != null && match.matchNumberInGauntlet != previousMatchNumber
        lastMatchNumber = match.matchNumberInGauntlet

        if (isNewMatch) seenMessageIds.clear()

        val newMessages = match.messages.filter { message -> seenMessageIds.add(message.id) }
        return MessageUpdate(newMessages = newMessages, isNewMatch = isNewMatch)
    }

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

        when (val arenaState = loadedState.arenaState) {
            is ArenaState.WaitingForAllPlayers -> leaveTimedOutMatch()

            is ArenaState.ChoosingClasses ->
                autoChooseCharacterClassForUser(
                    arenaState = arenaState,
                    characterClasses = loadedState.characterClasses,
                )

            is ArenaState.Combat -> autoResolveCombatForUser(arenaState = arenaState)

            is ArenaState.Finished ->
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

    private fun autoResolveCombatForUser(arenaState: ArenaState.Combat) {
        val userPlayerId = arenaState.userPlayerId
        if (userPlayerId !in arenaState.combatActionState.activeUserIds) return

        when (val actionState = arenaState.combatActionState) {
            is CombatActionState.Idle ->
                autoAttackRandomOpponent(arenaState = arenaState, attackerId = userPlayerId)

            is CombatActionState.NormalAttack ->
                onAttack(
                    attackArguments =
                        AttackArguments(
                            targetedOpponentUserId = actionState.defenderUserId,
                            userPlayerId = userPlayerId,
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
                        )
                )
        }
    }

    private fun autoAttackRandomOpponent(arenaState: ArenaState.Combat, attackerId: String) {
        val target =
            arenaState.playerMap.values
                .filter { player ->
                    player.userId != attackerId && player.characterClass.hitPoints > 0
                }
                .randomOrNull() ?: return

        onAttack(
            attackArguments =
                AttackArguments(targetedOpponentUserId = target.userId, userPlayerId = attackerId)
        )
    }

    private fun autoChooseCharacterClassForUser(
        arenaState: ArenaState.ChoosingClasses,
        characterClasses: List<CharacterClassState>,
    ) {
        if (!arenaState.isItThisUsersTurn) return

        val availableClasses =
            characterClasses.filterNot { it.type in arenaState.unavailableClasses }
        val chosenClass = arenaState.selectedClass ?: availableClasses.randomOrNull() ?: return
        val userPlayer = arenaState.playerMap.getValue(key = arenaState.userPlayerId)

        onChooseCharacterClass(playerState = userPlayer, characterClassState = chosenClass)
    }
}

private data class MessageUpdate(
    val newMessages: List<IncomingArenaMessage>,
    val isNewMatch: Boolean,
)
