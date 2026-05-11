package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toSelectedEntity
import com.linklabs.sorcerersandspirits.data.arena.utilities.ChooseCharacterClass
import com.linklabs.sorcerersandspirits.data.arena.utilities.ConnectToArena
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.nullable

// TODO Arena: Paginate messages and/or have a separate node for them to make more
//  efficient. Could also save all messages and get on demand, and only have the latest 30 or
//  something

class FirebaseArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)

    // TODO Arena: Cleanup, docstring
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

    /**
     * Connects a user to the arena. Uses a transaction so the first player initializes the match,
     * and so gets the honor of being first in the game.
     */
    override suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ) =
        ConnectToArena(
                arenaReference = arenaMatchesReference.child(path = arenaId),
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = displayName,
            )
            .invoke()

    override suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaMatchesReference
            .child(path = arenaId)
            .child(path = ArenaMatchEntity.playerEntitiesMapName)
            .child(path = arenaPlayer.userId)
            .setValue(value = arenaPlayer.toEntity())
    }

    override suspend fun performCombatAction(
        arenaId: String,
        attackerUserId: String,
        targetUserId: String,
        rollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType,
    ) {
        com.linklabs.sorcerersandspirits.data.arena.utilities
            .PerformCombatAction(
                arenaReference = arenaMatchesReference.child(path = arenaId),
                attackerUserId = attackerUserId,
                targetUserId = targetUserId,
                rollType = rollType,
            )
            .invoke()
    }

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> =
        arenaMatchesReference
            .child(path = arenaId)
            .valueEvents
            .map { snapshot ->
                snapshot.value(strategy = ArenaMatchEntity.serializer().nullable)?.toDomain()
            }
            .distinctUntilChanged()
}
