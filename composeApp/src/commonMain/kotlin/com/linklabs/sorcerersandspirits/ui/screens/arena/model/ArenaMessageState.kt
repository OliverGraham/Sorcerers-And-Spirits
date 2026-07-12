package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
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
        val impersonatedClassType: CharacterClassType,
    ) : ArenaMessageState {
        /**
         * The true roll total: the two die faces plus any consecutive-doubles bonus that applied.
         * It is both the Armor-class total behind a "Hit!" and the true damage behind a "Damage!".
         */
        val total: Int = firstDie + secondDie + bonus
    }

    /**
     * The domain trigger names no one, so [attackerUserId] is carried over from the knockout roll
     * that earned the momentum; null when that roll is no longer in the log.
     */
    @Immutable
    data class KillingMomentum(override val id: String, val attackerUserId: String) :
        ArenaMessageState

    /**
     * The domain trigger names no one, so [dodgerUserId] is carried over from the missed roll the
     * Rogue dodged; null when that roll is no longer in the log.
     */
    @Immutable
    data class Dodge(
        override val id: String,
        val didArmorIncrease: Boolean,
        val dodgerUserId: String,
    ) : ArenaMessageState

    @Immutable
    sealed interface Teleport : ArenaMessageState {
        /** The Mage escaping the attack, so the portal animation anchors to their seat. */
        val mageUserId: String

        @Immutable
        data class Trigger(override val id: String, override val mageUserId: String) : Teleport

        @Immutable
        data class Roll(
            override val id: String,
            override val mageUserId: String,
            val playerName: String,
            val singleDie: Int,
        ) : Teleport

        @Immutable
        data class Result(
            override val id: String,
            override val mageUserId: String,
            val didDefenderEscape: Boolean,
        ) : Teleport
    }

    @Immutable
    sealed interface LuckyCoin : ArenaMessageState {
        val gamblerUserId: String

        @Immutable
        data class Trigger(override val id: String, override val gamblerUserId: String) : LuckyCoin

        @Immutable
        data class Flip(
            override val id: String,
            override val gamblerUserId: String,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        @Immutable
        data class HighRollerTrigger(
            override val id: String,
            override val gamblerUserId: String,
            val targetName: String,
        ) : LuckyCoin

        @Immutable
        data class HighRollerDamage(
            override val id: String,
            override val gamblerUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin
    }

    @Immutable
    sealed interface Riposte : ArenaMessageState {
        @Immutable
        data class Trigger(
            override val id: String,
            val duelistUserId: String,
            val attackerUserId: String,
        ) : Riposte

        @Immutable
        data class EnGarde(
            override val id: String,
            val duelistUserId: String,
            val attackerUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
    }

    @Immutable
    data class SummonMinion(
        override val id: String,
        val necroUserId: String,
        val necroName: String,
        val singleDie: Int,
        val outcome: SummonMinionOutcome,
    ) : ArenaMessageState

    @Immutable
    sealed interface Persuade : ArenaMessageState {
        @Immutable data class Trigger(override val id: String, val bardUserId: String) : Persuade

        @Immutable
        data class StrumLute(
            override val id: String,
            val bardUserId: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Immutable
        data class CharmActivation(override val id: String, val bardUserId: String) : Persuade

        @Immutable data class Lull(override val id: String) : Persuade

        @Immutable
        data class Charm(
            override val id: String,
            val bardUserId: String,
            val attackerName: String,
            val defenderName: String,
        ) : Persuade
    }

    @Immutable
    sealed interface Encore : ArenaMessageState {
        val bardUserId: String

        @Immutable
        data class Trigger(override val id: String, override val bardUserId: String) : Encore

        @Immutable
        data class Roll(
            override val id: String,
            override val bardUserId: String,
            val encoreDie: Int,
            val isSuccessful: Boolean,
        ) : Encore
    }

    @Immutable
    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val illusionistName: String,
        val impersonatedClassType: CharacterClassType,
        val impersonatedAbilityName: String,
    ) : ArenaMessageState

    @Immutable
    sealed interface MirrorImage : ArenaMessageState {
        /**
         * The Illusionist defending the attack, so the split-image animation anchors to their seat.
         * Read from the armor-break roll that preceded this trigger (SS-209).
         */
        val illusionistUserId: String

        @Immutable
        data class Trigger(
            override val id: String,
            override val illusionistUserId: String,
            val firstDie: Int?,
            val secondDie: Int?,
        ) : MirrorImage

        @Immutable
        data class Pass(
            override val id: String,
            override val illusionistUserId: String,
            val attackerName: String,
            val messageType: NormalRollMessageType,
        ) : MirrorImage

        @Immutable
        data class TemptFate(
            override val id: String,
            override val illusionistUserId: String,
            val originalRoll: Int,
            val rawReroll: Int,
            val finalRoll: Int,
            val attackerName: String,
            val messageType: NormalRollMessageType,
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
        val mageUserId: String,
        val mageName: String,
        val mainTargetUserId: String,
        val mainTargetName: String,
        val firstDie: Int,
        val secondDie: Int,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String>,
        val isMainTargetKnockedOut: Boolean,
    ) : ArenaMessageState

    @Immutable
    sealed interface HolyShield : ArenaMessageState {
        val paladinUserId: String

        @Immutable
        data class Trigger(override val id: String, override val paladinUserId: String) : HolyShield

        @Immutable
        data class Prayer(
            override val id: String,
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield

        @Immutable
        data class Wrath(
            override val id: String,
            override val paladinUserId: String,
            val paladinName: String,
            val attackerUserId: String,
            val attackerName: String,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
        ) : HolyShield
    }

    @Immutable
    data class AllIn(
        override val id: String,
        val gamblerUserId: String,
        val gamblerName: String,
        val singleDie: Int,
        val outcome: AllInOutcome,
    ) : ArenaMessageState

    @Immutable
    data class PrepareAttack(
        override val id: String,
        val duelistUserId: String,
        val duelistName: String,
        val targetUserId: String,
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

/**
 * Maps the incoming messages in order while remembering the most recent [ArenaMessage.NormalRoll],
 * because the KillingMomentum and Dodge triggers name no players of their own: the domain emits
 * each one immediately after the roll that triggered it, so the actor their avatar animation needs
 * (SS-192) is read from that roll.
 */
fun List<IncomingArenaMessage>.toStates(
    participants: Map<String, ParticipantClass>
): List<ArenaMessageState> = map { incomingMessage ->
    incomingMessage.message.toState(id = incomingMessage.id, participants = participants)
}

private fun ArenaMessage.toState(
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
                gamblerUserId = gamblerUserId,
                gamblerName = participants.resolveClassName(userId = gamblerUserId),
                singleDie = singleDie,
                outcome = allInOutcomeFor(singleDie = singleDie),
            )

        is ArenaMessage.Dodge ->
            ArenaMessageState.Dodge(
                id = id,
                didArmorIncrease = didArmorIncrease,
                dodgerUserId = dodgerUserId,
            )

        is ArenaMessage.Encore.Trigger ->
            ArenaMessageState.Encore.Trigger(id = id, bardUserId = bardUserId)

        is ArenaMessage.Encore.Roll ->
            ArenaMessageState.Encore.Roll(
                id = id,
                bardUserId = bardUserId,
                encoreDie = encoreDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.HolyShield.Wrath ->
            ArenaMessageState.HolyShield.Wrath(
                id = id,
                paladinUserId = paladinUserId,
                paladinName = participants.resolveClassName(userId = paladinUserId),
                attackerUserId = attackerUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                singleDie = singleDie,
                isAttackerKnockedOut = isAttackerKnockedOut,
            )

        is ArenaMessage.HolyShield.Prayer ->
            ArenaMessageState.HolyShield.Prayer(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
            )

        is ArenaMessage.HolyShield.Trigger ->
            ArenaMessageState.HolyShield.Trigger(id = id, paladinUserId = paladinUserId)

        is ArenaMessage.Impersonate ->
            ArenaMessageState.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                illusionistName = participants.resolveClassName(userId = illusionistUserId),
                impersonatedClassType = impersonateAbility.impersonatedClassType,
                impersonatedAbilityName =
                    impersonateAbility.impersonatedAbility::class.simpleName.orEmpty(),
            )

        is ArenaMessage.KillingMomentum ->
            ArenaMessageState.KillingMomentum(id = id, attackerUserId = attackerUserId)

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageState.LuckyCoin.Flip(
                id = id,
                gamblerUserId = gamblerUserId,
                singleDie = singleDie,
                didStopAttack = didStopAttack,
            )

        is ArenaMessage.LuckyCoin.HighRollerDamage ->
            ArenaMessageState.LuckyCoin.HighRollerDamage(
                id = id,
                gamblerUserId = gamblerUserId,
                attackerUserId = attackerUserId,
                singleDie = singleDie,
                isKnockout = isKnockout,
            )

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageState.LuckyCoin.HighRollerTrigger(
                id = id,
                gamblerUserId = gamblerUserId,
                targetName = participants.resolveClassName(userId = attackerUserId),
            )

        is ArenaMessage.LuckyCoin.Trigger ->
            ArenaMessageState.LuckyCoin.Trigger(id = id, gamblerUserId = gamblerUserId)

        is ArenaMessage.MirrorImage.Pass ->
            ArenaMessageState.MirrorImage.Pass(
                id = id,
                illusionistUserId = illusionistUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                messageType = messageType,
            )

        is ArenaMessage.MirrorImage.Trigger ->
            ArenaMessageState.MirrorImage.Trigger(
                id = id,
                illusionistUserId = illusionistUserId,
                firstDie = firstDie,
                secondDie = secondDie,
            )

        is ArenaMessage.MirrorImage.TemptFate ->
            ArenaMessageState.MirrorImage.TemptFate(
                id = id,
                illusionistUserId = illusionistUserId,
                originalRoll = rerolledMirrorImageDie.originalRoll,
                rawReroll = rerolledMirrorImageDie.reroll ?: rerolledMirrorImageDie.originalRoll,
                finalRoll = rerolledMirrorImageDie.finalValue,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                messageType = messageType,
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
                impersonatedClassType = impersonatedCharacterType,
            )

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageState.Persuade.Charm(
                id = id,
                bardUserId = bardUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                defenderName = participants.resolveClassName(userId = defenderUserId),
            )

        is ArenaMessage.Persuade.CharmActivation ->
            ArenaMessageState.Persuade.CharmActivation(id = id, bardUserId = bardUserId)

        is ArenaMessage.Persuade.Lull -> ArenaMessageState.Persuade.Lull(id = id)

        is ArenaMessage.Persuade.StrumLute ->
            ArenaMessageState.Persuade.StrumLute(
                id = id,
                bardUserId = bardUserId,
                diceRoll = diceRoll,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Persuade.Trigger ->
            ArenaMessageState.Persuade.Trigger(id = id, bardUserId = bardUserId)

        is ArenaMessage.PrepareAttack ->
            ArenaMessageState.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                duelistName = participants.resolveClassName(userId = duelistUserId),
                targetUserId = targetUserId,
                targetName = participants.resolveClassName(userId = targetUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Riposte.EnGarde ->
            ArenaMessageState.Riposte.EnGarde(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Riposte.Trigger ->
            ArenaMessageState.Riposte.Trigger(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
            )

        is ArenaMessage.SpellPotency ->
            ArenaMessageState.SpellPotency(
                id = id,
                mageUserId = mageUserId,
                mageName = participants.resolveClassName(userId = mageUserId),
                mainTargetUserId = mainTargetUserId,
                mainTargetName = participants.resolveClassName(userId = mainTargetUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
                splashTargetUserIds = splashTargetUserIds,
                isMainTargetKnockedOut = isMainTargetKnockedOut,
            )

        is ArenaMessage.SummonMinion ->
            ArenaMessageState.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                necroName = participants.resolveClassName(userId = necroUserId),
                singleDie = singleDie,
                outcome = summonMinionOutcomeFor(singleDie = singleDie),
            )

        is ArenaMessage.Teleport.Result ->
            ArenaMessageState.Teleport.Result(
                id = id,
                mageUserId = mageUserId,
                didDefenderEscape = didDefenderEscape,
            )

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageState.Teleport.Roll(
                id = id,
                mageUserId = mageUserId,
                playerName = participants.resolveClassName(userId = userId),
                singleDie = singleDie,
            )

        is ArenaMessage.Teleport.Trigger ->
            ArenaMessageState.Teleport.Trigger(id = id, mageUserId = mageUserId)

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
