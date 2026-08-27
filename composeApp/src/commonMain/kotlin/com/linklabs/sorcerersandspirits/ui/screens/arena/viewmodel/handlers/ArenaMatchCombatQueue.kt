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
            val messages = arenaMatchCombatUpdate.messages.toMessageStates()

            messages.forEach { message ->
                val animationCoordinator = MessageAnimationCoordinator(message = message)
                currentAnimationCoordinator = animationCoordinator

                triggerAnimation(/* arenaMessageState= */ message)
                animationCoordinator.await()

                currentAnimationCoordinator = null
                addMessageToScreenState(/* arenaMessageState= */ message)
            }

            if (arenaMatchCombatUpdate.isMatchOver) {
                delay(timeMillis = MATCH_OVER_PAUSE_MILLIS)
            }

            onAnimationsComplete(arenaMatchCombatUpdate)

            val teleportRollsArguments =
                arenaMatchCombatUpdate.isTeleportRollOffComplete(userId = userId)
            if (teleportRollsArguments != null) {
                onTeleportRollOffComplete(teleportRollsArguments)
            }
        }
    }

    fun onAnimationComplete(arguments: AnimationCompleteArguments) {
        currentAnimationCoordinator?.markAnimationAsFinishedIfApplicable(arguments = arguments)
    }

    private fun ArenaMatch.Combat.isTeleportRollOffComplete(
        userId: String
    ): TeleportRollsArguments? {
        val abilityRoll = nextCombatAction as? NextCombatAction.AbilityRoll ?: return null
        val teleport = abilityRoll.purpose as? AbilityAction.Passive.Teleport ?: return null

        val isUserTheMage = teleport.defenderRoll.userId == userId
        if (!isUserTheMage) return null

        if (teleport.attackerRoll.roll == null || teleport.defenderRoll.roll == null) return null

        return TeleportRollsArguments(
            mageUserId = userId,
            playerMap = playerMap,
            roundNumber = roundNumber,
            nextCombatAction = nextCombatAction,
            teleportOfNextCombatAction = teleport,
        )
    }
}
