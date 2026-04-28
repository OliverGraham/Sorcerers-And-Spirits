package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType

@Immutable
sealed interface ArenaMessageState {
    val id: String

    @Immutable
    data class Round(override val id: String, val roundNumber: String) : ArenaMessageState

    @Immutable
    data class Action(
        override val id: String,
        val rollerName: String,
        val rollerValue: String,
        val defenderName: String,
        val rollTypeState: RollTypeState,
    ) : ArenaMessageState

    @Immutable
    data class Result(
        override val id: String,
        val rollerName: String,
        val defenderName: String,
        val rollTypeState: RollTypeState,
    ) : ArenaMessageState

    @Immutable data class Generic(override val id: String, val message: String) : ArenaMessageState
}

// TODO Arena: Use enums to then use string resources
fun ArenaMessageState.toDisplayText(): String =
    when (this) {
        is ArenaMessageState.Round -> "ROUND $roundNumber"

        is ArenaMessageState.Action ->
            "$rollerName rolled $rollerValue against $defenderName's $rollTypeState"

        is ArenaMessageState.Result ->
            "$rollerName is now rolling against $defenderName's $rollTypeState!"

        is ArenaMessageState.Generic -> message
    }

@Immutable
enum class RollTypeState {
    Ability,
    ArmorClass,
    Health,
}

fun ArenaMessageState.toDomain(): ArenaMessage =
    when (this) {
        is ArenaMessageState.Round ->
            ArenaMessage.Round(id = id, roundNumber = roundNumber.toIntOrNull() ?: 0)

        is ArenaMessageState.Action ->
            ArenaMessage.Action(
                id = id,
                rollerName = rollerName,
                rollerValue = rollerValue.toIntOrNull() ?: 0,
                defenderName = defenderName,
                rollType = rollTypeState.toDomain(),
            )

        is ArenaMessageState.Result ->
            ArenaMessage.Result(
                id = id,
                rollerName = rollerName,
                defenderName = defenderName,
                rollType = rollTypeState.toDomain(),
            )

        is ArenaMessageState.Generic -> ArenaMessage.Generic(id = id, message = message)
    }

fun ArenaMessage.toState(): ArenaMessageState =
    when (this) {
        is ArenaMessage.Round ->
            ArenaMessageState.Round(id = id, roundNumber = roundNumber.toString())

        is ArenaMessage.Action ->
            ArenaMessageState.Action(
                id = id,
                rollerName = rollerName,
                rollerValue = rollerValue.toString(),
                defenderName = defenderName,
                rollTypeState = rollType.toState(),
            )

        is ArenaMessage.Result ->
            ArenaMessageState.Result(
                id = id,
                rollerName = rollerName,
                defenderName = defenderName,
                rollTypeState = rollType.toState(),
            )

        is ArenaMessage.Generic -> ArenaMessageState.Generic(id = id, message = message)
    }

fun RollTypeState.toDomain(): RollType =
    when (this) {
        RollTypeState.Ability -> RollType.Ability
        RollTypeState.ArmorClass -> RollType.ArmorClass
        RollTypeState.Health -> RollType.Health
    }

fun RollType.toState(): RollTypeState =
    when (this) {
        RollType.Ability -> RollTypeState.Ability
        RollType.ArmorClass -> RollTypeState.ArmorClass
        RollType.Health -> RollTypeState.Health
    }
