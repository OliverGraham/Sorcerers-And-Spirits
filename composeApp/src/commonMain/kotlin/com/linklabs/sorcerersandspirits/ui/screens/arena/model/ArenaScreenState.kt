package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState

private const val TIMER_VISIBLE_THRESHOLD_SECONDS = 15
private const val TIMER_URGENT_THRESHOLD_SECONDS = 10
private const val WAITING_TIMER_DURATION_SECONDS = 30
private const val CHOOSING_CLASSES_TIMER_DURATION_SECONDS = 30
private const val COMBAT_TIMER_DURATION_SECONDS = 45
private const val MATCH_FINISHED_TIMER_DURATION_SECONDS = 30
private const val GAUNTLET_FINISHED_TIMER_DURATION_SECONDS = 60
private const val MINIMUM_PLAYERS_FOR_NEXT_MATCH = 2

// TODO: Can this massive data class be reduced in some way?
@Immutable
data class ArenaScreenState(
    val messages: List<ArenaMessageState>,
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val isUserPlayerKnockedOut: Boolean,
    val hasReadiedForNextMatch: Boolean,
    val arenaState: ArenaState,
    val participants: Map<String, ParticipantClass>,
    val numberOfNewMessages: Int,
    val gauntletProgress: GauntletProgress = GauntletProgress(),
    val characterClasses: List<CharacterClassState> = emptyList(),
    val classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
    val pendingFinishedState: ArenaState.Finished? = null,
) {
    val isTimerVisible: Boolean = secondsRemaining in 1..TIMER_VISIBLE_THRESHOLD_SECONDS

    val isTimerUrgent: Boolean = secondsRemaining in 1..TIMER_URGENT_THRESHOLD_SECONDS
    val isFinishPending: Boolean = pendingFinishedState != null

    internal val latestRoll: LatestRoll = messages.latestRoll()
    val flashMessages: List<ArenaMessageState> = messages.filterNot { it.flashCallout == null }
    val diceBadge: DiceBadge? = deriveDiceBadge()

    private fun deriveDiceBadge(): DiceBadge? {
        val combatArenaState = arenaState as? ArenaState.Combat ?: return null

        return when {
            combatArenaState.isDoublesBonusLive -> DiceBadge.DoublesBonus
            combatArenaState.isMirrorImagePenaltyActive -> DiceBadge.MirrorPenalty
            else -> null
        }
    }
}

@Immutable
sealed interface ArenaState {

    @Immutable data object WaitingForAllPlayers : ArenaState

    @Immutable
    data class ChoosingClasses(
        override val playerMap: Map<UserId, PlayerState>,
        override val userPlayerId: String,
        val activePlayerUserId: String,
        val selectedClass: CharacterClassState?,
        val unavailableClasses: Set<CharacterClassType>,
        val classSelections: Map<CharacterClassType, String>,
    ) : ActiveState {
        val otherPlayers = playerMap.filterKeys { it != activePlayerUserId }
        val isItThisUsersTurn = activePlayerUserId == userPlayerId
    }

    @Immutable
    data class Combat(
        override val playerMap: Map<UserId, PlayerState.Selected>,
        override val userPlayerId: String,
        val combatActionState: CombatActionState,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val doublesBonus: DoublesBonus?,
        val isMirrorImagePenaltyActive: Boolean,
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val isDoublesBonusLive = doublesBonus != null && doublesBonus.userId in activeUserIds
        val isItThisUsersTurn = userPlayerId in activeUserIds

        // TODO: Obviously, this was supposed to simply come from the domain
        val turnOwnerUserId = combatActionState.getTurnOwnerUserId()

        // TODO: Obviously, this was supposed to simply come from the domain
        val attackingUserId = combatActionState.getAttackingUserId()

        // TODO: Obviously, this was supposed to simply come from the domain
        val combatControls = deriveCombatControls()

        // TODO: Obviously, this was supposed to simply come from the domain
        val lockedTargetUserId = deriveLockedTargetUserId()
    }

    @Immutable
    data class Finished(
        val players: List<PlayerState.Selected>,
        val hasAnyPlayerReadied: Boolean = false,
    ) : ArenaState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
        val canStartNextMatch = players.size >= MINIMUM_PLAYERS_FOR_NEXT_MATCH
    }
}

private fun ArenaState.Combat.deriveLockedTargetUserId(): String? {
    val inFlightAttack = combatActionState as? CombatActionState.NormalAttack ?: return null
    val isUsersOwnAttack = inFlightAttack.attackerUserId == userPlayerId

    return if (isUsersOwnAttack) inFlightAttack.defenderUserId else null
}

@Immutable
sealed interface ActiveState : ArenaState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

// TODO(SS-031): Need to refactor this badly

internal fun ArenaMatch.toUpdatedState(
    previousState: ArenaScreenState?,
    userId: String,
    characterClasses: List<CharacterClassState> = emptyList(),
    classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
): ArenaStateUpdate {
    val userPlayer = playerMap.getValue(key = userId)

    // ArenaMessages refer to players by class name (SS-097), so the resolution map is keyed on the
    // class each player selected. Players who have left the arena are retained so their earlier
    // messages still resolve.
    val currentParticipants =
        playerMap.values.filterIsInstance<ArenaPlayer.Selected>().associate {
            it.userId to
                ParticipantClass(
                    className = it.characterClass.className,
                    type = it.characterClass.type,
                )
        }
    val participants = (previousState?.participants ?: emptyMap()) + currentParticipants

    val messages = messages.toStates(participants = participants)
    val previousSeconds = previousState?.secondsRemaining ?: 0
    val previousShowForfeitDialog = previousState?.showForfeitDialog ?: false

    val isUserPlayerKnockedOut =
        (userPlayer as? ArenaPlayer.Selected)?.characterClass?.isConscious()?.not() ?: false

    // Readiness comes from the shared match state, the same flag the data layer uses to start the
    // next match, rather than being tracked locally per client. This keeps every player's Finished
    // screen in step with that decision and resets automatically when a new match begins.
    val hasReadiedForNextMatch = userPlayer.isReadyForNextMatch

    val computedArenaState =
        when (this) {
            is ArenaMatch.WaitingForAllPlayers -> ArenaState.WaitingForAllPlayers

            is ArenaMatch.ChoosingClasses -> {
                val (unavailable, selections) = playerMap.prepareClassSelectionInfo(userId = userId)
                val previousSelection =
                    (previousState?.arenaState as? ArenaState.ChoosingClasses)?.selectedClass

                ArenaState.ChoosingClasses(
                    playerMap = playerMap.mapValues { it.value.toState() },
                    activePlayerUserId = activePlayer.userId,
                    userPlayerId = userId,
                    unavailableClasses = unavailable,
                    classSelections = selections,
                    selectedClass =
                        characterClasses.resolveSelectedClass(
                            previousSelection = previousSelection,
                            unavailableClasses = unavailable,
                        ),
                )
            }

            is ArenaMatch.Combat -> {
                ArenaState.Combat(
                    playerMap = playerMap.mapValues { it.value.toSelectedState() },
                    userPlayerId = userId,
                    combatActionState = nextCombatAction.toCombatActionState(),
                    roundNumber = roundNumber,
                    isNewRound = isNewRound,
                    doublesBonus = doublesBonus,
                    isMirrorImagePenaltyActive = isMirrorImagePenaltyActive,
                )
            }

            is ArenaMatch.Finished ->
                ArenaState.Finished(
                    players =
                        playerMap.values
                            .map { it.toState() }
                            .filterIsInstance<PlayerState.Selected>(),
                    hasAnyPlayerReadied = playerMap.values.any { it.isReadyForNextMatch },
                )
        }

    val (displayedArenaState, pendingFinishedState) =
        holdCombatForClosingFlash(
            computedState = computedArenaState,
            previousState = previousState?.arenaState,
        )

    val gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet)

    val nextScreenState =
        ArenaScreenState(
            messages = messages,
            secondsRemaining = previousSeconds,
            showForfeitDialog = previousShowForfeitDialog,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = displayedArenaState,
            participants = participants,
            gauntletProgress = gauntletProgress,
            characterClasses = characterClasses,
            classCopy = classCopy,
            pendingFinishedState = pendingFinishedState,
            numberOfNewMessages = numberOfNewMessages,
        )

    // While the Finished transition is held, the match is over, so the combat clock is stopped
    // rather than left running against the held combat view.
    val timerAction =
        if (pendingFinishedState != null) {
            TimerAction.Stop
        } else {
            determineTimerAction(
                previous = previousState?.arenaState,
                next = displayedArenaState,
                isFinalMatch = gauntletProgress.isFinalMatch,
            )
        }

    val finalState =
        when (timerAction) {
            is TimerAction.Start ->
                nextScreenState.copy(secondsRemaining = timerAction.timerDuration)

            TimerAction.Stop -> nextScreenState.copy(secondsRemaining = 0)

            TimerAction.None -> nextScreenState
        }

    return ArenaStateUpdate(nextState = finalState, timerAction = timerAction)
}

/**
 * Briefly holds the combat view on the Combat→Finished transition so the closing flash callout
 * (SS-148) can play before the Finished screen replaces it (SS-154). When the match has just
 * finished, the combat view is held in place—updated to show the final knockout—and the finished
 * state is returned as pending, for the ViewModel to advance to after a fixed hold. Otherwise the
 * computed state is shown as is.
 */
private fun holdCombatForClosingFlash(
    computedState: ArenaState,
    previousState: ArenaState?,
): Pair<ArenaState, ArenaState.Finished?> {
    if (computedState !is ArenaState.Finished) return computedState to null
    if (previousState !is ArenaState.Combat) return computedState to null

    val heldCombatState = previousState.withFinalPlayers(finishedPlayers = computedState.players)
    return heldCombatState to computedState
}

/**
 * Returns this combat view with each player replaced by their final, match-end state, so the held
 * view reflects the closing knockout while the Finished transition is held (SS-154).
 */
private fun ArenaState.Combat.withFinalPlayers(
    finishedPlayers: List<PlayerState.Selected>
): ArenaState.Combat {
    val finalPlayersById = finishedPlayers.associateBy { it.userId }
    val updatedPlayerMap =
        playerMap.mapValues { (userId, player) -> finalPlayersById[userId] ?: player }
    return copy(playerMap = updatedPlayerMap)
}

/**
 * The class to pre-select when the Choosing Classes screen is first entered, so the confirm button
 * is enabled immediately (SS-176). Rogue is the default; if it is already taken by another player,
 * the first still-available class is used instead.
 */
private fun List<CharacterClassState>.defaultSelectedClass(
    unavailableClasses: Set<CharacterClassType>
): CharacterClassState? {
    val rogue = firstOrNull { it.type == CharacterClassType.Rogue }
    val isRogueAvailable = rogue != null && rogue.type !in unavailableClasses
    return if (isRogueAvailable) rogue else firstOrNull { it.type !in unavailableClasses }
}

/**
 * The class to keep selected as the shared match state updates: the user's [previousSelection] when
 * it is still available, otherwise a fresh [defaultSelectedClass]. Dropping a previous selection
 * that another player has since taken is what stops a stale local pick — everyone defaults to Rogue
 * (SS-176) — from being confirmed into a duplicate class (SS-187).
 */
internal fun List<CharacterClassState>.resolveSelectedClass(
    previousSelection: CharacterClassState?,
    unavailableClasses: Set<CharacterClassType>,
): CharacterClassState? =
    previousSelection?.takeIf { it.type !in unavailableClasses }
        ?: defaultSelectedClass(unavailableClasses = unavailableClasses)

private fun Map<String, ArenaPlayer>.prepareClassSelectionInfo(
    userId: String
): Pair<Set<CharacterClassType>, Map<CharacterClassType, String>> {
    val allPlayers = values.map { player -> player.toState() }
    // Keyed on class type, not the full CharacterClassState: the no-duplicate-class rule and the
    // "chosen by" labels are about class identity, and a player's selected state can differ from
    // the master template by mutable fields (HP, ability counters), so value equality would let a
    // duplicate slip through (SS-122).
    val unavailableClasses =
        allPlayers
            .filter { player -> player.userId != userId }
            .mapNotNull { player -> (player as? PlayerState.Selected)?.characterClass?.type }
            .toSet()

    val classSelections =
        allPlayers.filterIsInstance<PlayerState.Selected>().associate { player ->
            player.characterClass.type to player.displayName
        }

    return unavailableClasses to classSelections
}

// TODO: Group similar code into separate files when it grows like this. 3 timer-related functions.
internal fun determineTimerAction(
    previous: ArenaState?,
    next: ArenaState,
    isFinalMatch: Boolean,
): TimerAction =
    when (next) {
        is ArenaState.WaitingForAllPlayers ->
            if (previous == null) {
                TimerAction.Start(timerDuration = WAITING_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaState.ChoosingClasses ->
            if (
                previous !is ArenaState.ChoosingClasses ||
                    previous.activePlayerUserId != next.activePlayerUserId
            ) {
                TimerAction.Start(timerDuration = CHOOSING_CLASSES_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        // The combat clock belongs to the action a player currently owes, so it (re)starts only
        // when that action changes (a new turn or roll) and keeps running across unrelated updates
        // such as incoming messages.
        is ArenaState.Combat ->
            if (
                previous !is ArenaState.Combat ||
                    previous.combatActionState != next.combatActionState
            ) {
                TimerAction.Start(timerDuration = COMBAT_TIMER_DURATION_SECONDS)
            } else {
                TimerAction.None
            }

        is ArenaState.Finished ->
            if (isFinalMatch) {
                determineGauntletFinishedTimerAction(previous = previous, next = next)
            } else {
                determineMatchFinishedTimerAction(previous = previous, next = next)
            }
    }

private fun determineMatchFinishedTimerAction(
    previous: ArenaState?,
    next: ArenaState.Finished,
): TimerAction =
    when {
        !next.canStartNextMatch -> TimerAction.Stop
        previous is ArenaState.Finished -> TimerAction.None
        else -> TimerAction.Start(timerDuration = MATCH_FINISHED_TIMER_DURATION_SECONDS)
    }

private fun determineGauntletFinishedTimerAction(
    previous: ArenaState?,
    next: ArenaState.Finished,
): TimerAction {
    val wasAlreadyCountingDown = previous is ArenaState.Finished && previous.hasAnyPlayerReadied
    return when {
        !next.hasAnyPlayerReadied -> TimerAction.Stop
        wasAlreadyCountingDown -> TimerAction.None
        else -> TimerAction.Start(timerDuration = GAUNTLET_FINISHED_TIMER_DURATION_SECONDS)
    }
}

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
