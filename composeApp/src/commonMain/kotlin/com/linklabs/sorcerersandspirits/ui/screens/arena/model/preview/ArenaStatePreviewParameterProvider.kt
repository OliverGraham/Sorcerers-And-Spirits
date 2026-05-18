package com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

// TODO Arena: update when state changes to use enums to determine the text (using resources)

class ArenaStatePreviewParameterProvider : PreviewParameterProvider<ArenaState> {
    private val classStates = createExampleCharacterClassStates()
    private val mageState = classStates.first { it.type == CharacterClassType.Mage }
    private val rogueState = classStates.first { it.type == CharacterClassType.Rogue }

    override val values: Sequence<ArenaState> =
        sequenceOf(
            getExampleArenaState(
                messages =
                    listOf(
                        ArenaMessageState.ChoseCharacterClass(
                            id = "1",
                            displayName = "Spirit Walker",
                            characterClassInfo =
                                CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue),
                        )
                    )
            ),
            getExampleArenaState(),
            getExampleChoosingClassesState(),
            getExampleCombatState(),
        )

    private fun getExampleArenaState(
        messages: List<ArenaMessageState> = getExampleMessages(),
        unhandledMessagesCount: Int = 0,
        secondsRemaining: Int = 20,
    ) =
        ArenaState.WaitingForAllPlayers(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = secondsRemaining,
        )

    private fun getExampleChoosingClassesState(
        messages: List<ArenaMessageState> = getExampleMessages(),
        unhandledMessagesCount: Int = 0,
        secondsRemaining: Int = 20,
        activePlayer: PlayerState = getExamplePlayers().values.first(),
        userPlayer: PlayerState = getExamplePlayers().values.first(),
        otherPlayers: List<PlayerState> = getExamplePlayers().values.drop(1),
        isItThisUsersTurn: Boolean = true,
    ) =
        ArenaState.ChoosingClasses(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = secondsRemaining,
            activePlayer = activePlayer,
            userPlayer = userPlayer,
            otherPlayers = otherPlayers,
            isItThisUsersTurn = isItThisUsersTurn,
            selectedClass = null,
            unavailableClasses = emptyList(),
            classSelections = emptyMap(),
        )

    private fun getExampleCombatState(
        messages: List<ArenaMessageState> = getExampleMessages(),
        unhandledMessagesCount: Int = 0,
        secondsRemaining: Int = 20,
        activePlayer: PlayerState.Selected = getExamplePlayers().values.first(),
        userPlayer: PlayerState.Selected = getExamplePlayers().values.first(),
        otherPlayers: List<PlayerState.Selected> = getExamplePlayers().values.drop(1),
        isItThisUsersTurn: Boolean = true,
    ) =
        ArenaState.Combat(
            messages = messages,
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = secondsRemaining,
            activePlayer = activePlayer,
            userPlayer = userPlayer,
            otherPlayers = otherPlayers,
            isItThisUsersTurn = isItThisUsersTurn,
            pendingRollType = RollType.ArmorClass,
            pendingTargetUserId = null,
        )

    private fun createExampleCharacterClassStates(): List<CharacterClassState> =
        CharacterClass.getAllClasses().map { it.toState() }

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
