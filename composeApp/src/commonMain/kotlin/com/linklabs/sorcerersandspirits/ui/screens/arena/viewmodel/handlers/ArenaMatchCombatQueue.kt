package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toMessageStates
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
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
    private var flashSignal: CompletableDeferred<Unit>? = null
    private var avatarSignal: CompletableDeferred<Unit>? = null

    fun enqueue(arenaMatchUpdate: ArenaMatch.Combat) {
        queue.add(element = arenaMatchUpdate)
    }

    suspend fun onCombatActionResolved(userId: String) {
        resolveMutex.withLock {
            if (queue.isEmpty()) return@withLock

            val arenaMatchCombatUpdate = queue.removeFirst()
            val messages = arenaMatchCombatUpdate.messages.toMessageStates()

            messages.forEach { message ->
                triggerAnimation(/* arenaMessageState= */ message)

                val animationSignals = message.animation.toAnimationSignals()
                animationSignals.awaitAll()

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

    fun onFlashComplete() {
        val signal = flashSignal
        flashSignal = null
        signal?.complete(value = Unit)
    }

    fun onAvatarComplete() {
        val signal = avatarSignal
        avatarSignal = null
        signal?.complete(value = Unit)
    }

    private fun ArenaAnimationState.toAnimationSignals() = buildList {
        if (isFlash()) {
            val flashSignal = CompletableDeferred<Unit>()
            this@ArenaMatchCombatQueue.flashSignal = flashSignal
            add(element = flashSignal)
        }

        if (isAvatar()) {
            val avatarSignal = CompletableDeferred<Unit>()
            this@ArenaMatchCombatQueue.avatarSignal = avatarSignal
            add(element = avatarSignal)
        }
    }

    private fun ArenaMatch.Combat.isTeleportRollOffComplete(
        userId: String,
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
