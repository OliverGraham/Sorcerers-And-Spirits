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

        return ArenaMatchEntity.Finished(
            messages = messages,
            numberOfNewMessages = 0,
            playerEntitiesMap = playerEntitiesMap,
            matchNumberInGauntlet = matchNumberInGauntlet,
            participants = participants.ifEmpty { playerEntitiesMap.toParticipants() },
        )
    }
}
