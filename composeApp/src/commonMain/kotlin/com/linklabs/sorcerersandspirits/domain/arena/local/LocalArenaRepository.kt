package com.linklabs.sorcerersandspirits.domain.arena.local

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ParticipantStatus
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RealDiceRoller
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfigProvider
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalArenaRepository(private val abilityConfigProvider: AbilityConfigProvider) {

    private val _matchFlow = MutableStateFlow<ArenaMatch?>(value = null)
    val matchFlow: StateFlow<ArenaMatch?> = _matchFlow.asStateFlow()

    private var messageIdCounter = 0L

    fun createComputerOpponentMatch(
        userPlayer: ArenaPlayer.Unselected,
        computerPlayer: ArenaPlayer.Unselected,
    ) {
        messageIdCounter = 0L

        _matchFlow.value =
            ArenaMatch.ChoosingClasses(
                messages =
                listOf(
                    ArenaMessage.ConnectedToArena(displayName = userPlayer.displayName),
                    ArenaMessage.ConnectedToArena(displayName = computerPlayer.displayName),
                )
                    .toIncomingMessages(),
                playerMap =
                mapOf(
                    userPlayer.userId to userPlayer,
                    computerPlayer.userId to computerPlayer,
                ),
                activePlayer = userPlayer,
                activePlayerUserId = userPlayer.userId,
            )
    }

    fun chooseCharacterClass(
        selectedArenaPlayer: ArenaPlayer.Selected,
        allPlayers: List<ArenaPlayer>,
    ) {
        _matchFlow.update { match ->
            if (match !is ArenaMatch.ChoosingClasses) return@update match
            match.withChosenClass(
                selectedArenaPlayer = selectedArenaPlayer,
                nextPlayerId = allPlayers.nextChoosingPlayerId(),
            )
        }
    }

    suspend fun performCombatAction(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        val diceRoller = RealDiceRoller(initialBonus = arguments.doublesBonus)
        val outcome =
            PerformCombatAction(
                arguments = arguments,
                diceRoller = diceRoller,
                abilityConfig = abilityConfigProvider.getAbilityConfig(),
            )
                .invoke(currentAction = currentAction)

        _matchFlow.update { match ->
            if (match !is ArenaMatch.Combat) return@update match
            match.withCombatResult(result = outcome)
        }
    }

    fun stayForAnotherMatch() {
        _matchFlow.update { match ->
            if (match !is ArenaMatch.Finished) return@update match
            match.startNextMatchOrGauntlet()
        }
    }

    fun finishMatch() {
        _matchFlow.update { match ->
            if (match !is ArenaMatch.Combat) return@update match
            if (!match.isMatchOver) return@update match

            ArenaMatch.Finished(
                messages = emptyList(),
                playerMap = match.playerMap,
                matchNumberInGauntlet = match.matchNumberInGauntlet,
                participantMap = match.participantMap,
            )
        }
    }

    fun exit() {
        _matchFlow.value = null
    }

    private fun List<ArenaPlayer>.nextChoosingPlayerId(): String {
        val unselected = filterIsInstance<ArenaPlayer.Unselected>()

        return if (unselected.isNotEmpty()) {
            unselected.random().userId
        } else {
            filterIsInstance<ArenaPlayer.Selected>().maxBy { it.characterClass.speed }.userId
        }
    }

    private fun ArenaMatch.ChoosingClasses.withChosenClass(
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    ): ArenaMatch {
        val updatedPlayers = playerMap + (selectedArenaPlayer.userId to selectedArenaPlayer)
        val areAllSelected = updatedPlayers.values.all { it is ArenaPlayer.Selected }

        val selectionMessage =
            ArenaMessage.ChoseCharacterClass(
                displayName = selectedArenaPlayer.displayName,
                characterClassInfo = selectedArenaPlayer.characterClass.toMessageInfo(),
            )

        if (!areAllSelected) {
            return copy(
                playerMap = updatedPlayers,
                activePlayer = updatedPlayers.getValue(key = nextPlayerId),
                activePlayerUserId = nextPlayerId,
                messages = listOf(selectionMessage).toIncomingMessages(),
            )
        }

        return updatedPlayers
            .mapValues { it.value as ArenaPlayer.Selected }
            .toInitialCombat(
                matchNumberInGauntlet = matchNumberInGauntlet,
                openingMessages = listOf(selectionMessage),
            )
    }

    private fun ArenaMatch.Combat.withCombatResult(result: ActionOutcome): ArenaMatch.Combat = ArenaMatch.Combat(
        messages = result.messages.toIncomingMessages(),
        playerMap = result.updatedPlayerMap,
        matchNumberInGauntlet = matchNumberInGauntlet,
        nextCombatAction = mergedTeleportOrNull(result = result) ?: result.nextCombatAction,
        currentTargetUserId = result.currentTargetUserId,
        roundNumber = result.roundNumber,
        isNewRound = result.isNewRound,
        isMatchOver = result.isMatchFinished,
        diceRoll = result.diceRoll,
        participantMap = result.participantMap,
    )

    private fun ArenaMatch.Combat.mergedTeleportOrNull(result: ActionOutcome): NextCombatAction? {
        val currentTeleport =
            (nextCombatAction as? NextCombatAction.AbilityRoll)?.purpose
                as? AbilityAction.Passive.Teleport ?: return null
        val incomingAction = result.nextCombatAction as? NextCombatAction.AbilityRoll ?: return null
        val incomingTeleport = incomingAction.purpose as? AbilityAction.Passive.Teleport ?: return null

        val isAttackerRollMissing = incomingTeleport.attackerRoll.roll == null
        val isDefenderRollMissing = incomingTeleport.defenderRoll.roll == null
        val isOnlyOneTeleportIn =
            when {
                isAttackerRollMissing && isDefenderRollMissing -> false
                !isAttackerRollMissing && !isDefenderRollMissing -> false
                isAttackerRollMissing -> currentTeleport.attackerRoll.roll != null
                else -> currentTeleport.defenderRoll.roll != null
            }
        if (!isOnlyOneTeleportIn) return null

        val mergedTeleport =
            incomingTeleport.copy(
                attackerRoll =
                incomingTeleport.attackerRoll.copy(
                    roll = incomingTeleport.attackerRoll.roll ?: currentTeleport.attackerRoll.roll,
                ),
                defenderRoll =
                incomingTeleport.defenderRoll.copy(
                    roll = incomingTeleport.defenderRoll.roll ?: currentTeleport.defenderRoll.roll,
                ),
            )

        return incomingAction.copy(purpose = mergedTeleport)
    }

    private fun ArenaMatch.Finished.startNextMatchOrGauntlet(): ArenaMatch {
        val isFinalMatch = matchNumberInGauntlet >= Gauntlet.MATCHES_PER_GAUNTLET

        if (isFinalMatch) {
            val resetPlayers =
                playerMap.mapValues { (_, player) ->
                    ArenaPlayer.Unselected(userId = player.userId, displayName = player.displayName)
                }
            val firstChooser = resetPlayers.values.first()
            return ArenaMatch.ChoosingClasses(
                messages = emptyList(),
                playerMap = resetPlayers,
                activePlayer = firstChooser,
                activePlayerUserId = firstChooser.userId,
            )
        }

        val freshPlayers =
            playerMap.values.filterIsInstance<ArenaPlayer.Selected>().associate { player ->
                player.userId to
                    player.copy(
                        isReadyForNextMatch = false,
                        characterClass = player.characterClass.resetForNewMatch(),
                    )
            }

        return freshPlayers.toInitialCombat(
            matchNumberInGauntlet = matchNumberInGauntlet + 1,
            openingMessages = emptyList(),
        )
    }

    private fun Map<String, ArenaPlayer.Selected>.toInitialCombat(
        matchNumberInGauntlet: Int,
        openingMessages: List<ArenaMessage>,
    ): ArenaMatch.Combat {
        val fastestPlayer = values.maxBy { it.characterClass.speed }

        val combatMessages =
            openingMessages +
                ArenaMessage.NewRound(roundNumber = 1) +
                ArenaMessage.NewTurn(characterClassInfo = fastestPlayer.characterClass.toMessageInfo())

        return ArenaMatch.Combat(
            messages = combatMessages.toIncomingMessages(),
            playerMap = this,
            matchNumberInGauntlet = matchNumberInGauntlet,
            nextCombatAction =
            NextCombatAction.Idle(activeUserIds = setOf(element = fastestPlayer.userId)),
            currentTargetUserId = null,
            roundNumber = 1,
            isNewRound = true,
            isMatchOver = false,
            diceRoll = null,
            participantMap = toInitialParticipantMap(),
        )
    }

    private fun Map<String, ArenaPlayer.Selected>.toInitialParticipantMap(): Map<String, MatchParticipant> = mapValues { (_, player) ->
        MatchParticipant(
            userId = player.userId,
            displayName = player.displayName,
            classType = player.characterClass.type,
            status = ParticipantStatus.Conscious,
        )
    }

    private fun List<ArenaMessage>.toIncomingMessages(): List<IncomingArenaMessage> {
        var currentRoundNumber = 0
        return map { message ->
            if (message is ArenaMessage.NewRound) currentRoundNumber = message.roundNumber
            IncomingArenaMessage(
                id = "local-message-${messageIdCounter++}",
                roundNumber = currentRoundNumber,
                message = message,
            )
        }
    }
}

private fun CharacterClass.toMessageInfo() = CharacterClassMessageInfo(className = className, type = type)

private fun CharacterClass.resetForNewMatch(): CharacterClass {
    val dodgeArmorBonus = (defensiveAbility as? DefensiveAbility.Dodge)?.armorBonus ?: 0

    return CharacterClass.create(
        type = type,
        hitPoints = maxHitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass - dodgeArmorBonus,
        speed = speed,
    )
}
