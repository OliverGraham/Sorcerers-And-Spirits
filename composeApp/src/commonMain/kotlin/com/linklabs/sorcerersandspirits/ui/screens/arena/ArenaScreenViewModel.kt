package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.RemoveArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

// TODO(SS-017): This is all fucked

@KoinViewModel
class ArenaScreenViewModel(
    @InjectedParam private val arguments: ArenaScreenViewModelArguments,
    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase,
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase,
    private val exitArenaUseCase: ExitArenaUseCase,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val performCombatActionUseCase: PerformCombatActionUseCase,
    private val removeArenaMatchUseCase: RemoveArenaMatchUseCase,
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }

    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onTimerExpiration,
        )

    private var screenStateFlowJob: Job? = null

    init {
        screenStateFlowJob =
            viewModelScope.launch {
                // TODO(SS-018): Figure out better way for userId
                val userId = getUserIdUseCase().orEmpty()

                coordinateArenaMatchUseCase(
                        arenaId = arguments.arenaId,
                        expectedNumberOfPlayers = arguments.expectedNumberOfPlayers,
                    )
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
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return
        val arenaState = loadedState.arenaState as? ArenaState.Combat ?: return
        val normalAttack = toDomain() ?: return

        val combatActionArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = arenaState.playerMap.mapValues { it.value.toDomain() },
                roundNumber = arenaState.roundNumber,
                doublesBonus = null, // TODO(SS-019): track doublesBonus in ArenaState.Combat
            )

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = normalAttack)
    }

    fun onUseAbility(useAbilityArguments: UseAbilityArguments) {
        viewModelScope.launch { useAbilityArguments.onUseAbility() }
    }

    private suspend fun UseAbilityArguments.onUseAbility() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return
        val arenaState = loadedState.arenaState as? ArenaState.Combat ?: return

        // TODO(SS-020): This Charm logic is done oddly or incorrectly
        val resolvedAbilityActionState =
            if (
                abilityActionState is AbilityActionState.Passive.Persuade.Charm &&
                    targetedOpponentUserId.isNotEmpty()
            ) {
                abilityActionState.copy(selectedCharmTargetUserId = targetedOpponentUserId)
            } else {
                abilityActionState
            }

        val combatActionArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = arenaState.playerMap.mapValues { it.value.toDomain() },
                roundNumber = arenaState.roundNumber,
                doublesBonus = null, // TODO(SS-019): track doublesBonus in ArenaState.Combat
            )

        val currentAction =
            when (val actionState = arenaState.combatActionState) {
                // Responding to an ability roll in progress: the combatant ids come from the
                // ongoing action
                is CombatActionState.AbilityRoll ->
                    CurrentCombatAction.AbilityRoll(
                        purpose = resolvedAbilityActionState.toDomain(),
                        attackerUserId = actionState.attackerUserId,
                        defenderUserId = actionState.defenderUserId,
                        originalTurnUserId = actionState.originalTurnUserId,
                    )

                // Activated ability used on the user's own turn against their selected target
                // (or themselves when no target is required)
                else ->
                    CurrentCombatAction.AbilityRoll(
                        purpose = resolvedAbilityActionState.toDomain(),
                        attackerUserId = userPlayerId,
                        defenderUserId = targetedOpponentUserId.ifEmpty { userPlayerId },
                        originalTurnUserId = userPlayerId,
                    )
            }

        performCombatActionUseCase(arguments = combatActionArguments, currentAction = currentAction)
    }

    fun onTryLeaveArena() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = true) }
    }

    // TODO(SS-021) Arena: Need a leave that updates stats with a forfeit, and one that triggers
    // when game
    //  is finished. Update stats only when user leaves (could be after multiple matches)

    fun onConfirmLeaveArena() {
        screenStateFlowJob?.cancel()

        viewModelScope.launch {
            exitArenaUseCase(arenaId = arguments.arenaId)
            setCurrentTravelerAvailableUseCase()
            // TODO(SS-022): If is last person leaving, then removeArenaMatchUseCase(arenaId =
            // arguments.arenaId)
            _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
            _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
        }
    }

    fun onDismissForfeitDialog() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
    }

    fun onStayForAnotherMatch() {
        viewModelScope.launch {
            stayForAnotherMatchUseCase(arenaId = arguments.arenaId)

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
                _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
            }
            return
        }

        val updatedArenaStateUpdate =
            toUpdatedState(
                previousState = _uiStateFlow.getLoadedStateOrNull(),
                userId = userId,
                characterClasses = characterClasses,
            )

        handleTimerAction(timerAction = updatedArenaStateUpdate.timerAction)

        val nextState = updatedArenaStateUpdate.nextState
        _uiStateFlow.value = UiState.Loaded(data = nextState)
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

        when (loadedState.arenaState) {
            is ArenaState.WaitingForAllPlayers -> {
                _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
            }

            is ArenaState.ChoosingClasses -> {
                // TODO(SS-014)
            }

            is ArenaState.Combat -> {
                // TODO(SS-014)
            }

            is ArenaState.Finished -> {
                // TODO(SS-014)
            }
        }
    }
}
