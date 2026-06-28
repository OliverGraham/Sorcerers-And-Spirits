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
private const val FINISHED_TIMER_DURATION_SECONDS = 60

@Immutable
data class ArenaScreenState(
    val messages: List<ArenaMessageState>,
    val secondsRemaining: Int,
    val showForfeitDialog: Boolean,
    val isUserPlayerKnockedOut: Boolean,
    val hasReadiedForNextMatch: Boolean,
    val arenaState: ArenaState,
    val participantClassNames: Map<String, String>,
    val numberOfNewMessages: Int,
    val gauntletProgress: GauntletProgress = GauntletProgress(),
    val characterClasses: List<CharacterClassState> = emptyList(),
    val classCopy: CharacterClassCopy = CharacterClassCopy.EMPTY,
    val pendingFinishedState: ArenaState.Finished? = null,
) {
    val isTimerVisible: Boolean = secondsRemaining in 1..TIMER_VISIBLE_THRESHOLD_SECONDS

    val isTimerUrgent: Boolean = secondsRemaining in 1..TIMER_URGENT_THRESHOLD_SECONDS

    /**
     * The most recent roll, so the shake-to-roll dice settle on the rolled value (SS-077) and a new
     * roll is told apart from the previous one even when it lands on the same faces (SS-117).
     */
    internal val latestRoll: LatestRoll = messages.latestRoll()

    /**
     * The most recent flash-worthy event (a normal-attack outcome), read from the raw [messages] so
     * the full-screen "Hit!"/"Miss!"/"K.O.!" flash fires immediately rather than waiting for the
     * throttled log (SS-148). Null until the first such event.
     */
    val latestFlashMessage: ArenaMessageState? = messages.lastOrNull { it.flashText != null }
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
    ) : ActiveState {
        val userPlayer = playerMap.getValue(key = userPlayerId)
        val activeUserIds = combatActionState.activeUserIds
        val otherPlayers = playerMap.filterKeys { it !in activeUserIds }
        val isItThisUsersTurn = userPlayerId in activeUserIds
        val turnOwnerUserId = combatActionState.turnOwnerUserId
        val turnOwnerPlayer = playerMap.getValue(key = turnOwnerUserId)
        val combatControls = deriveCombatControls()
        val lockedTargetUserId = deriveLockedTargetUserId()
        val autoTargetedOpponentUserId = deriveAutoTargetedOpponentUserId()
    }

    @Immutable
    data class Finished(
        val players: List<PlayerState.Selected>,
        val hasAnyPlayerReadied: Boolean = false,
    ) : ArenaState {
        val survivors = players.filter { it.characterClass.isConscious }
        val winner = survivors.singleOrNull()
    }
}

@Immutable
sealed interface ActiveState : ArenaState {
    val playerMap: Map<UserId, PlayerState>
    val userPlayerId: String
}

/**
 * The opponent the user is committed to attacking while one of their own normal attacks is
 * mid-resolution — the Armor-class roll is already committed and a follow-up roll is owed. The UI
 * pins the target to this opponent and stops offering others, so the user cannot change targets
 * after rolling. Null when the user is free to choose a target: at the start of their turn or when
 * an ability selects its own target.
 */
private fun ArenaState.Combat.deriveLockedTargetUserId(): String? {
    val inFlightAttack = combatActionState as? CombatActionState.NormalAttack ?: return null
    val isUsersOwnAttack = inFlightAttack.attackerUserId == userPlayerId
    return if (isUsersOwnAttack) inFlightAttack.defenderUserId else null
}

/**
 * The lone opponent to pre-target when the user has nothing to choose this turn. With a single
 * opponent and a normal attack as the only action the user can actually take, there is no target
 * decision to make, so the UI selects that opponent automatically rather than requiring a manual
 * tap. Only enabled controls count: a disabled ability is not a real choice, so leaving it in the
 * list must not suppress auto-targeting — otherwise the Gambler's granted All-In second attack,
 * which sits in an idle turn alongside the now-disabled All-In control, could never be rolled in a
 * two-player match (SS-116). Null whenever a real choice exists: more than one opponent, another
 * enabled activated ability is available, or it is not the user's free-choice turn.
 */
private fun ArenaState.Combat.deriveAutoTargetedOpponentUserId(): String? {
    val isStartOfUsersTurn = isItThisUsersTurn && combatActionState is CombatActionState.Idle
    if (!isStartOfUsersTurn) return null

    val enabledControls = combatControls.filter { it.enabled }
    val isNormalAttackOnlyAction = enabledControls.singleOrNull() is ArenaCombatControl.NormalAttack
    if (!isNormalAttackOnlyAction) return null

    return playerMap.keys.singleOrNull { it != userPlayerId }
}

/**
 * The player shown in the right-hand target avatar slot, in priority order. The Domain-pinned
 * defender of an in-flight attack wins; then, on the user's own turn, the user's tapped
 * [selectedOpponentUserId]; then the in-flight action's defender for a turn the user is only
 * watching (SS-157); and finally the turn owner's lone conscious opponent. That last fallback is
 * the implicit target when there is no choice to make — a two-player match, or any match down to
 * one conscious opponent — so it shows on every device and for every class, with no targeting phase
 * (SS-101). Empty only when the turn owner has several conscious opponents and has not picked one.
 * The selection is Composable-local interaction state, threaded in rather than derived at map time.
 */
internal fun ArenaState.Combat.resolveTargetUserId(selectedOpponentUserId: String): String {
    val isUserTheTurnOwner = turnOwnerUserId == userPlayerId
    val userChosenTarget =
        if (isUserTheTurnOwner) lockedTargetUserId ?: selectedOpponentUserId else ""
    return userChosenTarget
        .ifEmpty { combatActionState.targetedDefenderUserId.orEmpty() }
        .ifEmpty { turnOwnerLoneOpponentUserId.orEmpty() }
}

/**
 * The single conscious opponent of the current turn owner, or null when they have none or more than
 * one. With only one possible target there is no choice to make, so it is the implicit target every
 * device shows before the turn owner commits a roll.
 */
private val ArenaState.Combat.turnOwnerLoneOpponentUserId: String?
    get() =
        playerMap.values
            .singleOrNull { it.userId != turnOwnerUserId && it.characterClass.isConscious }
            ?.userId

/**
 * The opponent shown as the full bottom-half target avatar (SS-145) for a resolved [targetUserId],
 * or null when there is none or it is knocked out — a KO'd player is never the target avatar and
 * drops back to a head icon instead (SS-152).
 */
internal fun ArenaState.Combat.targetPlayerFor(targetUserId: String): PlayerState.Selected? {
    val target = playerMap[targetUserId] ?: return null
    return if (target.characterClass.isConscious) target else null
}

/**
 * The opponents shown as collapsed head icons (SS-143) for an [activeUserId] turn owner and a
 * resolved [targetUserId]: every player except those two — the turn owner (left) and the current
 * target avatar (right, [targetPlayerFor]) — who are the two full bottom-half avatars instead
 * (SS-157). A knocked-out opponent is never the target avatar, so it always falls back into the
 * head-icon row, where it is rendered grayed-out and out of the fight (SS-152, mirror SS-123).
 */
internal fun ArenaState.Combat.headIconPlayersFor(
    activeUserId: String,
    targetUserId: String,
): List<PlayerState.Selected> {
    val targetPlayer = targetPlayerFor(targetUserId = targetUserId)
    return playerMap.values.filter {
        it.userId != activeUserId && it.userId != targetPlayer?.userId
    }
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
    val currentClassNames =
        playerMap.values.filterIsInstance<ArenaPlayer.Selected>().associate {
            it.userId to it.characterClass.className
        }
    val participantClassNames =
        (previousState?.participantClassNames ?: emptyMap()) + currentClassNames

    val messages =
        messages.map { incomingMessage ->
            incomingMessage.message.toState(
                id = incomingMessage.id,
                classNames = participantClassNames,
            )
        }
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

                ArenaState.ChoosingClasses(
                    playerMap = playerMap.mapValues { it.value.toState() },
                    activePlayerUserId = activePlayer.userId,
                    userPlayerId = userId,
                    unavailableClasses = unavailable,
                    classSelections = selections,
                    selectedClass =
                        (previousState?.arenaState as? ArenaState.ChoosingClasses)?.selectedClass,
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

    val nextScreenState =
        ArenaScreenState(
            messages = messages,
            secondsRemaining = previousSeconds,
            showForfeitDialog = previousShowForfeitDialog,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = displayedArenaState,
            participantClassNames = participantClassNames,
            gauntletProgress = GauntletProgress(currentMatch = matchNumberInGauntlet),
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
            determineTimerAction(previous = previousState?.arenaState, next = displayedArenaState)
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

internal fun determineTimerAction(previous: ArenaState?, next: ArenaState): TimerAction =
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

        // The Finished screen waits indefinitely until someone readies for the next match; the
        // first
        // ready starts a single countdown (shared across clients, since readiness is shared), after
        // which a timeout auto-readies each remaining player. Later readies neither restart nor
        // stop
        // it. This is identical whether the next step is another match or a brand-new Gauntlet.
        is ArenaState.Finished -> {
            val wasAlreadyCountingDown =
                previous is ArenaState.Finished && previous.hasAnyPlayerReadied
            when {
                !next.hasAnyPlayerReadied -> TimerAction.Stop
                wasAlreadyCountingDown -> TimerAction.None
                else -> TimerAction.Start(timerDuration = FINISHED_TIMER_DURATION_SECONDS)
            }
        }
    }

data class ArenaStateUpdate(val nextState: ArenaScreenState, val timerAction: TimerAction)
