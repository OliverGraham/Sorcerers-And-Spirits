package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

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
        nextAction.resetMirrorImageIfApplicable()

        nextCombatAction = nextAction
    }

    fun getOutcome() =
        ActionOutcome(
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

        val nextPlayer = calculateNextPlayerInRound()
        if (nextPlayer != null) {
            nextPlayer.characterClass.addNewTurnMessage()
            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserIds = setOf(nextPlayer.userId))
            )
        } else {
            goToNextRound()
        }
    }

    private fun goToNextRound() {
        val newRoundNumber = roundNumber + 1
        updatedRoundNumber = newRoundNumber

        resetAllPlayersActed()
        messages.add(element = OutgoingArenaMessage.NewRound(roundNumber = newRoundNumber))

        val fastestPlayerNextRound = consciousPlayers.maxBy { it.characterClass.speed }
        fastestPlayerNextRound.characterClass.addNewTurnMessage()

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(fastestPlayerNextRound.userId))
        )
    }

    private fun calculateNextPlayerInRound(): ArenaPlayer.Selected? {
        val candidates = consciousPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun resetAllPlayersActed() {
        consciousPlayers.forEach { player ->
            updatePlayerClass(userId = player.userId) { characterClass ->
                characterClass.copy(hasActedThisRound = false)
            }
        }
    }

    /**
     * Resets Mirror Image unless the next action is the Health roll of the same attack.
     *
     * Mirror Image can be used once across the full attack attempt against Illusionist: Armor roll
     * first, then Health roll only if armor passes.
     *
     * So AC -> Health must preserve `usedThisAttack`. Any other transition means that attack
     * attempt ended or a new one is starting, so Mirror Image becomes available again.
     */
    private fun NextCombatAction.resetMirrorImageIfApplicable() {
        if (this is NextCombatAction.NormalAttack && this.roll is NormalAttackRoll.Health) return

        val illusionistPlayer =
            consciousPlayers.firstOrNull {
                it.characterClass.type == CharacterClassType.Illusionist
            } ?: return

        val mirrorImage =
            illusionistPlayer.characterClass.defensiveAbility as? DefensiveAbility.MirrorImage
                ?: return

        updatePlayerClass(userId = illusionistPlayer.userId) { illusionist ->
            illusionist.copy(defensiveAbility = mirrorImage.copy(usedThisAttack = false))
        }
    }

    private fun CharacterClass.addNewTurnMessage() {
        val characterClassInfo = CharacterClassMessageInfo(className = className, type = type)
        messages.add(
            element = OutgoingArenaMessage.NewTurn(characterClassInfo = characterClassInfo)
        )
    }
}
