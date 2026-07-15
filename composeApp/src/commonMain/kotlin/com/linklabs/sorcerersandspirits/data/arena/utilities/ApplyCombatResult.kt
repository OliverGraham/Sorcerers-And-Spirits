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

        val newMessages =
            result.messages.map { outgoingArenaMessage ->
                outgoingArenaMessage.toEntity(id = messagesReference.addNewKey())
            }

        val updatedMessages = messages.appendCapped(newMessages = newMessages)
        return if (result.isMatchFinished) {
            toFinished(updatedMessages = updatedMessages)
        } else {
            toUpdatedCombat(updatedMessages = updatedMessages)
        }
    }

    private fun ArenaMatchEntity.Combat.toFinished(
        updatedMessages: List<ArenaMessageEntity>
    ): ArenaMatchEntity.Finished =
        ArenaMatchEntity.Finished(
            messages = updatedMessages,
            numberOfNewMessages = result.messages.size,
            playerEntitiesMap = result.updatedPlayerMap.mapValues { it.value.toEntity() },
            matchNumberInGauntlet = matchNumberInGauntlet,
            participants = participants.ifEmpty { playerEntitiesMap.toParticipants() },
        )

    private fun ArenaMatchEntity.Combat.toUpdatedCombat(
        updatedMessages: List<ArenaMessageEntity>
    ): ArenaMatchEntity.Combat =
        ArenaMatchEntity.Combat(
            messages = updatedMessages,
            numberOfNewMessages = result.messages.size,
            playerEntitiesMap = result.updatedPlayerMap.mapValues { it.value.toSelectedEntity() },
            nextCombatActionEntity = result.nextCombatAction.toEntity(),
            roundNumber = result.roundNumber,
            isNewRound = result.isNewRound,
            doublesBonusEntity = result.doublesBonus?.toEntity(),
            isMirrorImagePenaltyActive = result.isMirrorImagePenaltyActive,
            matchNumberInGauntlet = matchNumberInGauntlet,
            myDiceRoll = result.myDiceRoll?.toEntity(),
            participants = participants.ifEmpty { playerEntitiesMap.toParticipants() },
        )
}
