package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toMessageStates
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MATCH_OVER_PAUSE_MILLIS = 1500L

internal class ArenaMatchCombatQueue(
    private val triggerAnimation: (ArenaMessageState?) -> Unit,
    private val addMessageToScreenState: (ArenaMessageState) -> Unit,
    private val onAnimationsComplete: (ArenaMatch.Combat) -> Unit,
    private val onTeleportRollOffComplete: (TeleportRollsArguments) -> Unit,
    private val onHeaderAnimationsComplete: () -> Unit,
    private val finishMatch: suspend () -> Unit,
) {
    private val queue = ArrayDeque<ArenaMatch.Combat>()

    // TODO: Determine if Mutex is needed
    private val resolveMutex = Mutex()
    private var currentAnimationCoordinator: MessageAnimationCoordinator? = null

    fun enqueue(arenaMatchUpdate: ArenaMatch.Combat) {
        queue.add(element = arenaMatchUpdate)
    }

    suspend fun onCombatActionResolved(userId: String) {
        resolveMutex.withLock {
            if (queue.isEmpty()) return@withLock

            val arenaMatchCombatUpdate = queue.removeFirst()
            val roundAndTurnHeaders = arenaMatchCombatUpdate.animateMessages()

            if (arenaMatchCombatUpdate.isMatchOver) {
                delay(timeMillis = MATCH_OVER_PAUSE_MILLIS)
                finishMatch()
                return@withLock
            }

            onAnimationsComplete(/* arenaMatchCombatUpdate = */ arenaMatchCombatUpdate)

            arenaMatchCombatUpdate.completeTeleportIfApplicable(userId = userId)

            roundAndTurnHeaders.animationRoundAndTurnMessages()
        }
    }

    fun onAnimationComplete(arguments: AnimationCompleteArguments) {
        currentAnimationCoordinator?.markAnimationAsFinishedIfApplicable(arguments = arguments)
    }

    private suspend fun ArenaMatch.Combat.animateMessages(): List<ArenaMessageState> {
        val (roundAndTurnHeaders, animationMessages) =
            messages.toMessageStates().partition { it.isRoundOrTurnHeader() }

        animationMessages.forEach { message ->
            message.animateMessage()
        }

        return roundAndTurnHeaders
    }

    private suspend fun ArenaMessageState.animateMessage() {
        val animationCoordinator = MessageAnimationCoordinator(message = this)
        currentAnimationCoordinator = animationCoordinator

        if (isAnimated()) {
            triggerAnimation(/* arenaMessageState = */ this)
        }
        animationCoordinator.await()

        currentAnimationCoordinator = null
        addMessageToScreenState(/* arenaMessageState = */ this)
    }

    private fun ArenaMatch.Combat.completeTeleportIfApplicable(
        userId: String,
    ) {
        val teleportRollsArguments = isTeleportRollOffComplete(userId = userId)
        if (teleportRollsArguments != null) {
            onTeleportRollOffComplete(teleportRollsArguments)
        }
    }

    private fun ArenaMatch.Combat.isTeleportRollOffComplete(
        userId: String,
    ): TeleportRollsArguments? {
        val abilityRoll = nextCombatAction as? NextCombatAction.AbilityRoll ?: return null
        val teleport = abilityRoll.purpose as? AbilityAction.Passive.Teleport ?: return null

        val mageUserId = teleport.defenderRoll.userId
        val isMageDrivenByThisClient = mageUserId == userId
        if (!isMageDrivenByThisClient) return null

        if (teleport.attackerRoll.roll == null || teleport.defenderRoll.roll == null) return null

        return TeleportRollsArguments(
            mageUserId = mageUserId,
            playerMap = playerMap,
            roundNumber = roundNumber,
            nextCombatAction = nextCombatAction,
            teleportOfNextCombatAction = teleport,
        )
    }

    private suspend fun List<ArenaMessageState>.animationRoundAndTurnMessages() {
        if (isNotEmpty()) {
            forEach { header -> header.animateMessage() }
            onHeaderAnimationsComplete()
        }
    }

    private fun ArenaMessageState.isRoundOrTurnHeader(): Boolean = this is ArenaMessageState.NewRound || this is ArenaMessageState.NewTurn
}
