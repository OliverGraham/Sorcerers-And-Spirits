package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
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

    @Immutable
    data class NormalRoll(
        override val id: String,
        val attackerName: String,
        val defenderName: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : ArenaMessageState

    @Immutable data class KillingMomentum(override val id: String) : ArenaMessageState

    @Immutable
    data class Dodge(override val id: String, val didArmorIncrease: Boolean) : ArenaMessageState

    @Immutable
    sealed interface Teleport : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : Teleport

        @Immutable
        data class Roll(override val id: String, val playerName: String, val singleDie: Int) :
            Teleport

        @Immutable
        data class Result(override val id: String, val didDefenderEscape: Boolean) : Teleport
    }

    @Immutable
    sealed interface LuckyCoin : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : LuckyCoin

        @Immutable
        data class Flip(override val id: String, val singleDie: Int, val didStopAttack: Boolean) :
            LuckyCoin

        @Immutable
        data class HighRollerTrigger(override val id: String, val targetName: String) : LuckyCoin

        @Immutable
        data class HighRollerDamage(
            override val id: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin
    }

    @Immutable
    sealed interface Riposte : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : Riposte

        @Immutable
        data class EnGarde(
            override val id: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
    }

    @Immutable
    data class SummonMinion(override val id: String, val necroName: String, val singleDie: Int) :
        ArenaMessageState

    @Immutable
    sealed interface Persuade : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : Persuade

        @Immutable
        data class StrumLute(
            override val id: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Immutable data class CharmActivation(override val id: String) : Persuade

        @Immutable data class Lull(override val id: String) : Persuade

        @Immutable
        data class Charm(
            override val id: String,
            val attackerName: String,
            val defenderName: String,
        ) : Persuade
    }

    @Immutable
    sealed interface Encore : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : Encore

        @Immutable
        data class Roll(override val id: String, val encoreDie: Int, val isSuccessful: Boolean) :
            Encore
    }

    @Immutable
    data class Impersonate(
        override val id: String,
        val illusionistName: String,
        val impersonatedAbilityName: String,
    ) : ArenaMessageState

    @Immutable
    sealed interface MirrorImage : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : MirrorImage

        @Immutable data class Pass(override val id: String) : MirrorImage

        @Immutable
        data class Reroll(override val id: String, val firstDie: Int, val secondDie: Int) :
            MirrorImage

        @Immutable
        data class TemptFate(override val id: String, val originalRoll: Int, val finalRoll: Int) :
            MirrorImage
    }

    @Immutable
    data class SpellPotency(
        override val id: String,
        val mageName: String,
        val mainTargetName: String,
        val mainDamage: Int,
        val splashDamage: Int,
    ) : ArenaMessageState

    @Immutable
    sealed interface HolyShield : ArenaMessageState {
        @Immutable data class Trigger(override val id: String) : HolyShield

        @Immutable
        data class Prayer(
            override val id: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        @Immutable
        data class Wrath(
            override val id: String,
            val paladinName: String,
            val attackerName: String,
            val singleDie: Int,
        ) : HolyShield
    }

    @Immutable
    data class AllIn(override val id: String, val gamblerName: String, val singleDie: Int) :
        ArenaMessageState

    @Immutable
    data class PrepareAttack(
        override val id: String,
        val duelistName: String,
        val targetName: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessageState
}

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

fun ArenaMessage.toState(id: String, displayNames: Map<String, String>): ArenaMessageState =
    when (this) {
        is ArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

        is ArenaMessage.ChoseCharacterClass ->
            ArenaMessageState.ChoseCharacterClass(
                id = id,
                displayName = displayName,
                characterClassInfo = characterClassInfo.toState(),
            )

        is ArenaMessage.ConnectedToArena ->
            ArenaMessageState.ConnectedToArena(id = id, displayName = displayName)

        is ArenaMessage.NewTurn ->
            ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

        is ArenaMessage.AllIn ->
            ArenaMessageState.AllIn(
                id = id,
                gamblerName = displayNames.resolve(userId = gamblerUserId),
                singleDie = singleDie,
            )

        is ArenaMessage.Dodge ->
            ArenaMessageState.Dodge(id = id, didArmorIncrease = didArmorIncrease)

        is ArenaMessage.Encore.Trigger -> ArenaMessageState.Encore.Trigger(id = id)

        is ArenaMessage.Encore.Roll ->
            ArenaMessageState.Encore.Roll(
                id = id,
                encoreDie = encoreDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.HolyShield.Wrath ->
            ArenaMessageState.HolyShield.Wrath(
                id = id,
                paladinName = displayNames.resolve(userId = paladinUserId),
                attackerName = displayNames.resolve(userId = attackerUserId),
                singleDie = singleDie,
            )

        is ArenaMessage.HolyShield.Prayer ->
            ArenaMessageState.HolyShield.Prayer(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessage.HolyShield.Trigger -> ArenaMessageState.HolyShield.Trigger(id = id)

        is ArenaMessage.Impersonate ->
            ArenaMessageState.Impersonate(
                id = id,
                illusionistName = displayNames.resolve(userId = illusionistUserId),
                impersonatedAbilityName =
                    impersonateAbility.impersonatedAbility::class.simpleName.orEmpty(),
            )

        is ArenaMessage.KillingMomentum -> ArenaMessageState.KillingMomentum(id = id)

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageState.LuckyCoin.Flip(
                id = id,
                singleDie = singleDie,
                didStopAttack = didStopAttack,
            )

        is ArenaMessage.LuckyCoin.HighRollerDamage ->
            ArenaMessageState.LuckyCoin.HighRollerDamage(
                id = id,
                singleDie = singleDie,
                isKnockout = isKnockout,
            )

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageState.LuckyCoin.HighRollerTrigger(
                id = id,
                targetName = displayNames.resolve(userId = targetUserId),
            )

        is ArenaMessage.LuckyCoin.Trigger -> ArenaMessageState.LuckyCoin.Trigger(id = id)

        is ArenaMessage.MirrorImage.Pass -> ArenaMessageState.MirrorImage.Pass(id = id)

        is ArenaMessage.MirrorImage.Reroll ->
            ArenaMessageState.MirrorImage.Reroll(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessage.MirrorImage.Trigger -> ArenaMessageState.MirrorImage.Trigger(id = id)

        is ArenaMessage.MirrorImage.TemptFate ->
            ArenaMessageState.MirrorImage.TemptFate(
                id = id,
                originalRoll = rerolledMirrorImageDie.originalRoll,
                finalRoll = rerolledMirrorImageDie.finalValue,
            )

        is ArenaMessage.NormalRoll ->
            ArenaMessageState.NormalRoll(
                id = id,
                attackerName = displayNames.resolve(userId = attackerUserId),
                defenderName = displayNames.resolve(userId = defenderUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                type = type,
            )

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageState.Persuade.Charm(
                id = id,
                attackerName = displayNames.resolve(userId = attackerUserId),
                defenderName = displayNames.resolve(userId = defenderUserId),
            )

        is ArenaMessage.Persuade.CharmActivation ->
            ArenaMessageState.Persuade.CharmActivation(id = id)

        is ArenaMessage.Persuade.Lull -> ArenaMessageState.Persuade.Lull(id = id)

        is ArenaMessage.Persuade.StrumLute ->
            ArenaMessageState.Persuade.StrumLute(
                id = id,
                diceRoll = diceRoll,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Persuade.Trigger -> ArenaMessageState.Persuade.Trigger(id = id)

        is ArenaMessage.PrepareAttack ->
            ArenaMessageState.PrepareAttack(
                id = id,
                duelistName = displayNames.resolve(userId = duelistUserId),
                targetName = displayNames.resolve(userId = targetUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Riposte.EnGarde ->
            ArenaMessageState.Riposte.EnGarde(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Riposte.Trigger -> ArenaMessageState.Riposte.Trigger(id = id)

        is ArenaMessage.SpellPotency ->
            ArenaMessageState.SpellPotency(
                id = id,
                mageName = displayNames.resolve(userId = mageUserId),
                mainTargetName = displayNames.resolve(userId = mainTargetUserId),
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is ArenaMessage.SummonMinion ->
            ArenaMessageState.SummonMinion(
                id = id,
                necroName = displayNames.resolve(userId = necroUserId),
                singleDie = singleDie,
            )

        is ArenaMessage.Teleport.Result ->
            ArenaMessageState.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageState.Teleport.Roll(
                id = id,
                playerName = displayNames.resolve(userId = userId),
                singleDie = singleDie,
            )

        is ArenaMessage.Teleport.Trigger -> ArenaMessageState.Teleport.Trigger(id = id)
    }

private fun Map<String, String>.resolve(userId: String): String = this[userId] ?: userId

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
