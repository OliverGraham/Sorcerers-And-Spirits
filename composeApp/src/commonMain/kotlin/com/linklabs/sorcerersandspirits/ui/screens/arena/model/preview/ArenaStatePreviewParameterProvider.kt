package com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

// TODO(SS-028) Arena: update when state changes to use enums to determine the text (using
// resources)

class ArenaStatePreviewParameterProvider : PreviewParameterProvider<ArenaScreenState> {
    private val classStates = createExampleCharacterClassStates()
    private val mageState = classStates.first { it.type == CharacterClassType.Mage }
    private val rogueState = classStates.first { it.type == CharacterClassType.Rogue }

    override val values: Sequence<ArenaScreenState> =
        sequenceOf(
            getExampleScreenState(
                arenaState = getExampleArenaState(),
                messages =
                    listOf(
                        ArenaMessageState.ChoseCharacterClass(
                            id = "1",
                            displayName = "Spirit Walker",
                            characterClassInfo =
                                CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue),
                        )
                    ),
            ),
            getExampleScreenState(arenaState = getExampleArenaState()),
            getExampleScreenState(arenaState = getExampleChoosingClassesState()),
            getExampleScreenState(arenaState = getExampleCombatState()),
            getExampleScreenState(arenaState = getExampleFinishedState()),
        )

    private fun getExampleScreenState(
        arenaState: ArenaState,
        messages: List<ArenaMessageState> = getExampleMessages(),
        unhandledMessagesCount: Int = 0,
        secondsRemaining: Int = 20,
        showForfeitDialog: Boolean = false,
        isUserPlayerKnockedOut: Boolean = false,
        hasReadiedForNextMatch: Boolean = false,
    ) =
        ArenaScreenState(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = secondsRemaining,
            showForfeitDialog = showForfeitDialog,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = arenaState,
            participantClassNames =
                getExamplePlayers().mapValues { it.value.characterClass.className },
        )

    private fun getExampleArenaState() = ArenaState.WaitingForAllPlayers

    private fun getExampleFinishedState() =
        ArenaState.Finished(
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
        ArenaState.ChoosingClasses(
            playerMap = playerMap,
            userPlayerId = userPlayerId,
            activePlayerUserId = activePlayerUserId,
            selectedClass = null,
            unavailableClasses = emptyList(),
            classSelections = emptyMap(),
        )

    private fun getExampleCombatState(
        playerMap: Map<String, PlayerState.Selected> = getExamplePlayers(),
        userPlayerId: String = "player1",
        combatActionState: CombatActionState =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackerUserId = "player1",
                defenderUserId = "player2",
                originalTurnUserId = "player1",
                activeUserIds = setOf("player1"),
            ),
    ) =
        ArenaState.Combat(
            playerMap = playerMap,
            userPlayerId = userPlayerId,
            combatActionState = combatActionState,
            roundNumber = 1,
            isNewRound = true,
            doublesBonus = null,
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
                characterClassInfo = CharacterClassMessageInfoState("Mage", CharacterClassType.Mage),
            ),
            ArenaMessageState.ChoseCharacterClass(
                id = "21",
                displayName = "Spirit Walker",
                characterClassInfo =
                    CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue),
            ),
            ArenaMessageState.ConnectedToArena(id = "1", displayName = "The battle begins!"),
            ArenaMessageState.NewRound(id = "2", roundNumber = "1"),
            ArenaMessageState.NewTurn(
                id = "3",
                characterClassInfo =
                    CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue),
            ),
        )
}
