package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
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

        return if (result.isMatchFinished) {
            toFinished(newMessages = newMessages)
        } else {
            toUpdatedCombat(newMessages = newMessages)
        }
    }

    private fun ArenaMatchEntity.Combat.toFinished(
        newMessages: List<ArenaMessageEntity>
    ): ArenaMatchEntity.Finished =
        ArenaMatchEntity.Finished(
            messages = messages.appendCapped(newMessages = newMessages),
            numberOfNewMessages = newMessages.size,
            playerEntitiesMap = result.updatedPlayerMap.mapValues { it.value.toEntity() },
            matchNumberInGauntlet = matchNumberInGauntlet,
        )

    private fun ArenaMatchEntity.Combat.toUpdatedCombat(
        newMessages: List<ArenaMessageEntity>
    ): ArenaMatchEntity.Combat =
        ArenaMatchEntity.Combat(
            messages = messages.appendCapped(newMessages = newMessages),
            numberOfNewMessages = newMessages.size,
            playerEntitiesMap = result.updatedPlayerMap.mapValues { it.value.toSelectedEntity() },
            nextCombatActionEntity = result.nextCombatAction.toEntity(),
            roundNumber = result.roundNumber,
            isNewRound = result.isNewRound,
            doublesBonusEntity = result.doublesBonus?.toEntity(),
            matchNumberInGauntlet = matchNumberInGauntlet,
        )
}
