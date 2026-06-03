package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
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
    when (val message = this.message) {
        is ArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = message.roundNumber.toString())

        is ArenaMessage.ChoseCharacterClass ->
            ArenaMessageState.ChoseCharacterClass(
                id = id,
                displayName = message.displayName,
                characterClassInfo = message.characterClassInfo.toState(),
            )

        is ArenaMessage.ConnectedToArena ->
            ArenaMessageState.ConnectedToArena(id = id, displayName = message.displayName)

        is ArenaMessage.NewTurn ->
            ArenaMessageState.NewTurn(
                id = id,
                characterClassInfo = message.characterClassInfo.toState(),
            )

        is ArenaMessage.AllIn -> TODO()

        is ArenaMessage.Dodge -> TODO()

        is ArenaMessage.Encore.Trigger -> TODO()

        is ArenaMessage.Encore.Roll -> TODO()

        is ArenaMessage.HolyShield.Wrath -> TODO()

        is ArenaMessage.HolyShield.Prayer -> TODO()

        is ArenaMessage.HolyShield.Trigger -> TODO()

        is ArenaMessage.Impersonate -> TODO()

        is ArenaMessage.KillingMomentum -> TODO()

        is ArenaMessage.LuckyCoin.Flip -> TODO()

        is ArenaMessage.LuckyCoin.HighRollerDamage -> TODO()

        is ArenaMessage.LuckyCoin.HighRollerTrigger -> TODO()

        is ArenaMessage.LuckyCoin.Trigger -> TODO()

        is ArenaMessage.MirrorImage.Pass -> TODO()

        is ArenaMessage.MirrorImage.Reroll -> TODO()

        is ArenaMessage.MirrorImage.Trigger -> TODO()

        is ArenaMessage.MirrorImage.TemptFate -> TODO()

        is ArenaMessage.NormalRoll -> TODO()

        is ArenaMessage.Persuade.Charm -> TODO()

        is ArenaMessage.Persuade.CharmActivation -> TODO()

        is ArenaMessage.Persuade.Lull -> TODO()

        is ArenaMessage.Persuade.StrumLute -> TODO()

        is ArenaMessage.Persuade.Trigger -> TODO()

        is ArenaMessage.PrepareAttack -> TODO()

        is ArenaMessage.Riposte.EnGarde -> TODO()

        is ArenaMessage.Riposte.Trigger -> TODO()

        is ArenaMessage.SpellPotency -> TODO()

        is ArenaMessage.SummonMinion -> TODO()

        is ArenaMessage.Teleport.Result -> TODO()

        is ArenaMessage.Teleport.Roll -> TODO()

        is ArenaMessage.Teleport.Trigger -> TODO()
    }

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
