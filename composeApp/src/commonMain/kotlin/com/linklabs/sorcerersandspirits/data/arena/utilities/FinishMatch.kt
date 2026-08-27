package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
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

        // TODO: add messages?
        return ArenaMatchEntity.Finished(
            newMessages = emptyList(),
            playerEntitiesMap = playerEntitiesMap,
            matchNumberInGauntlet = matchNumberInGauntlet,
        )
    }
}
