package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toParticipants
import dev.gitlive.firebase.database.DatabaseReference

internal class FinishMatch {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { match ->
            match.finishIfTerminal()
        }
    }

    private fun ArenaMatchEntity.finishIfTerminal(): ArenaMatchEntity {
        if (this !is ArenaMatchEntity.Combat) return this
        if (!isMatchOver) return this

        // CHANGE: numberOfNewMessages removed. This is a pure state transition with NO new batch,
        // so `messages` here should become emptyList() (nothing new to append to the on-device log;
        // the full log already lives in the store). QUESTION: confirm empty batch on transitions.
        // Sure, for now. I might want to have new messages here in the future, but for now, nothing
        // should be ok.
        return ArenaMatchEntity.Finished(
            messages = messages,
            playerEntitiesMap = playerEntitiesMap,
            matchNumberInGauntlet = matchNumberInGauntlet,
            participants = participants.ifEmpty { playerEntitiesMap.toParticipants() },
        )
    }
}
