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
        val attackerUserId: String,
        val attackerName: String,
        val attackerClassType: CharacterClassType,
        val defenderUserId: String,
        val defenderName: String,
        val firstDie: Int,
        val secondDie: Int,
        val bonus: Int,
        val type: NormalRollMessageType,
    ) : ArenaMessageState {
        /**
         * The true roll total: the two die faces plus any consecutive-doubles bonus that applied.
         * It is both the Armor-class total behind a "Hit!" and the true damage behind a "Damage!".
         */
        val total: Int = firstDie + secondDie + bonus
    }

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
    data class SummonMinion(
        override val id: String,
        val necroName: String,
        val singleDie: Int,
        val outcome: SummonMinionOutcome,
    ) : ArenaMessageState

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
        @Immutable
        data class Trigger(override val id: String, val firstDie: Int?, val secondDie: Int?) :
            MirrorImage

        @Immutable
        data class Pass(
            override val id: String,
            val attackerName: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage

        @Immutable
        data class TemptFate(
            override val id: String,
            val originalRoll: Int,
            val rawReroll: Int,
            val finalRoll: Int,
            val attackerName: String,
            val outcome: NormalRollMessageType,
        ) : MirrorImage {
            /**
             * The reroll shown as the raw die and its -1 penalty (e.g. "4 - 1"), mirroring the
             * Summon Minion hit-point display, so the player sees what was rolled and the penalty
             * Mirror Image applied to it.
             */
            val rerollDisplay: String = "$rawReroll - 1"
        }
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
    data class AllIn(
        override val id: String,
        val gamblerName: String,
        val singleDie: Int,
        val outcome: AllInOutcome,
    ) : ArenaMessageState

    @Immutable
    data class PrepareAttack(
        override val id: String,
        val duelistName: String,
        val targetName: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessageState

    @Immutable
    data class Doubles(override val id: String, val playerName: String) : ArenaMessageState
}

/** The four distinct results of a Necromancer's Summon Minion roll, keyed off the rolled die. */
enum class SummonMinionOutcome {
    Fizzle,
    Skeleton,
    SkeletonAndHeal,
    Golem,
}

/** The three distinct results of a Gambler's All-In roll, keyed off the rolled die. */
enum class AllInOutcome {
    SelfKnockout,
    NoEffect,
    DoubleAttack,
}

private fun summonMinionOutcomeFor(singleDie: Int): SummonMinionOutcome =
    when (singleDie) {
        2,
        3 -> SummonMinionOutcome.Skeleton

        4,
        5 -> SummonMinionOutcome.SkeletonAndHeal

        6 -> SummonMinionOutcome.Golem

        else -> SummonMinionOutcome.Fizzle
    }

private fun allInOutcomeFor(singleDie: Int): AllInOutcome =
    when (singleDie) {
        1 -> AllInOutcome.SelfKnockout

        5,
        6 -> AllInOutcome.DoubleAttack

        else -> AllInOutcome.NoEffect
    }

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

/**
 * The class a participant played, kept for every player who has ever been in the arena so their
 * messages still resolve a name and color after they leave. Messages refer to players by class name
 * (SS-097); the [type] additionally colors normal-attack flashes in the acting class's color.
 */
@Immutable data class ParticipantClass(val className: String, val type: CharacterClassType)

fun ArenaMessage.toState(
    id: String,
    participants: Map<String, ParticipantClass>,
): ArenaMessageState =
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
                gamblerName = participants.resolveClassName(userId = gamblerUserId),
                singleDie = singleDie,
                outcome = allInOutcomeFor(singleDie = singleDie),
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
                paladinName = participants.resolveClassName(userId = paladinUserId),
                attackerName = participants.resolveClassName(userId = attackerUserId),
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
                illusionistName = participants.resolveClassName(userId = illusionistUserId),
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
                targetName = participants.resolveClassName(userId = targetUserId),
            )

        is ArenaMessage.LuckyCoin.Trigger -> ArenaMessageState.LuckyCoin.Trigger(id = id)

        is ArenaMessage.MirrorImage.Pass ->
            ArenaMessageState.MirrorImage.Pass(
                id = id,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                outcome = outcome,
            )

        is ArenaMessage.MirrorImage.Trigger ->
            ArenaMessageState.MirrorImage.Trigger(
                id = id,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessage.MirrorImage.TemptFate ->
            ArenaMessageState.MirrorImage.TemptFate(
                id = id,
                originalRoll = rerolledMirrorImageDie.originalRoll,
                rawReroll = rerolledMirrorImageDie.reroll ?: rerolledMirrorImageDie.originalRoll,
                finalRoll = rerolledMirrorImageDie.finalValue,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                outcome = outcome,
            )

        is ArenaMessage.NormalRoll ->
            ArenaMessageState.NormalRoll(
                id = id,
                attackerUserId = attackerUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                attackerClassType = participants.resolveClassType(userId = attackerUserId),
                defenderUserId = defenderUserId,
                defenderName = participants.resolveClassName(userId = defenderUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = bonus,
                type = type,
            )

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageState.Persuade.Charm(
                id = id,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                defenderName = participants.resolveClassName(userId = defenderUserId),
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
                duelistName = participants.resolveClassName(userId = duelistUserId),
                targetName = participants.resolveClassName(userId = targetUserId),
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
                mageName = participants.resolveClassName(userId = mageUserId),
                mainTargetName = participants.resolveClassName(userId = mainTargetUserId),
                mainDamage = mainDamage,
                splashDamage = splashDamage,
            )

        is ArenaMessage.SummonMinion ->
            ArenaMessageState.SummonMinion(
                id = id,
                necroName = participants.resolveClassName(userId = necroUserId),
                singleDie = singleDie,
                outcome = summonMinionOutcomeFor(singleDie = singleDie),
            )

        is ArenaMessage.Teleport.Result ->
            ArenaMessageState.Teleport.Result(id = id, didDefenderEscape = didDefenderEscape)

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageState.Teleport.Roll(
                id = id,
                playerName = participants.resolveClassName(userId = userId),
                singleDie = singleDie,
            )

        is ArenaMessage.Teleport.Trigger -> ArenaMessageState.Teleport.Trigger(id = id)

        is ArenaMessage.Doubles ->
            ArenaMessageState.Doubles(
                id = id,
                playerName = participants.resolveClassName(userId = userId),
            )
    }

private fun Map<String, ParticipantClass>.resolveClassName(userId: String) =
    getValue(key = userId).className

private fun Map<String, ParticipantClass>.resolveClassType(userId: String) =
    getValue(key = userId).type

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
