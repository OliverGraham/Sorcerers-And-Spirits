package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toParticipants
import com.linklabs.sorcerersandspirits.data.arena.model.toSelectedEntity
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import dev.gitlive.firebase.database.DatabaseReference

internal class ApplyCombatResult(private val result: ActionOutcome) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        val messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName)

        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { existingMatch ->
            existingMatch.applyCombatResultTransaction(messagesReference = messagesReference)
        }
    }

    private fun ArenaMatchEntity.applyCombatResultTransaction(
        messagesReference: DatabaseReference
    ): ArenaMatchEntity {
        if (this !is ArenaMatchEntity.Combat) return this

        // TODO: return updatedMessages from a function. FFS

        // CHANGE: RTDB's messages node becomes a REPLACE, not an append. Write only this batch;
        // drop appendCapped / the 30-cap entirely. The RTDB node now holds ONLY the latest batch,
        // which the domain layer will treat as `newMessages`. The full accumulated log lives
        // on-device (DataStore), not in RTDB.
        // QUESTION: keep generating push-key ids per message (still useful for on-device dedup
        // across duplicate snapshots), or switch to a simpler stable id scheme now that RTDB no
        // longer accumulates? Leaning: keep push keys for dedup.

        // Yes, push keys. Also, don't remove my TODO comments if you're not taking care of them.
        val newMessages =
            result.messages.map { outgoingArenaMessage ->
                outgoingArenaMessage.toEntity(id = messagesReference.addNewKey())
            }

        return toUpdatedCombat(updatedMessages = newMessages)
    }

    private fun ArenaMatchEntity.Combat.toUpdatedCombat(
        updatedMessages: List<ArenaMessageEntity>
    ): ArenaMatchEntity.Combat =
        // CHANGE: numberOfNewMessages is deleted from the entity. `messages` here IS the new batch.
        // Good
        ArenaMatchEntity.Combat(
            messages = updatedMessages,
            playerEntitiesMap = result.updatedPlayerMap.mapValues { it.value.toSelectedEntity() },
            nextCombatActionEntity = result.nextCombatAction.toEntity(),
            roundNumber = result.roundNumber,
            isNewRound = result.isNewRound,
            isMatchOver = result.isMatchFinished,
            matchNumberInGauntlet = matchNumberInGauntlet,
            diceRoll = result.diceRoll?.toEntity(),
            participants = participants.ifEmpty { playerEntitiesMap.toParticipants() },
        )
}
