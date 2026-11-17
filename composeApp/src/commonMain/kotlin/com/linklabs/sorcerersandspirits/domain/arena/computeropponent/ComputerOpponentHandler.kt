package com.linklabs.sorcerersandspirits.domain.arena.computeropponent

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfigProvider
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.effective
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

const val COMPUTER_OPPONENT_TURN_DELAY_MILLIS = 3000L

sealed interface ComputerOpponentDecision {
    data class ChooseClass(val userId: String, val characterClass: CharacterClass) : ComputerOpponentDecision

    data class Attack(val targetUserId: String) : ComputerOpponentDecision

    data class UseAbility(
        val abilityAction: AbilityAction,
        val targetUserId: String,
    ) : ComputerOpponentDecision
}

class ComputerOpponentHandler(
    private val computerOpponentIdentity: ComputerOpponentIdentity,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    abilityConfigProvider: AbilityConfigProvider,
) {
    private val localArenaMatch = LocalArenaMatch(abilityConfigProvider = abilityConfigProvider)
    private val computerUserId = computerOpponentIdentity.userId

    val matchFlow: StateFlow<ArenaMatch?> = localArenaMatch.matchFlow

    fun start(userDisplayName: String, userId: String) {
        localArenaMatch.start(
            userPlayer = ArenaPlayer.Unselected(userId = userId, displayName = userDisplayName),
            computerPlayer =
            ArenaPlayer.Unselected(
                userId = computerUserId,
                displayName = computerOpponentIdentity.displayName,
            ),
        )
    }

    suspend fun chooseCharacterClass(
        selectedArenaPlayer: ArenaPlayer.Selected,
        allPlayers: List<ArenaPlayer>,
    ) {
        localArenaMatch.chooseCharacterClass(
            selectedArenaPlayer = selectedArenaPlayer,
            allPlayers = allPlayers,
        )
    }

    suspend fun performCombatAction(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        localArenaMatch.performCombatAction(arguments = arguments, currentAction = currentAction)
    }

    fun finishMatch() = localArenaMatch.finishMatch()

    fun stayForAnotherMatch() = localArenaMatch.stayForAnotherMatch()

    fun leave() = localArenaMatch.leave()

    suspend fun decideTurn(): ComputerOpponentDecision? {
        if (matchFlow.value?.isComputerOwed() != true) return null

        delay(timeMillis = COMPUTER_OPPONENT_TURN_DELAY_MILLIS)

        return when (val match = matchFlow.value) {
            is ArenaMatch.ChoosingClasses -> match.decideClassChoice()

            is ArenaMatch.Combat -> match.decideCombatAction()

            is ArenaMatch.WaitingForAllPlayers,
            is ArenaMatch.Finished,
            null,
            -> null
        }
    }

    private fun ArenaMatch.isComputerOwed(): Boolean = when (this) {
        is ArenaMatch.ChoosingClasses -> activePlayerUserId == computerUserId

        is ArenaMatch.Combat ->
            !isMatchOver &&
                computerUserId in nextCombatAction.activeUserIds &&
                nextCombatAction.isComputerOwedRoll()

        is ArenaMatch.WaitingForAllPlayers,
        is ArenaMatch.Finished,
        -> false
    }

    private fun NextCombatAction.isComputerOwedRoll(): Boolean {
        val teleport = (this as? NextCombatAction.AbilityRoll)?.purpose as? AbilityAction.Passive.Teleport
            ?: return true

        val isAttackerPending = teleport.attackerRoll.userId == computerUserId && teleport.attackerRoll.roll == null
        val isDefenderPending = teleport.defenderRoll.userId == computerUserId && teleport.defenderRoll.roll == null
        return isAttackerPending || isDefenderPending
    }

    private fun ArenaMatch.ChoosingClasses.decideClassChoice(): ComputerOpponentDecision? {
        val takenClassTypes =
            playerMap.values.filterIsInstance<ArenaPlayer.Selected>().map { it.characterClass.type }
        val availableClass =
            getCharacterClassesUseCase().filterNot { it.type in takenClassTypes }.randomOrNull()
                ?: return null

        return ComputerOpponentDecision.ChooseClass(
            userId = computerUserId,
            characterClass = availableClass,
        )
    }

    private fun ArenaMatch.Combat.decideCombatAction(): ComputerOpponentDecision? = when (val action = nextCombatAction) {
        is NextCombatAction.Idle -> decideIdleTurn()

        is NextCombatAction.NormalAttack ->
            ComputerOpponentDecision.Attack(targetUserId = action.defenderUserId)

        is NextCombatAction.AbilityRoll ->
            ComputerOpponentDecision.UseAbility(
                abilityAction = action.purpose.rerollingHigherMirrorImageDie(),
                targetUserId = charmTargetOrEmpty(abilityAction = action.purpose),
            )
    }

    private fun ArenaMatch.Combat.decideIdleTurn(): ComputerOpponentDecision? {
        val attackDecision =
            randomLivingOpponentUserId()?.let { ComputerOpponentDecision.Attack(targetUserId = it) }

        val computerOffensiveAbility =
            playerMap.getValue(key = computerUserId).characterClass.offensiveAbility
        val activatedDecision =
            if (isMinionAttackInProgress(offensiveAbility = computerOffensiveAbility)) {
                null
            } else {
                activatedAbilityDecision(offensiveAbility = computerOffensiveAbility)
            }

        return listOfNotNull(attackDecision, activatedDecision).randomOrNull()
    }

    private fun ArenaMatch.Combat.activatedAbilityDecision(
        offensiveAbility: OffensiveAbility,
    ): ComputerOpponentDecision.UseAbility? = when (offensiveAbility) {
        is OffensiveAbility.AllIn ->
            if (offensiveAbility.usedThisTurn) {
                null
            } else {
                ComputerOpponentDecision.UseAbility(
                    abilityAction = AbilityAction.Activated.AllIn(allInAbility = offensiveAbility),
                    targetUserId = "",
                )
            }

        is OffensiveAbility.SummonMinion ->
            ComputerOpponentDecision.UseAbility(
                abilityAction = AbilityAction.Activated.SummonMinion(summonMinionAbility = offensiveAbility),
                targetUserId = "",
            )

        is OffensiveAbility.PrepareAttack ->
            randomLivingOpponentUserId()?.let { targetUserId ->
                ComputerOpponentDecision.UseAbility(
                    abilityAction = AbilityAction.Activated.PrepareAttack,
                    targetUserId = targetUserId,
                )
            }

        is OffensiveAbility.Impersonate ->
            randomImpersonationTargetUserId(impersonateAbility = offensiveAbility)?.let { targetUserId ->
                ComputerOpponentDecision.UseAbility(
                    abilityAction =
                    AbilityAction.Activated.Impersonate(
                        impersonateAbility = OffensiveAbility.Impersonate(),
                    ),
                    targetUserId = targetUserId,
                )
            }

        is OffensiveAbility.KillingMomentum,
        is OffensiveAbility.Encore,
        is OffensiveAbility.SpellPotency,
        is OffensiveAbility.None,
        -> null
    }

    private fun isMinionAttackInProgress(offensiveAbility: OffensiveAbility): Boolean {
        val summonMinion = offensiveAbility.effective as? OffensiveAbility.SummonMinion ?: return false
        return summonMinion.minions.any { it.hasAttackedThisRound }
    }

    private fun ArenaMatch.Combat.charmTargetOrEmpty(abilityAction: AbilityAction): String = (abilityAction as? AbilityAction.Passive.Persuade.Charm)
        ?.targetableUserIds
        ?.randomOrNull()
        .orEmpty()

    private fun ArenaMatch.Combat.randomLivingOpponentUserId(): String? = playerMap.values
        .filter { player ->
            player.userId != computerUserId && player.characterClass.hitPoints > 0
        }
        .randomOrNull()
        ?.userId

    private fun ArenaMatch.Combat.randomImpersonationTargetUserId(
        impersonateAbility: OffensiveAbility.Impersonate,
    ): String? = playerMap.values
        .filter { player ->
            val isLivingOpponent =
                player.userId != computerUserId && player.characterClass.hitPoints > 0
            val isRedundant =
                isImpersonateRedundant(
                    targetOffensiveAbility = player.characterClass.offensiveAbility,
                    copiedAbility = impersonateAbility.impersonatedAbility,
                )
            isLivingOpponent && !isRedundant
        }
        .randomOrNull()
        ?.userId

    private fun isImpersonateRedundant(
        targetOffensiveAbility: OffensiveAbility,
        copiedAbility: OffensiveAbility,
    ): Boolean {
        if (targetOffensiveAbility is OffensiveAbility.None) return true
        if (copiedAbility is OffensiveAbility.None) return false
        return targetOffensiveAbility::class == copiedAbility::class
    }
}

internal fun AbilityAction.rerollingHigherMirrorImageDie(): AbilityAction {
    if (this !is AbilityAction.Passive.MirrorImage) return this

    val isFirstDieHigher = firstDie.originalRoll >= secondDie.originalRoll
    val higherDieSelection =
        if (isFirstDieHigher) MirrorImageRerollSelection.First else MirrorImageRerollSelection.Second

    return copy(mirrorImageRerollSelection = higherDieSelection)
}
