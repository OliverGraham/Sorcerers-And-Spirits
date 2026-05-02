package com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

// TODO Arena: update when state changes to use enums to determine the text (using resources)

class ArenaStatePreviewParameterProvider : PreviewParameterProvider<ArenaState> {
    private val classStates = createExampleCharacterClassStates()
    private val mageState = classStates.filterIsInstance<CharacterClassState.Mage>().first()
    private val rogueState = classStates.filterIsInstance<CharacterClassState.Rogue>().first()

    override val values: Sequence<ArenaState> =
        sequenceOf(
            getExampleArenaState(
                messages =
                    listOf(
                        ArenaMessageState.Generic(id = "1", message = "Spirit Walker chose Rogue")
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
            ArenaMessageState.Generic(id = "12", message = "Player1 selected Rogue"),
            ArenaMessageState.Generic(id = "21", message = "Player2 selected Mage"),
            ArenaMessageState.Generic(id = "1", message = "The battle begins!"),
            ArenaMessageState.Round(id = "2", roundNumber = "1"),
            ArenaMessageState.Generic(id = "3", message = "It is Rogue's turn!"),
        )
}
