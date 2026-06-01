package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.RollOutcome
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

// TODO: Could consolidate some abilities here? Like anytime attacker does damage, try Encore

internal class CombatMutator(
    val attackerUserId: UserId,
    val defenderUserId: UserId,
    val updatedPlayerMap: MutableMap<UserId, ArenaPlayer.Selected>,
    val originalTurnUserId: String,
    private val roundNumber: Int,
    private val messages: MutableList<OutgoingArenaMessage> = mutableListOf(),
) {
    private lateinit var nextCombatAction: NextCombatAction
    private var updatedRoundNumber: Int? = null

    val attacker
        get() = updatedPlayerMap.getValue(key = attackerUserId).characterClass

    val defender
        get() = updatedPlayerMap.getValue(key = defenderUserId).characterClass

    val consciousPlayers
        get() = updatedPlayerMap.values.filter { it.characterClass.isConscious() }

    fun setNextCombatAction(nextAction: NextCombatAction) {
        nextCombatAction = nextAction
    }

    fun getOutcome() =
        RollOutcome(
            updatedPlayerMap = updatedPlayerMap,
            messages = messages,
            nextCombatAction = nextCombatAction,
            roundNumber = updatedRoundNumber ?: roundNumber,
            isNewRound = updatedRoundNumber != null,
        )

    fun updatePlayer(userId: UserId, transform: (ArenaPlayer.Selected) -> ArenaPlayer.Selected) {
        val current = updatedPlayerMap.getValue(key = userId)
        updatedPlayerMap[userId] = transform(current)
    }

    fun updatePlayerClass(userId: UserId, transform: (CharacterClass) -> CharacterClass) {
        updatePlayer(userId = userId) { player ->
            player.copy(characterClass = transform(player.characterClass))
        }
    }

    fun updateAttackerClass(transform: (CharacterClass) -> CharacterClass) {
        updatePlayer(userId = attackerUserId) { player ->
            player.copy(characterClass = transform(player.characterClass))
        }
    }

    fun updateDefenderClass(transform: (CharacterClass) -> CharacterClass) {
        updatePlayer(userId = defenderUserId) { player ->
            player.copy(characterClass = transform(player.characterClass))
        }
    }

    fun updateDefenderHitPoints(hitPoints: Int) {
        updateDefenderClass { defender -> defender.copy(hitPoints = hitPoints) }
    }

    fun addMessage(message: OutgoingArenaMessage) {
        messages.add(element = message)
    }

    fun resolveTurnEnd() {
        updatePlayerClass(userId = originalTurnUserId) { it.copy(hasActedThisRound = true) }

        // TODO: Refactor this - get next player, etc.
        val nextPlayer = calculateNextPlayerInRound()

        if (nextPlayer == null) {
            val newRoundNumber = roundNumber + 1
            updatedRoundNumber = newRoundNumber

            resetAllPlayersActed()
            messages.add(element = OutgoingArenaMessage.NewRound(roundNumber = newRoundNumber))

            val fastestNextRound = calculateFastestLivingPlayer()
            fastestNextRound.characterClass.addNewTurnMessage()

            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserId = fastestNextRound.userId)
            )
            return
        }

        nextPlayer.characterClass.addNewTurnMessage()

        setNextCombatAction(nextAction = NextCombatAction.Idle(activeUserId = nextPlayer.userId))
    }

    private fun calculateNextPlayerInRound(): ArenaPlayer.Selected? {
        val livingPlayers = updatedPlayerMap.values.filter { it.characterClass.hitPoints > 0 }
        val candidates = livingPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun calculateFastestLivingPlayer(): ArenaPlayer.Selected {
        val livingPlayers = updatedPlayerMap.values.filter { it.characterClass.hitPoints > 0 }

        return livingPlayers.maxBy { it.characterClass.speed }
    }

    private fun resetAllPlayersActed() {
        updatedPlayerMap.keys.forEach { userId ->
            updatePlayerClass(userId) { characterClass ->

                // TODO: Move this to MirrorImage updater thingy itself
                val updatedDefensiveAbility =
                    when (val defensiveAbility = characterClass.defensiveAbility) {
                        is DefensiveAbility.MirrorImage ->
                            defensiveAbility.copy(usedThisAttack = false)

                        else -> defensiveAbility
                    }

                characterClass.copy(
                    hasActedThisRound = false,
                    defensiveAbility = updatedDefensiveAbility,
                )
            }
        }
    }

    private fun CharacterClass.addNewTurnMessage() {
        val characterClassInfo = CharacterClassMessageInfo(className = className, type = type)
        messages.add(
            element = OutgoingArenaMessage.NewTurn(characterClassInfo = characterClassInfo)
        )
    }
}
