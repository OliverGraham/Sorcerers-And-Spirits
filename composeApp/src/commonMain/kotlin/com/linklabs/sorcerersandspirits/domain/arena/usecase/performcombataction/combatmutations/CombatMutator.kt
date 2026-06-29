package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated.SummonMinionActivated
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.game.AbilityConfig
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective
import com.linklabs.sorcerersandspirits.domain.game.minionAttacks
import com.linklabs.sorcerersandspirits.domain.game.withEffective
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

internal class CombatMutator(
    val attackerUserId: UserId,
    val defenderUserId: UserId,
    val updatedPlayerMap: MutableMap<UserId, ArenaPlayer.Selected>,
    val originalTurnUserId: String,
    val diceRoller: DiceRoller,
    val abilityConfig: AbilityConfig,
    private val roundNumber: Int,
    private val messages: MutableList<ArenaMessage> = mutableListOf(),
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

    /** The current attack continues: the attacker passed armor and now rolls against Health. */
    fun continueAttackWithHealthRoll() {
        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = NormalAttackType.Health,
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    originalTurnUserId = originalTurnUserId,
                    activeUserIds = setOf(element = attackerUserId),
                )
        )
    }

    /**
     * Sets the next action to an ability roll, carrying this action's combatant ids forward.
     * [defenderUserId] can be overridden when the ability owner is not the current defender (e.g.
     * Holy Shield triggered by Spell Potency splash damage).
     */
    fun setNextAbilityRoll(
        purpose: AbilityAction,
        activeUserIds: Set<UserId>,
        defenderUserId: UserId = this.defenderUserId,
    ) {
        setNextCombatAction(
            nextAction =
                NextCombatAction.AbilityRoll(
                    purpose = purpose,
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    originalTurnUserId = originalTurnUserId,
                    activeUserIds = activeUserIds,
                )
        )
    }

    fun getOutcome() =
        ActionOutcome(
            updatedPlayerMap = updatedPlayerMap,
            messages = messages,
            nextCombatAction = nextCombatAction,
            roundNumber = updatedRoundNumber ?: roundNumber,
            isNewRound = updatedRoundNumber != null,
            doublesBonus = diceRoller.getDoublesBonus(),
            isMatchFinished = getIsMatchFinished(),
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

    fun addMessage(message: ArenaMessage) {
        messages.add(element = message)
    }

    fun resolveTurnEnd() {
        if (consciousPlayers.size <= 1) {
            setNextCombatAction(nextAction = NextCombatAction.Idle(activeUserIds = emptySet()))
            return
        }

        val wasSecondAttackGranted = grantAllInSecondAttackIfApplicable()
        if (wasSecondAttackGranted) return

        val wasMinionAttackGranted = grantMinionAttackIfApplicable()
        if (wasMinionAttackGranted) return

        updatePlayerClass(userId = originalTurnUserId) { it.copy(hasActedThisRound = true) }

        val nextPlayer = calculateNextPlayerInRound()
        if (nextPlayer != null) {
            resetMinionAttacks(userId = nextPlayer.userId)
            nextPlayer.characterClass.addNewTurnMessage()
            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserIds = setOf(nextPlayer.userId))
            )
        } else {
            goToNextRound()
        }
    }

    /**
     * A Gambler whose All-In succeeded attacks twice this round. The second attack is granted here,
     * once, after the first attack resolves: control stays with the Gambler instead of advancing,
     * and the flag is cleared so the turn ends normally after that second attack.
     */
    private fun grantAllInSecondAttackIfApplicable(): Boolean {
        val turnPlayer = updatedPlayerMap.getValue(key = originalTurnUserId).characterClass
        val allIn = turnPlayer.offensiveAbility.effective as? OffensiveAbility.AllIn ?: return false
        if (!allIn.hasAnotherAttack) return false
        if (!turnPlayer.isConscious()) return false

        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            val clearedAllIn = allIn.copy(hasAnotherAttack = false)
            characterClass.copy(
                offensiveAbility = characterClass.offensiveAbility.withEffective(clearedAllIn)
            )
        }
        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(originalTurnUserId))
        )

        return true
    }

    /**
     * A summoner attacks once per minion they hold, on top of their normal attack. The follow-up
     * attacks are granted here, one at a time after each attack resolves: control stays with the
     * summoner and the taken-count is raised, until it reaches the live minion count and the turn
     * ends. The count is read live so a minion consumed as a shield this turn no longer grants its
     * attack. [SummonMinionActivated] marks the count spent when the turn is used to summon, so a
     * minion first grants its attack on a later turn. Minions can live inside an Illusionist's
     * Impersonate wrapper, so the effective ability is read and written back wrapped.
     */
    private fun grantMinionAttackIfApplicable(): Boolean {
        val turnPlayer = updatedPlayerMap.getValue(key = originalTurnUserId).characterClass
        val minions =
            turnPlayer.offensiveAbility.effective as? OffensiveAbility.SummonMinion ?: return false
        if (minions.attacksTakenThisTurn >= minions.minionAttacks) return false
        if (!turnPlayer.isConscious()) return false

        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            val updatedMinions =
                minions.copy(attacksTakenThisTurn = minions.attacksTakenThisTurn + 1)
            characterClass.copy(
                offensiveAbility =
                    characterClass.offensiveAbility.withEffective(updated = updatedMinions)
            )
        }
        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(element = originalTurnUserId))
        )

        return true
    }

    /**
     * Clears a summoner's minion-attack count as their turn begins, so they may attack once per
     * minion again. A minion summoned later in the same turn is not counted until the next turn,
     * because summoning spends the count for the summon turn.
     */
    private fun resetMinionAttacks(userId: UserId) {
        val player = updatedPlayerMap.getValue(key = userId).characterClass
        val minions = player.offensiveAbility.effective as? OffensiveAbility.SummonMinion ?: return

        updatePlayerClass(userId = userId) { characterClass ->
            val resetMinions = minions.copy(attacksTakenThisTurn = 0)
            characterClass.copy(
                offensiveAbility = characterClass.offensiveAbility.withEffective(resetMinions)
            )
        }
    }

    private fun goToNextRound() {
        val newRoundNumber = roundNumber + 1
        updatedRoundNumber = newRoundNumber

        resetPlayersForNewRound()
        messages.add(element = ArenaMessage.NewRound(roundNumber = newRoundNumber))

        val fastestPlayerNextRound = consciousPlayers.maxBy { it.characterClass.speed }
        resetMinionAttacks(userId = fastestPlayerNextRound.userId)
        fastestPlayerNextRound.characterClass.addNewTurnMessage()

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(fastestPlayerNextRound.userId))
        )
    }

    private fun calculateNextPlayerInRound(): ArenaPlayer.Selected? {
        val candidates = consciousPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun resetPlayersForNewRound() {
        consciousPlayers.forEach { player ->
            updatePlayerClass(userId = player.userId) { characterClass ->
                characterClass.copy(
                    hasActedThisRound = false,
                    offensiveAbility = characterClass.offensiveAbility.resetForNewTurn(),
                )
            }
        }
    }

    /**
     * Clears per-turn ability state so a new turn starts clean. All-In's [usedThisTurn] guards it
     * to once per turn, so it must be released when the owner's next turn begins — including when
     * an Illusionist is using a copied All-In, where it lives inside the Impersonate wrapper.
     */
    private fun OffensiveAbility.resetForNewTurn(): OffensiveAbility {
        val allIn = effective as? OffensiveAbility.AllIn ?: return this
        return withEffective(allIn.copy(usedThisTurn = false))
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
        if (this is NextCombatAction.NormalAttack && this.roll == NormalAttackType.Health) return

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
        messages.add(element = ArenaMessage.NewTurn(characterClassInfo = characterClassInfo))
    }

    private fun getIsMatchFinished(): Boolean {
        if (consciousPlayers.size > 1) return false
        if (!::nextCombatAction.isInitialized) return false

        val anyActiveUnconsciousPlayers =
            nextCombatAction.activeUserIds.any { userId ->
                !updatedPlayerMap.getValue(key = userId).characterClass.isConscious()
            }

        return !anyActiveUnconsciousPlayers
    }
}
