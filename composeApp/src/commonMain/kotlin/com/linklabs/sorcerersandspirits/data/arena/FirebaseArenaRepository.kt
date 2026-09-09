package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
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
import com.linklabs.sorcerersandspirits.data.arena.utilities.applycombatresult.ApplyCombatResult
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

class FirebaseArenaRepository(
    database: FirebaseDatabase,
    private val coroutineScope: CoroutineScope = CoroutineScope(context = Dispatchers.IO),
) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)
    private val arenaPresenceReference = database.reference(path = ARENA_PRESENCE_PATH)
    private val connectedReference = database.reference(path = CONNECTED_REFERENCE_PATH)

    private var presenceJob: Job? = null

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

        presenceJob?.cancel()
        presenceJob =
            connectedFlow()
                .distinctUntilChanged()
                .filter { isConnected -> isConnected }
                .onEach {
                    playerPresenceReference.onDisconnect().setValue(value = false)
                    playerPresenceReference.setValue(value = true)
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
        ApplyCombatResult(result = result)
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> = arenaMatchesReference
        .child(path = arenaId)
        .valueEvents
        .map { snapshot ->
            snapshot.value(strategy = ArenaMatchEntity.serializer().nullable)?.toDomain()
        }
        .distinctUntilChanged()

    override suspend fun exitArena(arenaId: String, userId: String) {
        ExitArena(userId = userId)
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }

    override suspend fun stayForAnotherMatch(arenaId: String, userId: String) {
        StayForAnotherMatch(userId = userId)
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }

    override suspend fun finishMatch(arenaId: String) {
        FinishMatch().invoke(arenaReference = arenaMatchesReference.child(path = arenaId))
    }
}
