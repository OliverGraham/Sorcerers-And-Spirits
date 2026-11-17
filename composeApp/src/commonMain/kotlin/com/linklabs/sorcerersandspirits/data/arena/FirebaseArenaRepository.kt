package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MEMBERSHIP_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_PRESENCE_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CONNECTED_REFERENCE_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toSelectedEntity
import com.linklabs.sorcerersandspirits.data.arena.utilities.ChooseCharacterClass
import com.linklabs.sorcerersandspirits.data.arena.utilities.ConnectToArena
import com.linklabs.sorcerersandspirits.data.arena.utilities.ExitArena
import com.linklabs.sorcerersandspirits.data.arena.utilities.FinishMatch
import com.linklabs.sorcerersandspirits.data.arena.utilities.StayForAnotherMatch
import com.linklabs.sorcerersandspirits.data.arena.utilities.addNewKey
import com.linklabs.sorcerersandspirits.data.arena.utilities.applycombatresult.ApplyCombatResult
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_COMBAT_WRITE_ATTEMPTS = 4
private const val COMBAT_WRITE_TIMEOUT_MILLIS = 4000L
private const val COMBAT_WRITE_RETRY_BACKOFF_MILLIS = 500L

class FirebaseArenaRepository(
    database: FirebaseDatabase,
    private val coroutineScope: CoroutineScope = CoroutineScope(context = Dispatchers.IO),
) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)
    private val arenaPresenceReference = database.reference(path = ARENA_PRESENCE_PATH)
    private val arenaMembershipReference = database.reference(path = ARENA_MEMBERSHIP_PATH)
    private val connectedReference = database.reference(path = CONNECTED_REFERENCE_PATH)

    private var presenceJob: Job? = null
    private var presenceReference: DatabaseReference? = null
    private var presenceUserId: String? = null

    override suspend fun chooseCharacterClass(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    ) {
        ChooseCharacterClass(
            arenaReference = arenaMatchesReference.child(path = arenaId),
            selectedArenaPlayer = selectedArenaPlayer.toSelectedEntity(),
            nextPlayerId = nextPlayerId,
        )
            .invoke()
    }

    override suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ) = ConnectToArena(
        expectedNumberOfPlayers = expectedNumberOfPlayers,
        userId = userId,
        displayName = displayName,
    )
        .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))

    override fun connectArenaPresence(arenaId: String, userId: String) {
        val playerPresenceReference =
            arenaPresenceReference.child(path = arenaId).child(path = userId)
        presenceReference = playerPresenceReference
        presenceUserId = userId

        presenceJob?.cancel()
        presenceJob =
            connectedFlow()
                .distinctUntilChanged()
                .filter { isConnected -> isConnected }
                .onEach {
                    playerPresenceReference.onDisconnect().setValue(value = false)
                    playerPresenceReference.setValue(value = true)
                    arenaMembershipReference.child(path = userId).setValue(value = arenaId)
                }
                .launchIn(scope = coroutineScope)
    }

    override fun getArenaPresenceFlow(arenaId: String): Flow<Map<String, Boolean>> = arenaPresenceReference
        .child(path = arenaId)
        .valueEvents
        .map { snapshot ->
            snapshot.value(
                strategy =
                MapSerializer(
                    keySerializer = String.serializer(),
                    valueSerializer = Boolean.serializer(),
                )
                    .nullable,
            )
                ?: emptyMap()
        }
        .distinctUntilChanged()

    private fun connectedFlow(): Flow<Boolean> = connectedReference.valueEvents.map { snapshot ->
        snapshot.value<Boolean?>() ?: false
    }

    override suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaMatchesReference
            .child(path = arenaId)
            .child(path = ArenaMatchEntity.playerEntitiesMapName)
            .child(path = arenaPlayer.userId)
            .setValue(value = arenaPlayer.toEntity())
    }

    override suspend fun applyCombatResult(arenaId: String, result: ActionOutcome) {
        val arenaReference = arenaMatchesReference.child(path = arenaId)
        val actionId = arenaReference.addNewKey()
        val applyCombatResult = ApplyCombatResult(result = result, actionId = actionId)

        repeat(times = MAX_COMBAT_WRITE_ATTEMPTS) {
            val didApply =
                applyCombatResult.tryApply(arenaReference = arenaReference)
            if (didApply) return

            delay(timeMillis = COMBAT_WRITE_RETRY_BACKOFF_MILLIS)
        }
    }

    private suspend fun ApplyCombatResult.tryApply(arenaReference: DatabaseReference): Boolean = try {
        withTimeoutOrNull(timeMillis = COMBAT_WRITE_TIMEOUT_MILLIS) {
            invoke(arenaReference = arenaReference)
            true
        } ?: false
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        false
    }

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> = arenaMatchesReference
        .child(path = arenaId)
        .valueEvents
        .map { snapshot ->
            snapshot.value(strategy = ArenaMatchEntity.serializer().nullable)?.toDomain()
        }
        .distinctUntilChanged()

    override suspend fun exitArena(arenaId: String, userId: String) {
        removeFromMatch(arenaId = arenaId, userId = userId)
        releaseOwnArenaMembership(arenaId = arenaId, userId = userId)
    }

    override suspend fun removeDisconnectedPlayer(arenaId: String, userId: String) {
        removeFromMatch(arenaId = arenaId, userId = userId)
        pruneArenaPresence(arenaId = arenaId, userId = userId)
    }

    private suspend fun removeFromMatch(arenaId: String, userId: String) {
        ExitArena(userId = userId)
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }

    private suspend fun releaseOwnArenaMembership(arenaId: String, userId: String) {
        tearDownPresenceWriter()
        pruneArenaPresence(arenaId = arenaId, userId = userId)
        arenaMembershipReference.child(path = userId).removeValue()
    }

    override suspend fun syncArenaMembershipOnConnect(userId: String) {
        val orphanedArenaId =
            try {
                arenaMembershipReference.child(path = userId).awaitNullableString()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return
            }
        if (orphanedArenaId.isNullOrBlank()) return

        try {
            exitArena(arenaId = orphanedArenaId, userId = userId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            releaseOwnArenaMembership(arenaId = orphanedArenaId, userId = userId)
            deleteMatchIfUndecodable(arenaId = orphanedArenaId)
        }
    }

    private suspend fun deleteMatchIfUndecodable(arenaId: String) {
        val matchReference = arenaMatchesReference.child(path = arenaId)
        val snapshot =
            try {
                matchReference.valueEvents.first()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return
            }

        if (!snapshot.exists) return
        if (snapshot.isDecodableArenaMatch()) return

        matchReference.removeValue()
    }

    private fun DataSnapshot.isDecodableArenaMatch(): Boolean = try {
        value(strategy = ArenaMatchEntity.serializer().nullable)
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun disconnectArenaPresence() {
        val leavingUserId = presenceUserId ?: return
        val leavingPresenceReference = presenceReference

        tearDownPresenceWriter()

        leavingPresenceReference?.removeValue()
        arenaMembershipReference.child(path = leavingUserId).removeValue()
    }

    private suspend fun tearDownPresenceWriter() {
        presenceJob?.cancel()
        presenceJob = null
        presenceReference?.onDisconnect()?.cancel()
        presenceReference = null
        presenceUserId = null
    }

    private suspend fun pruneArenaPresence(arenaId: String, userId: String) {
        val arenaPresenceForMatch = arenaPresenceReference.child(path = arenaId)
        val isMatchGone = !arenaMatchesReference.child(path = arenaId).awaitExists()

        if (isMatchGone) {
            arenaPresenceForMatch.removeValue()
        } else {
            arenaPresenceForMatch.child(path = userId).removeValue()
        }
    }

    private suspend fun DatabaseReference.awaitExists(): Boolean = valueEvents.first().exists

    private suspend fun DatabaseReference.awaitNullableString(): String? {
        val snapshot = valueEvents.first()
        return if (snapshot.exists) snapshot.value<String?>() else null
    }

    override suspend fun stayForAnotherMatch(arenaId: String, userId: String) {
        StayForAnotherMatch(userId = userId)
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }

    override suspend fun finishMatch(arenaId: String) {
        FinishMatch().invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }
}
