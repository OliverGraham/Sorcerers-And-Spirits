package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toSelectedEntity
import com.linklabs.sorcerersandspirits.data.arena.utilities.ApplyCombatResult
import com.linklabs.sorcerersandspirits.data.arena.utilities.ChooseCharacterClass
import com.linklabs.sorcerersandspirits.data.arena.utilities.ConnectToArena
import com.linklabs.sorcerersandspirits.data.arena.utilities.ExitArena
import com.linklabs.sorcerersandspirits.data.arena.utilities.StayForAnotherMatch
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.nullable

class FirebaseArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)

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
    ) =
        ConnectToArena(
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = displayName,
            )
            .invoke(arenaReference = arenaMatchesReference.child(path = arenaId))

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

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> =
        arenaMatchesReference
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
}
