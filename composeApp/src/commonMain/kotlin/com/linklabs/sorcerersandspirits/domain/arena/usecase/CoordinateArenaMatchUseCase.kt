package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// CHANGE: this use case becomes the single owner of the UI-facing message log. It takes the RTDB
// ArenaMatch flow (each emission carries only the latest batch), writes that batch into the
// on-device ArenaMessageStore, then emits an ArenaMatch whose `messages` = full accumulated log from
// the store and `newMessages` = the batch just written.
//
// QUESTIONS for the store seam (new dependency below):
//   1) Store shape: is a single suspend `appendAndLoad(arenaId, batch): List<IncomingArenaMessage>`
//      enough (we drive emissions off the RTDB flow, so no separate store Flow needed), or do you
//      want the store to expose its own Flow that we combine()? Leaning: appendAndLoad per emission.
//   2) Dedup: Firebase re-emits the snapshot on ANY field change, so the same batch can arrive
//      twice. Dedup by message id when appending (store ignores ids it already holds)? Leaning yes.
//   3) Scoping: key the store by arenaId so a fresh arena starts clean?
//   4) Clear: wipe the store at the new-match / new-gauntlet boundary (detected here via
//      matchNumberInGauntlet change, or roundNumber==1 && isNewRound on a fresh Combat). Where
//      exactly do you want the clear to fire?
//  Look at my thoughts in ArenaMatchEntity.kt first.
//  1: This may be moot. If still relevant, then yes, appendAndLoad().
//  2: Yeah, dedupe by message id - so can you save as an ordered map to get that O(1) lookup?
//  3: Yes
//  4: I'll want to clear it elsewhere, most likely. Don't worry about it for now.
class CoordinateArenaMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
    private val profileRepository: ProfileRepository,
    // private val arenaMessageStore: ArenaMessageStore,
) {
    suspend operator fun invoke(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        computerOpponents: List<ComputerOpponent> = emptyList(),
    ): Flow<ArenaMatch?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)
        val profile = profileRepository.getProfile(userId = userId) ?: return flowOf(value = null)

        computerOpponents.forEach { opponent ->
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = opponent.userId,
                displayName = opponent.displayName,
            )
        }

        val isUserConnected =
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = profile.displayName,
            )

        return if (isUserConnected) {
            // CHANGE: map each RTDB emission through the store:
            //   1. take arenaMatch.newMessages (the batch)
            //   2. arenaMessageStore.appendAndLoad(arenaId, batch) -> full log (with dedup)
            //   3. emit arenaMatch.copy(messages = fullLog) [newMessages already set]
            // plus fire a store.clear(arenaId) when a new-match boundary is detected (see QUESTIONs).
            arenaRepository.getArenaMatchFlow(arenaId = arenaId)
        } else {
            flowOf(value = null)
        }
    }
}
