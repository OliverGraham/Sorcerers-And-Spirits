package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * A full-screen flash call-out (SS-148/SS-164): a sealed, punchy description of a flash-worthy
 * event. Each variant carries only the data its wording needs, so the text is built from a string
 * resource at render time rather than baked in here. [actingClassType] colors a call-out in that
 * class's color; a null type renders neutral (white) — used for "Miss!" and ability call-outs until
 * per-class message coloring lands (SS-179).
 */
@Immutable
sealed interface FlashCallout {
    val actingClassType: CharacterClassType?
        get() = null

    @Immutable
    data class Hit(override val actingClassType: CharacterClassType, val total: Int) : FlashCallout

    @Immutable data object Miss : FlashCallout

    @Immutable
    data class Damage(val amount: Int, override val actingClassType: CharacterClassType? = null) :
        FlashCallout

    @Immutable
    data class Knockout(override val actingClassType: CharacterClassType? = null) : FlashCallout

    @Immutable data object KillingMomentum : FlashCallout

    @Immutable data object Dodge : FlashCallout

    @Immutable data object SpellPotency : FlashCallout

    @Immutable data object Doubles : FlashCallout

    @Immutable data object Impersonate : FlashCallout

    @Immutable data object PreparedStrike : FlashCallout

    @Immutable data object AttackWhiffed : FlashCallout

    @Immutable data object Encore : FlashCallout

    @Immutable data object AllIn : FlashCallout

    @Immutable data object AllInStrikeTwice : FlashCallout

    @Immutable data object Fizzle : FlashCallout

    @Immutable data object Skeleton : FlashCallout

    @Immutable data class SkeletonAndHeal(val necromancerName: String) : FlashCallout

    @Immutable data class Golem(val necromancerName: String) : FlashCallout

    @Immutable data object Teleport : FlashCallout

    @Immutable data object Escaped : FlashCallout

    @Immutable data object Caught : FlashCallout

    @Immutable data object LuckyCoin : FlashCallout

    @Immutable data object AttackStopped : FlashCallout

    @Immutable data class AttackContinues(val die: Int) : FlashCallout

    @Immutable data object Riposte : FlashCallout

    @Immutable data object Reversed : FlashCallout

    @Immutable data object CounterFailed : FlashCallout

    @Immutable data object Persuade : FlashCallout

    @Immutable data object Charmed : FlashCallout

    @Immutable data class Redirected(val targetName: String) : FlashCallout

    @Immutable data object MirrorImage : FlashCallout

    @Immutable data class Reroll(val finalRoll: Int) : FlashCallout

    @Immutable data object HolyShield : FlashCallout

    @Immutable data object Saved : FlashCallout

    @Immutable data object StruckDown : FlashCallout

    @Immutable data object HolyWrath : FlashCallout

    @Immutable data class NewRound(val roundNumber: String) : FlashCallout

    @Immutable
    data class ChoseClass(val className: String, override val actingClassType: CharacterClassType) :
        FlashCallout

    @Immutable data class Joined(val playerName: String) : FlashCallout

    @Immutable
    data class TurnStart(val className: String, override val actingClassType: CharacterClassType) :
        FlashCallout

    @Immutable data class TeleportRoll(val die: Int) : FlashCallout

    @Immutable data object HighRoller : FlashCallout

    @Immutable data object Lulled : FlashCallout

    @Immutable data object EncoreAgain : FlashCallout

    @Immutable data object NoEncore : FlashCallout

    @Immutable data object MirrorHolds : FlashCallout
}

/**
 * The flash call-out for a message. Every message currently flashes (SS-164); paring some back to
 * log-only is a follow-up, which the nullable return and the [ArenaScreenState.flashMessages]
 * filter keep room for. Normal-attack outcomes flash their result; an ability flashes every step it
 * takes — its name then the outcome of its roll — so a defensive ability narrates as a sequence
 * (e.g. [Hit] then [FlashCallout.LuckyCoin] then [FlashCallout.AttackStopped]). The exhaustive
 * [when] makes a new message type a compile error until its flash is chosen.
 */
val ArenaMessageState.flashCallout: FlashCallout?
    get() =
        when (this) {
            is ArenaMessageState.NewRound -> FlashCallout.NewRound(roundNumber = roundNumber)

            is ArenaMessageState.ChoseCharacterClass ->
                FlashCallout.ChoseClass(
                    className = characterClassInfo.className,
                    actingClassType = characterClassInfo.type,
                )

            is ArenaMessageState.ConnectedToArena -> FlashCallout.Joined(playerName = displayName)

            is ArenaMessageState.NewTurn ->
                FlashCallout.TurnStart(
                    className = characterClassInfo.className,
                    actingClassType = characterClassInfo.type,
                )

            is ArenaMessageState.NormalRoll ->
                when (type) {
                    NormalRollMessageType.ArmorClassHit ->
                        FlashCallout.Hit(actingClassType = attackerClassType, total = total)

                    NormalRollMessageType.ArmorClassMiss -> FlashCallout.Miss

                    NormalRollMessageType.Damage ->
                        FlashCallout.Damage(amount = total, actingClassType = attackerClassType)

                    NormalRollMessageType.Knockout ->
                        FlashCallout.Knockout(actingClassType = attackerClassType)
                }

            is ArenaMessageState.KillingMomentum -> FlashCallout.KillingMomentum

            is ArenaMessageState.Dodge -> FlashCallout.Dodge

            is ArenaMessageState.Teleport.Trigger -> FlashCallout.Teleport

            is ArenaMessageState.Teleport.Roll -> FlashCallout.TeleportRoll(die = singleDie)

            is ArenaMessageState.Teleport.Result ->
                if (didDefenderEscape) FlashCallout.Escaped else FlashCallout.Caught

            is ArenaMessageState.LuckyCoin.Trigger -> FlashCallout.LuckyCoin

            is ArenaMessageState.LuckyCoin.HighRollerTrigger -> FlashCallout.HighRoller

            is ArenaMessageState.LuckyCoin.Flip ->
                if (didStopAttack) {
                    FlashCallout.AttackStopped
                } else {
                    FlashCallout.AttackContinues(die = singleDie)
                }

            is ArenaMessageState.LuckyCoin.HighRollerDamage ->
                if (isKnockout) FlashCallout.Knockout() else FlashCallout.Damage(amount = singleDie)

            is ArenaMessageState.Riposte.Trigger -> FlashCallout.Riposte

            is ArenaMessageState.Riposte.EnGarde ->
                if (isSuccessful) FlashCallout.Reversed else FlashCallout.CounterFailed

            is ArenaMessageState.SummonMinion ->
                when (outcome) {
                    SummonMinionOutcome.Fizzle -> FlashCallout.Fizzle

                    SummonMinionOutcome.Skeleton -> FlashCallout.Skeleton

                    SummonMinionOutcome.SkeletonAndHeal ->
                        FlashCallout.SkeletonAndHeal(necromancerName = necroName)

                    SummonMinionOutcome.Golem -> FlashCallout.Golem(necromancerName = necroName)
                }

            is ArenaMessageState.Persuade.Trigger -> FlashCallout.Persuade

            is ArenaMessageState.Persuade.StrumLute ->
                if (isSuccessful) {
                    FlashCallout.AttackStopped
                } else {
                    FlashCallout.AttackContinues(die = diceRoll)
                }

            is ArenaMessageState.Persuade.CharmActivation -> FlashCallout.Charmed

            is ArenaMessageState.Persuade.Lull -> FlashCallout.Lulled

            is ArenaMessageState.Persuade.Charm ->
                FlashCallout.Redirected(targetName = defenderName)

            is ArenaMessageState.Encore.Trigger -> FlashCallout.Encore

            is ArenaMessageState.Encore.Roll ->
                if (isSuccessful) FlashCallout.EncoreAgain else FlashCallout.NoEncore

            is ArenaMessageState.Impersonate -> FlashCallout.Impersonate

            is ArenaMessageState.MirrorImage.Trigger -> FlashCallout.MirrorImage

            is ArenaMessageState.MirrorImage.Pass -> FlashCallout.MirrorHolds

            is ArenaMessageState.MirrorImage.TemptFate -> FlashCallout.Reroll(finalRoll = finalRoll)

            is ArenaMessageState.SpellPotency -> FlashCallout.SpellPotency

            is ArenaMessageState.HolyShield.Trigger -> FlashCallout.HolyShield

            is ArenaMessageState.HolyShield.Prayer ->
                if (didSurvive) FlashCallout.Saved else FlashCallout.StruckDown

            is ArenaMessageState.HolyShield.Wrath -> FlashCallout.HolyWrath

            is ArenaMessageState.AllIn ->
                when (outcome) {
                    AllInOutcome.SelfKnockout -> FlashCallout.Knockout()
                    AllInOutcome.DoubleAttack -> FlashCallout.AllInStrikeTwice
                    AllInOutcome.NoEffect -> FlashCallout.AllIn
                }

            is ArenaMessageState.PrepareAttack ->
                if (isSuccessful) FlashCallout.PreparedStrike else FlashCallout.AttackWhiffed

            is ArenaMessageState.Doubles -> FlashCallout.Doubles
        }
