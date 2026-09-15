package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.toDieColor
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.effective
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.getSummonerDamagedResult
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.recordMinionAttack
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.resetMinionAttacks
import com.linklabs.sorcerersandspirits.domain.game.ability.withEffective
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

internal data class AttackingMinion(val index: Int, val type: MinionType, val name: String)

internal class CombatMutator(
    val attackerUserId: UserId,
    val defenderUserId: UserId,
    val rollingUserId: UserId,
    val updatedPlayerMap: MutableMap<UserId, ArenaPlayer.Selected>,
    val originalTurnUserId: String,
    val diceRoller: DiceRoller,
    val abilityConfig: AbilityConfig,
    private val roundNumber: Int,
    private val messages: MutableList<ArenaMessage> = mutableListOf(),
) {
    private lateinit var nextCombatAction: NextCombatAction
    private var updatedRoundNumber: Int? = null

    // TODO: Use PersuadeHandler - don't put class-wide state here
    private var isCurrentTargetRedirected = false

    val attacker
        get() = updatedPlayerMap.getValue(key = attackerUserId).characterClass

    val defender
        get() = updatedPlayerMap.getValue(key = defenderUserId).characterClass

    val consciousPlayers
        get() = updatedPlayerMap.values.filter { it.characterClass.isConscious() }

    fun dieColorFor(userId: UserId): DieColor = updatedPlayerMap.getValue(key = userId).characterClass.type.toDieColor()

    fun setNextCombatAction(nextAction: NextCombatAction) {
        nextCombatAction = nextAction
    }

    fun redirectCurrentTarget() {
        isCurrentTargetRedirected = true
    }

    fun currentAttackTurn(): AttackTurn = attackTurnFor(attackerUserId = attackerUserId)

    fun attackingMinionOrNull(): AttackingMinion? {
        if (attackerUserId != originalTurnUserId) return null
        val summonMinionAbility = attacker.getSummonMinionOrNull() ?: return null
        val attackingMinionIndex = summonMinionAbility.minions.indexOfFirst { minion -> !minion.hasAttackedThisRound }
        if (attackingMinionIndex < 0) return null

        val attackingMinion = summonMinionAbility.minions[attackingMinionIndex]

        return AttackingMinion(
            index = attackingMinionIndex,
            type = attackingMinion.type,
            name = attackingMinion.name,
        )
    }

    fun attackTurnFor(attackerUserId: UserId): AttackTurn = if (attackerUserId ==
        originalTurnUserId
    ) {
        AttackTurn.Standard(attackerUserId = attackerUserId)
    } else {
        AttackTurn.RiposteReversal(
            attackerUserId = attackerUserId,
            turnOwnerUserId = originalTurnUserId,
        )
    }

    fun continueAttackWithHealthRoll() {
        setNextCombatAction(
            nextAction =
            NextCombatAction.NormalAttack(
                roll = NormalAttackType.Health,
                attackTurn = currentAttackTurn(),
                defenderUserId = defenderUserId,
                activeUserIds = setOf(element = attackerUserId),
            ),
        )
    }

    fun setNextAbilityRoll(
        purpose: AbilityAction,
        activeUserIds: Set<UserId>,
        defenderUserId: UserId = this.defenderUserId,
        attackTurn: AttackTurn = currentAttackTurn(),
    ) {
        setNextCombatAction(
            nextAction =
            NextCombatAction.AbilityRoll(
                purpose = purpose,
                attackTurn = attackTurn,
                defenderUserId = defenderUserId,
                activeUserIds = activeUserIds,
            ),
        )
    }

    fun getOutcome() = ActionOutcome(
        updatedPlayerMap = updatedPlayerMap,
        messages = messages,
        nextCombatAction = nextCombatAction,
        currentTargetUserId =
        if (isCurrentTargetRedirected) null else defenderUserId.takeIf { it != attackerUserId },
        roundNumber = updatedRoundNumber ?: roundNumber,
        isNewRound = updatedRoundNumber != null,
        isMatchFinished = getIsMatchFinished(),
        diceRoll = diceRoller.getDiceRoll(),
    )

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

    // TODO: Refactor takeDamage() for Summoners
    fun takeDamage(damage: Int, userId: String): Int {
        val playerClass = updatedPlayerMap.getValue(key = userId).characterClass
        val summonMinion = playerClass.offensiveAbility.effective as? OffensiveAbility.SummonMinion

        if (summonMinion != null) {
            return handleSummonerDamaged(
                damage = damage,
                userId = userId,
                summonMinion = summonMinion,
                summonerClassName = playerClass.className,
                summonerClassType = playerClass.type,
            )
        }

        updatePlayerClass(userId = userId) { currentClass ->
            currentClass.copy(hitPoints = currentClass.hitPoints - damage)
        }

        return damage
    }

    // TODO: Move this somewhere else? - should just be part of takeDamage() - they're always called
    //  together
    fun hasAbsorbingMinions(userId: String): Boolean {
        val summonMinion =
            updatedPlayerMap.getValue(key = userId)
                .characterClass.offensiveAbility.effective as? OffensiveAbility.SummonMinion
        return summonMinion != null && summonMinion.minions.isNotEmpty()
    }

    // TODO: Move this somewhere else?
    fun addMinionShieldResidualDamage(summonerUserId: String, damageAfterMinionShield: Int) {
        val summoner = updatedPlayerMap.getValue(key = summonerUserId).characterClass
        addMessage(
            message =
            ArenaMessage.NormalRoll.Outcome(
                attackerUserId = attackerUserId,
                defenderUserId = summonerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                minionAttackerName = null,
                defenderClassName = summoner.className,
                defenderClassType = summoner.type,
                defenderArmorClass = summoner.armorClass,
                firstDie = 0,
                secondDie = 0,
                bonus = 0,
                type = NormalRollMessageType.Damage,
                damageAfterMinionShield = damageAfterMinionShield,
            ),
        )
    }

    fun wouldBeKnockedOut(damage: Int, userId: String): Boolean {
        val playerClass = updatedPlayerMap.getValue(key = userId).characterClass
        val summonMinion = playerClass.offensiveAbility.effective as? OffensiveAbility.SummonMinion
        val damageToPlayer = if (summonMinion != null) {
            damage - summonMinion.minions.sumOf { minion -> minion.hitPoints }
        } else {
            damage
        }

        return playerClass.hitPoints - damageToPlayer <= 0
    }

    fun spendTurnOwnerAttack() {
        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            characterClass.copy(
                attacksRemaining =
                (characterClass.attacksRemaining - 1).coerceAtLeast(minimumValue = 0),
            )
        }
    }

    fun grantBonusAttack() {
        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            characterClass.copy(attacksRemaining = characterClass.attacksRemaining + 1)
        }
    }

    fun addMessage(message: ArenaMessage) {
        messages.add(element = message)
    }

    fun resolveTurnEnd() {
        if (consciousPlayers.size <= 1) {
            setNextCombatAction(
                nextAction =
                NextCombatAction.Idle(activeUserIds = setOf(element = originalTurnUserId)),
            )
            return
        }

        resetMirrorImageIfApplicable()

        val wasSecondAttackGranted = grantAllInSecondAttackIfApplicable()
        if (wasSecondAttackGranted) return

        val wasMinionAttackGranted = grantMinionAttackIfApplicable()
        if (wasMinionAttackGranted) return

        updatePlayerClass(userId = originalTurnUserId) { it.copy(hasActedThisRound = true) }

        val nextPlayer = calculateNextPlayerInRound()
        if (nextPlayer != null) {
            nextPlayer.characterClass.addNewTurnMessage()
            setNextCombatAction(
                nextAction = NextCombatAction.Idle(activeUserIds = setOf(nextPlayer.userId)),
            )
        } else {
            goToNextRound()
        }
    }

    private fun updatePlayer(userId: UserId, transform: (ArenaPlayer.Selected) -> ArenaPlayer.Selected) {
        val current = updatedPlayerMap.getValue(key = userId)
        updatedPlayerMap[userId] = transform(current)
    }

    private fun grantAllInSecondAttackIfApplicable(): Boolean {
        val turnPlayer = updatedPlayerMap.getValue(key = originalTurnUserId).characterClass
        val allIn = turnPlayer.offensiveAbility.effective as? OffensiveAbility.AllIn ?: return false
        if (!allIn.hasAnotherAttack) return false
        if (!turnPlayer.isConscious()) return false

        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            val clearedAllIn = allIn.copy(hasAnotherAttack = false)
            characterClass.copy(
                offensiveAbility = characterClass.offensiveAbility.withEffective(clearedAllIn),
            )
        }
        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(originalTurnUserId)),
        )

        return true
    }

    private fun grantMinionAttackIfApplicable(): Boolean {
        val turnPlayer = updatedPlayerMap.getValue(key = originalTurnUserId).characterClass
        val summonMinion =
            turnPlayer.getSummonMinionOrNull() ?: return false
        if (summonMinion.usedThisTurn) return false
        if (summonMinion.minions.all { it.hasAttackedThisRound }) return false
        if (!turnPlayer.isConscious()) return false

        updatePlayerClass(userId = originalTurnUserId) { characterClass ->
            characterClass.copy(
                offensiveAbility =
                characterClass.offensiveAbility.withEffective(updated = summonMinion.recordMinionAttack()),
            )
        }
        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(element = originalTurnUserId)),
        )

        return true
    }

    private fun goToNextRound() {
        val newRoundNumber = roundNumber + 1
        updatedRoundNumber = newRoundNumber

        resetPlayersForNewRound()
        messages.add(element = ArenaMessage.NewRound(roundNumber = newRoundNumber))

        val fastestPlayerNextRound = consciousPlayers.maxBy { it.characterClass.speed }
        fastestPlayerNextRound.characterClass.addNewTurnMessage()

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(
                activeUserIds = setOf(fastestPlayerNextRound.userId),
            ),
        )
    }

    private fun calculateNextPlayerInRound(): ArenaPlayer.Selected? {
        val candidates = consciousPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun resetPlayersForNewRound() {
        consciousPlayers.forEach { player ->
            val minionAttacks = player.getMinionAttacks()

            updatePlayerClass(userId = player.userId) { characterClass ->
                characterClass.copy(
                    hasActedThisRound = false,
                    attacksRemaining = 1 + minionAttacks,
                    offensiveAbility = characterClass.offensiveAbility.resetForNewTurn(),
                )
            }
        }
    }

    private fun ArenaPlayer.Selected.getMinionAttacks(): Int {
        val summonMinion = characterClass.getSummonMinionOrNull() ?: return 0

        return summonMinion.minions.size
    }

    private fun OffensiveAbility.resetForNewTurn(): OffensiveAbility = when (val effectiveAbility = effective) {
        is OffensiveAbility.AllIn -> withEffective(
            updated = effectiveAbility.copy(usedThisTurn = false),
        )

        is OffensiveAbility.SummonMinion -> withEffective(
            updated = effectiveAbility.resetMinionAttacks(),
        )

        else -> this
    }

    private fun resetMirrorImageIfApplicable() {
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
        if (nextCombatAction is NextCombatAction.Idle) return true

        val anyActiveUnconsciousPlayers =
            nextCombatAction.activeUserIds.any { userId ->
                !updatedPlayerMap.getValue(key = userId).characterClass.isConscious()
            }

        return !anyActiveUnconsciousPlayers
    }

    private fun handleSummonerDamaged(
        damage: Int,
        userId: String,
        summonMinion: OffensiveAbility.SummonMinion,
        summonerClassName: String,
        summonerClassType: CharacterClassType,
    ): Int {
        val summonerDamagedResult = summonMinion.getSummonerDamagedResult(
            damage = damage,
            summonerUserId = userId,
            summonerClassName = summonerClassName,
            summonerClassType = summonerClassType,
        )

        messages.addAll(elements = summonerDamagedResult.minionMessages)

        updatePlayerClass(userId = userId) { currentClass ->
            currentClass.copy(
                hitPoints = currentClass.hitPoints - summonerDamagedResult.damageToSummoner,
                offensiveAbility = summonerDamagedResult.updatedSummonMinion,
            )
        }

        return summonerDamagedResult.damageToSummoner
    }

    private fun CharacterClass.getSummonMinionOrNull() = offensiveAbility.effective as? OffensiveAbility.SummonMinion

    fun livingMinionNames(): Set<String> = consciousPlayers
        .flatMap { player -> player.characterClass.getSummonMinionOrNull()?.minions.orEmpty() }
        .map { minion -> minion.name }
        .toSet()
}
