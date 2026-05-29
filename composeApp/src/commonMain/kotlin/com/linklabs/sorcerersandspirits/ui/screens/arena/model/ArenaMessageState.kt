package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
sealed interface ArenaMessageState {
    val id: String

    @Immutable
    data class NewRound(override val id: String, val roundNumber: String) : ArenaMessageState

    @Immutable
    data class ChoseCharacterClass(
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState

    @Immutable
    data class ConnectedToArena(override val id: String, val displayName: String) :
        ArenaMessageState

    @Immutable
    data class NewTurn(
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState
}

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

fun IncomingArenaMessage.toState(): ArenaMessageState =
    when (this) {
        is IncomingArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

        is IncomingArenaMessage.ChoseCharacterClass ->
            ArenaMessageState.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toState(),
            )

        is IncomingArenaMessage.ConnectedToArena ->
            ArenaMessageState.ConnectedToArena(id = id, displayName = displayName)

        is IncomingArenaMessage.NewTurn ->
            ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

        is IncomingArenaMessage.AllIn -> TODO()

        is IncomingArenaMessage.Dodge -> TODO()

        is IncomingArenaMessage.Encore -> TODO()

        is IncomingArenaMessage.HolyShield.Reflect -> TODO()

        is IncomingArenaMessage.HolyShield.Survival -> TODO()

        is IncomingArenaMessage.HolyShield.Trigger -> TODO()

        is IncomingArenaMessage.Impersonate -> TODO()

        is IncomingArenaMessage.KillingMomentum -> TODO()

        is IncomingArenaMessage.LuckyCoin.ArmorClassRoll -> TODO()

        is IncomingArenaMessage.LuckyCoin.ReflectRoll -> TODO()

        is IncomingArenaMessage.LuckyCoin.Trigger -> TODO()

        is IncomingArenaMessage.MirrorImage.Pass -> TODO()

        is IncomingArenaMessage.MirrorImage.Reroll -> TODO()

        is IncomingArenaMessage.MirrorImage.Trigger -> TODO()

        is IncomingArenaMessage.NormalRoll -> TODO()

        is IncomingArenaMessage.Persuade.Charm -> TODO()

        is IncomingArenaMessage.Persuade.Lull -> TODO()

        is IncomingArenaMessage.Persuade.Trigger -> TODO()

        is IncomingArenaMessage.PrepareAttack -> TODO()

        is IncomingArenaMessage.Riposte.EnGarde -> TODO()

        is IncomingArenaMessage.Riposte.Trigger -> TODO()

        is IncomingArenaMessage.SpellPotency -> TODO()

        is IncomingArenaMessage.SummonMinion -> TODO()

        is IncomingArenaMessage.Teleport.Result -> TODO()

        is IncomingArenaMessage.Teleport.Roll -> TODO()

        is IncomingArenaMessage.Teleport.Trigger -> TODO()
    }

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
