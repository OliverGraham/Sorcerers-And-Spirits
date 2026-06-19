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
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.OffensiveAbilityState
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
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

private const val COMPUTER_MIN_THINKING_DELAY_MILLIS = 1600L
private const val COMPUTER_MAX_THINKING_DELAY_MILLIS = 3000L

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

    private val computerOpponentIds: Set<String> =
        arguments.computerOpponents.map { it.userId }.toSet()

    private var computerActionJob: Job? = null
    private var scheduledComputerActionSignature: String? = null

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

            if (playerState.userId == arenaState.userPlayerId) {
                sessionStats.onClassSelected(classType = selectedArenaPlayer.characterClass.type)
            }

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

            // The computer has no client to ready itself, so the user's client readies it up too,
            // otherwise the match would hang waiting on a never-ready bot.
            computerOpponentIds.forEach { computerId ->
                stayForAnotherMatchUseCase(arenaId = arguments.arenaId, playerUserId = computerId)
            }

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

        driveComputerIfNeeded(arenaState = nextState.arenaState)
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

        exitArenaUseCase(arenaId = arguments.arenaId)
        setCurrentTravelerAvailableUseCase()
        flushSessionStats()
        _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
    }

    private fun autoResolveCombatForUser(arenaState: ArenaState.Combat) =
        autoResolveCombatForPlayer(
            arenaState = arenaState,
            playerId = arenaState.userPlayerId,
            onOwnTurn = ::autoAttackRandomOpponent,
        )

    private fun resolveComputerCombat(arenaState: ArenaState.Combat, computerId: String) =
        autoResolveCombatForPlayer(
            arenaState = arenaState,
            playerId = computerId,
            onOwnTurn = ::chooseComputerTurnAction,
        )

    /**
     * Resolves the combat action [playerId] currently owes. Owed reaction/continuation rolls are
     * forced and resolved identically for any player; only the free choice at the start of the
     * player's own turn ([CombatActionState.Idle]) varies, supplied as [onOwnTurn] — a random
     * attack for the user's timed-out turn, or the computer's lightly-smart choice.
     */
    private fun autoResolveCombatForPlayer(
        arenaState: ArenaState.Combat,
        playerId: String,
        onOwnTurn: (ArenaState.Combat, String) -> Unit,
    ) {
        // The player owes an action only when their id is among the active ids (it is their turn,
        // or
        // a roll they own); otherwise there is nothing to auto-resolve.
        if (playerId !in arenaState.combatActionState.activeUserIds) return

        when (val actionState = arenaState.combatActionState) {
            is CombatActionState.Idle -> onOwnTurn(arenaState, playerId)

            is CombatActionState.NormalAttack ->
                onAttack(
                    attackArguments =
                        AttackArguments(
                            combatActionState = actionState,
                            targetedOpponentUserId = actionState.defenderUserId,
                            userPlayerId = playerId,
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
                            userPlayerId = playerId,
                        )
                )
        }
    }

    /** The user's timed-out turn attacks a random living opponent. */
    private fun autoAttackRandomOpponent(arenaState: ArenaState.Combat, attackerId: String) {
        val target =
            livingOpponents(arenaState = arenaState, excludingPlayerId = attackerId).randomOrNull()
                ?: return

        attackOpponent(
            arenaState = arenaState,
            attackerId = attackerId,
            targetUserId = target.userId,
        )
    }

    /**
     * The computer's turn: activate an offensive ability when it is clearly favorable, otherwise
     * attack the weakest (lowest-HP) living opponent to secure kills.
     */
    private fun chooseComputerTurnAction(arenaState: ArenaState.Combat, computerId: String) {
        val computerPlayer = arenaState.playerMap.getValue(key = computerId)
        val favorableAbility = computerPlayer.characterClass.favorableOffensiveAbility()

        if (favorableAbility != null) {
            onUseAbility(
                useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = favorableAbility,
                        targetedOpponentUserId = "",
                        userPlayerId = computerId,
                    )
            )
            return
        }

        val target =
            weakestLivingOpponent(arenaState = arenaState, excludingPlayerId = computerId) ?: return

        attackOpponent(
            arenaState = arenaState,
            attackerId = computerId,
            targetUserId = target.userId,
        )
    }

    private fun attackOpponent(
        arenaState: ArenaState.Combat,
        attackerId: String,
        targetUserId: String,
    ) {
        onAttack(
            attackArguments =
                AttackArguments(
                    combatActionState = arenaState.combatActionState,
                    targetedOpponentUserId = targetUserId,
                    userPlayerId = attackerId,
                )
        )
    }

    /**
     * The clearly-beneficial offensive ability for the computer to activate this turn instead of
     * attacking, or null to attack. Only the Necromancer's Summon Minion qualifies, and only while
     * it still has something to gain — it can build minion shields and heal toward full with no
     * real downside beyond a fizzle. The riskier gambits (All-In's self-knockout, Prepare Attack
     * forgoing a hit, Impersonate) are intentionally left to a normal attack.
     */
    private fun CharacterClassState.favorableOffensiveAbility(): AbilityActionState.Activated? =
        when (val ability = offensiveAbility) {
            is OffensiveAbilityState.SummonMinion -> {
                val hasNoMinions = ability.skeletonCount == 0 && ability.golemCount == 0
                val isBelowMaxHp = hitPoints < maxHitPoints

                if (hasNoMinions || isBelowMaxHp) {
                    AbilityActionState.Activated.SummonMinion(
                        summonMinionAbility =
                            OffensiveAbility.SummonMinion(
                                skeletonCount = ability.skeletonCount,
                                golemCount = ability.golemCount,
                            )
                    )
                } else {
                    null
                }
            }

            else -> null
        }

    private fun livingOpponents(
        arenaState: ArenaState.Combat,
        excludingPlayerId: String,
    ): List<PlayerState.Selected> =
        arenaState.playerMap.values.filter { player ->
            player.userId != excludingPlayerId && player.characterClass.hitPoints > 0
        }

    /**
     * The lowest-HP living opponent, ties broken at random so the computer is not fully
     * predictable.
     */
    private fun weakestLivingOpponent(
        arenaState: ArenaState.Combat,
        excludingPlayerId: String,
    ): PlayerState.Selected? {
        val opponents =
            livingOpponents(arenaState = arenaState, excludingPlayerId = excludingPlayerId)
        val lowestHitPoints = opponents.minOfOrNull { it.characterClass.hitPoints } ?: return null

        return opponents
            .filter { player -> player.characterClass.hitPoints == lowestHitPoints }
            .randomOrNull()
    }

    private fun autoChooseCharacterClassForUser(
        arenaState: ArenaState.ChoosingClasses,
        characterClasses: List<CharacterClassState>,
    ) =
        autoChooseClassForPlayer(
            arenaState = arenaState,
            characterClasses = characterClasses,
            playerId = arenaState.userPlayerId,
            preferredClass = arenaState.selectedClass,
        )

    private fun autoChooseClassForPlayer(
        arenaState: ArenaState.ChoosingClasses,
        characterClasses: List<CharacterClassState>,
        playerId: String,
        preferredClass: CharacterClassState?,
    ) {
        if (arenaState.activePlayerUserId != playerId) return

        val availableClasses = characterClasses.filterNot { it in arenaState.unavailableClasses }
        val chosenClass = preferredClass ?: availableClasses.randomOrNull() ?: return
        val player = arenaState.playerMap.getValue(key = playerId)

        onChooseCharacterClass(playerState = player, characterClassState = chosenClass)
    }

    /**
     * After each state update, drives any computer opponent that currently owes an action. The
     * action is scheduled behind a short randomized "thinking" delay and keyed on an action
     * signature so unrelated emissions (such as incoming messages) neither re-trigger nor
     * double-fire the same owed action.
     */
    private fun driveComputerIfNeeded(arenaState: ArenaState) {
        if (computerOpponentIds.isEmpty()) return

        val pendingComputerId = arenaState.pendingComputerActorId()
        if (pendingComputerId == null) {
            scheduledComputerActionSignature = null
            computerActionJob?.cancel()
            return
        }

        val signature = arenaState.computerActionSignature(actorId = pendingComputerId)
        if (signature == scheduledComputerActionSignature) return

        scheduledComputerActionSignature = signature
        computerActionJob?.cancel()
        computerActionJob =
            viewModelScope.launch {
                delay(timeMillis = computerThinkingDelayMillis())
                performComputerAction(actorId = pendingComputerId)
            }
    }

    private fun performComputerAction(actorId: String) {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        // The match may have advanced during the thinking delay; only act if this computer still
        // owes the action.
        if (loadedState.arenaState.pendingComputerActorId() != actorId) return

        when (val arenaState = loadedState.arenaState) {
            is ArenaState.ChoosingClasses ->
                autoChooseClassForPlayer(
                    arenaState = arenaState,
                    characterClasses = loadedState.characterClasses,
                    playerId = actorId,
                    preferredClass = null,
                )

            is ArenaState.Combat ->
                resolveComputerCombat(arenaState = arenaState, computerId = actorId)

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> Unit
        }
    }

    /**
     * The computer opponent that owes the current action, or null if none does (or it is the user).
     */
    private fun ArenaState.pendingComputerActorId(): String? =
        when (this) {
            is ArenaState.ChoosingClasses -> activePlayerUserId.takeIf { it in computerOpponentIds }

            is ArenaState.Combat ->
                if (userPlayerId in combatActionState.activeUserIds) {
                    null
                } else {
                    combatActionState.activeUserIds.firstOrNull { it in computerOpponentIds }
                }

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> null
        }

    private fun ArenaState.computerActionSignature(actorId: String): String =
        when (this) {
            is ArenaState.ChoosingClasses -> "choosing:$actorId"

            is ArenaState.Combat -> "combat:$roundNumber:$actorId:$combatActionState"

            is ArenaState.WaitingForAllPlayers,
            is ArenaState.Finished -> "none"
        }

    private fun computerThinkingDelayMillis(): Long =
        Random.nextLong(
            from = COMPUTER_MIN_THINKING_DELAY_MILLIS,
            until = COMPUTER_MAX_THINKING_DELAY_MILLIS,
        )
}
