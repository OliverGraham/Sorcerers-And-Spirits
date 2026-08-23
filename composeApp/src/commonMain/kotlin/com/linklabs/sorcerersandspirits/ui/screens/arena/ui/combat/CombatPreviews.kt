package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ArenaCombatMultiplayerPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                        previewCombatPlayer(
                            userId = "player3",
                            displayName = "Card Sharp",
                            type = CharacterClassType.Gambler,
                        ),
                        previewCombatPlayer(
                            userId = "player4",
                            displayName = "Holy Knight",
                            type = CharacterClassType.Paladin,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ArenaCombatTwoPlayerPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ArenaCombatKnockedOutOpponentPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                        previewCombatPlayer(
                            userId = "player3",
                            displayName = "Card Sharp",
                            type = CharacterClassType.Gambler,
                            hitPoints = 0,
                        ),
                        previewCombatPlayer(
                            userId = "player4",
                            displayName = "Holy Knight",
                            type = CharacterClassType.Paladin,
                        ),
                    )
            )
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ArenaCombatUserTurnPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Trickster",
                            type = CharacterClassType.Illusionist,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    ),
                combatActionState = CombatActionState.Idle(activeUserIds = setOf("player1")),
            )
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ArenaCombatFlashPreview() {
    PreviewArenaCombat(
        state =
            previewCombatState(
                players =
                    listOf(
                        previewCombatPlayer(
                            userId = "player1",
                            displayName = "Sorcerer Supreme",
                            type = CharacterClassType.Mage,
                        ),
                        previewCombatPlayer(
                            userId = "player2",
                            displayName = "Spirit Walker",
                            type = CharacterClassType.Rogue,
                        ),
                    )
            ),
        flashMessage =
            ArenaMessageState.NormalRoll.Outcome(
                id = "ko",
                attackerUserId = "player1",
                attackerClassName = "Mage",
                defenderUserId = "player2",
                defenderClassName = "Rogue",
                firstDie = 6,
                secondDie = 6,
                bonus = 0,
                normalRollMessageType = NormalRollMessageType.Knockout,
                animation = ArenaAnimationState.Flash,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun InspectDrawerPreview() {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            InspectContent(
                player =
                    PlayerState.Selected(
                        userId = "player1",
                        displayName = "Sorcerer Supreme",
                        characterClass = previewCharacterClassState(type = CharacterClassType.Mage),
                    ),
                onClose = {},
            )
        }
    }
}

@Composable
private fun PreviewArenaCombat(
    state: ArenaMatchState.Combat,
    messages: List<ArenaMessageState> = previewMessages(),
    flashMessage: ArenaMessageState? = null,
    secondsRemaining: Int = 8,
) {
    val allMessages = if (flashMessage != null) messages + flashMessage else messages
    val screenState =
        ArenaScreenState(
            messageGroups = listOf(ArenaMessageGroupState.Setup(messages = allMessages)),
            showForfeitDialog = false,
            isUserPlayerKnockedOut = false,
            hasReadiedForNextMatch = false,
            arenaMatchState = state,
        )

    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            ArenaCombat(
                screenState = screenState,
                arenaMatchState = state,
                timerState = { ArenaTimerState(secondsRemaining = secondsRemaining) },
                modifier =
                    Modifier.fillMaxSize()
                        .paint(
                            painter = painterResource(resource = Res.drawable.arena_background),
                            contentScale = ContentScale.Crop,
                        ),
            )
        }
    }
}

private fun previewCombatPlayer(
    userId: String,
    displayName: String,
    type: CharacterClassType,
    hitPoints: Int? = null,
): PlayerState.Selected {
    val classState = previewCharacterClassState(type = type)
    return PlayerState.Selected(
        userId = userId,
        displayName = displayName,
        characterClass =
            if (hitPoints == null) classState else classState.copy(hitPoints = hitPoints),
    )
}

private fun previewCombatState(
    players: List<PlayerState.Selected>,
    combatActionState: CombatActionState? = null,
): ArenaMatchState.Combat {
    val userId = players.first().userId

    return ArenaMatchState.Combat(
        playerMap = players.associateBy { it.userId },
        userPlayerId = userId,
        combatActionState =
            combatActionState
                ?: CombatActionState.NormalAttack(
                    roll = NormalAttackType.ArmorClass,
                    attackTurn = AttackTurn.Standard(attackerUserId = userId),
                    defenderUserId = players[1].userId,
                    activeUserIds = setOf(userId),
                ),
        roundNumber = 1,
        isNewRound = true,
        isMatchOver = false,
        attackerUserId = userId,
        turnOwnerClassType = players.first().characterClass.type,
        lockedTargetUserId = null,
        diceRollState = null,
    )
}

private fun previewMessages(): List<ArenaMessageState> =
    listOf(
        ArenaMessageState.ConnectedToArena(id = "1", displayName = "The battle begins!"),
        ArenaMessageState.NewRound(id = "2", roundNumber = 1),
        ArenaMessageState.NewTurn(
            id = "3",
            characterClassInfo =
                CharacterClassMessageInfoState(className = "Mage", type = CharacterClassType.Mage),
        ),
    )
