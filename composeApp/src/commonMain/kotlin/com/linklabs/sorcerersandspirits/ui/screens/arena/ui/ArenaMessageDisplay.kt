package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RollTypeState
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_message_chose_character_class
import sorcerersandspirits.composeapp.generated.resources.arena_message_joined_arena
import sorcerersandspirits.composeapp.generated.resources.arena_message_new_turn
import sorcerersandspirits.composeapp.generated.resources.arena_message_round

@Composable
internal fun ArenaMessageState.getDisplayString(): String =
    when (this) {
        is ArenaMessageState.NewRound ->
            stringResource(resource = Res.string.arena_message_round, roundNumber)

        is ArenaMessageState.ChoseCharacterClass ->
            stringResource(
                resource = Res.string.arena_message_chose_character_class,
                displayName,
                characterClassInfo.className,
            )

        is ArenaMessageState.ConnectedToArena ->
            stringResource(resource = Res.string.arena_message_joined_arena, displayName)

        is ArenaMessageState.NewTurn ->
            stringResource(
                resource = Res.string.arena_message_new_turn,
                characterClassInfo.className,
            )

        is ArenaMessageState.DiceRoll -> {
            val typeStr =
                when (rollType) {
                    is RollTypeState.AbilityRoll ->
                        "ability (${rollType.ability::class.simpleName})"

                    RollTypeState.ArmorClassRoll -> "Armor Class"

                    RollTypeState.HealthRoll -> "Health"
                }
            "$rollerClassName rolled $rollValue for $typeStr"
        }

        is ArenaMessageState.AttackResult -> {
            val action =
                when (rollType) {
                    is RollTypeState.AbilityRoll -> "ability usage"
                    RollTypeState.ArmorClassRoll -> "attack"
                    RollTypeState.HealthRoll -> "kill"
                }
            val result = if (isHit) "HIT!" else "MISS"
            "$attackerClassName's $action against $targetClassName: $result"
        }

        is ArenaMessageState.TargetDead -> "$targetClassName has been defeated by $killerClassName!"

        is ArenaMessageState.GameFinished -> "GAME OVER! $winnerClassName is the Champion!"
    }
