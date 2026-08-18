package com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

class ArenaStatePreviewParameterProvider : PreviewParameterProvider<ArenaScreenState> {
    private val classStates = createExampleCharacterClassStates()
    private val mageState = classStates.first { it.type == CharacterClassType.Mage }
    private val rogueState = classStates.first { it.type == CharacterClassType.Rogue }

    override val values: Sequence<ArenaScreenState> =
        sequenceOf(
            getExampleScreenState(
                arenaMatchState = getExampleArenaState(),
                messages =
                    listOf(
                        ArenaMessageState.ChoseCharacterClass(
                            id = "1",
                            displayName = "Spirit Walker",
                            characterClassInfo = CharacterClassType.Rogue.toMessageInfoState(),
                        )
                    ),
            ),
            getExampleScreenState(arenaMatchState = getExampleArenaState()),
            getExampleScreenState(arenaMatchState = getExampleChoosingClassesState()),
            getExampleScreenState(arenaMatchState = getExampleCombatState()),
            getExampleScreenState(arenaMatchState = getExampleFinishedState()),
        )

    private fun getExampleScreenState(
        arenaMatchState: ArenaMatchState,
        messages: List<ArenaMessageState> = getExampleMessages(),
        secondsRemaining: Int = 20,
        showForfeitDialog: Boolean = false,
        isUserPlayerKnockedOut: Boolean = false,
        hasReadiedForNextMatch: Boolean = false,
    ) =
        ArenaScreenState(
            messageGroups = listOf(ArenaMessageGroupState.Setup(messages = messages)),
            secondsRemaining = secondsRemaining,
            showForfeitDialog = showForfeitDialog,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaMatchState = arenaMatchState,
        )

    private fun getExampleArenaState() = ArenaMatchState.WaitingForAllPlayers

    private fun getExampleFinishedState() =
        ArenaMatchState.Finished(
            players =
                listOf(
                    PlayerState.Selected(
                        userId = "player1",
                        displayName = "Sorcerer Supreme",
                        characterClass = mageState,
                    ),
                    PlayerState.Selected(
                        userId = "player2",
                        displayName = "Spirit Walker",
                        characterClass = rogueState.copy(hitPoints = 0),
                    ),
                )
        )

    private fun getExampleChoosingClassesState(
        playerMap: Map<String, PlayerState> = getExamplePlayers(),
        userPlayerId: String = "player1",
        activePlayerUserId: String = "player1",
    ) =
        ArenaMatchState.ChoosingClasses(
            playerMap = playerMap,
            userPlayerId = userPlayerId,
            activePlayerUserId = activePlayerUserId,
            selectedClass = null,
            unavailableClasses = emptySet(),
            classSelections = emptyMap(),
        )

    private fun getExampleCombatState(
        playerMap: Map<String, PlayerState.Selected> = getExamplePlayers(),
        userPlayerId: String = "player1",
        combatActionState: CombatActionState =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackTurn = AttackTurn.Standard(attackerUserId = "player1"),
                defenderUserId = "player2",
                activeUserIds = setOf("player1"),
            ),
    ) =
        ArenaMatchState.Combat(
            playerMap = playerMap,
            userPlayerId = userPlayerId,
            combatActionState = combatActionState,
            roundNumber = 1,
            isNewRound = true,
            isMatchOver = false,
            turnOwnerUserId = userPlayerId,
            lockedTargetUserId = null,
            diceRollState = null,
        )

    private fun createExampleCharacterClassStates(): List<CharacterClassState> =
        previewCharacterClassStates()

    private fun getExamplePlayers() =
        mapOf(
            "player1" to
                PlayerState.Selected(
                    userId = "player1",
                    displayName = "Sorcerer Supreme",
                    characterClass = mageState,
                ),
            "player2" to
                PlayerState.Selected(
                    userId = "player2",
                    displayName = "Spirit Walker",
                    characterClass = rogueState,
                ),
        )

    private fun getExampleMessages() =
        listOf(
            ArenaMessageState.ChoseCharacterClass(
                id = "12",
                displayName = "Sorcerer Supreme",
                characterClassInfo = CharacterClassType.Mage.toMessageInfoState(),
            ),
            ArenaMessageState.ChoseCharacterClass(
                id = "21",
                displayName = "Spirit Walker",
                characterClassInfo = CharacterClassType.Rogue.toMessageInfoState(),
            ),
            ArenaMessageState.ConnectedToArena(id = "1", displayName = "The battle begins!"),
            ArenaMessageState.NewRound(id = "2", roundNumber = 1),
            ArenaMessageState.NewTurn(
                id = "3",
                characterClassInfo = CharacterClassType.Rogue.toMessageInfoState(),
            ),
        )
}

private fun CharacterClassType.toMessageInfoState() =
    CharacterClassMessageInfoState(className = name, type = this)
