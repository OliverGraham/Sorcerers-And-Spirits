package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.computer.ComputerOpponentHandler
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toUpdatedState
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
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val updateProfileStatsUseCase: UpdateProfileStatsUseCase,
) : ViewModel() {

    private val sessionStats = ArenaSessionStats()

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }

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

    init {
        loadArenaMatch()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadArenaMatch()
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

            sessionStats.onClassSelected(classType = selectedArenaPlayer.characterClass.type)

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

    fun onIncomingMessageHandled() {
        _uiStateFlow.updateLoaded { screenState ->
            if (screenState.unhandledMessagesCount <= 0) return@updateLoaded screenState

            screenState.copy(unhandledMessagesCount = screenState.unhandledMessagesCount - 1)
        }
    }

    fun onAttack(attackArguments: AttackArguments) {
        viewModelScope.launch { attackArguments.onAttack() }
    }

    private suspend fun AttackArguments.onAttack() {
        val arenaState = currentCombatState() ?: return
        val normalAttack = toDomain() ?: return

        arenaState.performCombatAction(currentAction = normalAttack)
    }

    fun onUseAbility(useAbilityArguments: UseAbilityArguments) {
        viewModelScope.launch { useAbilityArguments.onUseAbility() }
    }

    private suspend fun UseAbilityArguments.onUseAbility() {
        val arenaState = currentCombatState() ?: return
        val currentAction = toDomain(combatActionState = arenaState.combatActionState)

        arenaState.performCombatAction(currentAction = currentAction)
    }

    private fun currentCombatState(): ArenaState.Combat? =
        _uiStateFlow.getLoadedStateOrNull()?.arenaState as? ArenaState.Combat

    private suspend fun ArenaState.Combat.performCombatAction(currentAction: CurrentCombatAction) {
        val combatActionArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = playerMap.mapValues { it.value.toDomain() },
                roundNumber = roundNumber,
                doublesBonus = doublesBonus,
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
    }

    fun onTryLeaveArena() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = true) }
    }

    fun onConfirmLeaveArena() {
        screenStateFlowJob?.cancel()
        computerOpponentHandler.cancel()

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
        updateProfileStatsUseCase(delta = sessionStats.consumeDelta())
    }

    fun onDismissForfeitDialog() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
    }

    fun onStayForAnotherMatch() {
        viewModelScope.launch {
            stayForAnotherMatchUseCase(arenaId = arguments.arenaId)
            computerOpponentHandler.readyUpForNextMatch()

            _uiStateFlow.updateLoaded { screenState ->
                screenState.copy(hasMadeKnockoutDecision = true)
            }
        }
    }

    private fun ArenaMatch?.updateScreenState(userId: String) {
        if (this == null) {
            screenStateFlowJob?.cancel()

            viewModelScope.launch {
                setCurrentTravelerAvailableUseCase()
                flushSessionStats()
                _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
            }
            return
        }

        sessionStats.onMatchUpdate(match = this, userId = userId)

        val updatedArenaStateUpdate =
            toUpdatedState(
                previousState = _uiStateFlow.getLoadedStateOrNull(),
                userId = userId,
                characterClasses = characterClasses,
            )

        handleTimerAction(timerAction = updatedArenaStateUpdate.timerAction)

        val nextState = updatedArenaStateUpdate.nextState
        _uiStateFlow.value = UiState.Loaded(data = nextState)

        computerOpponentHandler.onArenaState(
            arenaState = nextState.arenaState,
            characterClasses = nextState.characterClasses,
        )
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

            // The match is over; the user leaves or stays through the match-finished decision
            // dialog, and the timer is stopped in Finished, so there is nothing to resolve here.
            is ArenaState.Finished -> Unit
        }
    }

    private suspend fun leaveTimedOutMatch() {
        screenStateFlowJob?.cancel()
        computerOpponentHandler.cancel()

        exitArenaUseCase(arenaId = arguments.arenaId)
        setCurrentTravelerAvailableUseCase()
        flushSessionStats()
        _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
    }

    /**
     * Auto-resolves the action the user owes when their turn timer expires: a random attack on
     * their own turn, or the owed attack/ability roll. (The computer opponents are driven
     * separately by [ComputerOpponentHandler].)
     */
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
                            combatActionState = actionState,
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
                AttackArguments(
                    combatActionState = arenaState.combatActionState,
                    targetedOpponentUserId = target.userId,
                    userPlayerId = attackerId,
                )
        )
    }

    private fun autoChooseCharacterClassForUser(
        arenaState: ArenaState.ChoosingClasses,
        characterClasses: List<CharacterClassState>,
    ) {
        if (!arenaState.isItThisUsersTurn) return

        val availableClasses = characterClasses.filterNot { it in arenaState.unavailableClasses }
        val chosenClass = arenaState.selectedClass ?: availableClasses.randomOrNull() ?: return
        val userPlayer = arenaState.playerMap.getValue(key = arenaState.userPlayerId)

        onChooseCharacterClass(playerState = userPlayer, characterClassState = chosenClass)
    }
}
