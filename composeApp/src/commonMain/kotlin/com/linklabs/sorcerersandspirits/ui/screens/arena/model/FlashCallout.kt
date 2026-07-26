package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * A full-screen flash call-out (SS-148/SS-164): a sealed, punchy description of a flash-worthy
 * event. Each variant carries only the data its wording needs, so the text is built from a string
 * resource at render time rather than baked in here. [actingClassType] colors a call-out in its
 * owning class's color (attacker's class for a normal roll, the ability's class for a triggered
 * ability); a null type renders neutral (white) for system banners like new rounds and joins.
 */
@Immutable
sealed interface FlashCallout {
    val actingClassType: CharacterClassType?
        get() = null

    @Immutable
    data class Hit(
        override val actingClassType: CharacterClassType,
        val total: Int,
        val doublesBonus: Int,
    ) : FlashCallout

    @Immutable
    data class Miss(val total: Int, override val actingClassType: CharacterClassType) : FlashCallout

    @Immutable
    data class Damage(
        val amount: Int,
        override val actingClassType: CharacterClassType? = null,
        val doublesBonus: Int = 0,
    ) : FlashCallout

    @Immutable
    data class Knockout(
        override val actingClassType: CharacterClassType? = null,
        val total: Int? = null,
    ) : FlashCallout

    @Immutable
    data object KillingMomentum : FlashCallout {
        override val actingClassType = CharacterClassType.Rogue
    }

    @Immutable
    data object Dodge : FlashCallout {
        override val actingClassType = CharacterClassType.Rogue
    }

    @Immutable
    data object SpellPotency : FlashCallout {
        override val actingClassType = CharacterClassType.Mage
    }

    @Immutable data object Doubles : FlashCallout

    @Immutable
    data object Impersonate : FlashCallout {
        override val actingClassType = CharacterClassType.Illusionist
    }

    @Immutable
    data object PreparedStrike : FlashCallout {
        override val actingClassType = CharacterClassType.Duelist
    }

    @Immutable
    data object AttackWhiffed : FlashCallout {
        override val actingClassType = CharacterClassType.Duelist
    }

    @Immutable
    data object Encore : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object AllIn : FlashCallout {
        override val actingClassType = CharacterClassType.Gambler
    }

    @Immutable
    data object AllInStrikeTwice : FlashCallout {
        override val actingClassType = CharacterClassType.Gambler
    }

    @Immutable
    data object Fizzle : FlashCallout {
        override val actingClassType = CharacterClassType.Necromancer
    }

    @Immutable
    data object Skeleton : FlashCallout {
        override val actingClassType = CharacterClassType.Necromancer
    }

    @Immutable
    data class SkeletonAndHeal(val necromancerName: String) : FlashCallout {
        override val actingClassType = CharacterClassType.Necromancer
    }

    @Immutable
    data class Golem(val necromancerName: String) : FlashCallout {
        override val actingClassType = CharacterClassType.Necromancer
    }

    @Immutable
    data object Teleport : FlashCallout {
        override val actingClassType = CharacterClassType.Mage
    }

    @Immutable
    data object Escaped : FlashCallout {
        override val actingClassType = CharacterClassType.Mage
    }

    @Immutable
    data object Caught : FlashCallout {
        override val actingClassType = CharacterClassType.Mage
    }

    @Immutable
    data object LuckyCoin : FlashCallout {
        override val actingClassType = CharacterClassType.Gambler
    }

    @Immutable
    data class AttackStopped(override val actingClassType: CharacterClassType) : FlashCallout

    @Immutable
    data class AttackContinues(val die: Int, override val actingClassType: CharacterClassType) :
        FlashCallout

    @Immutable
    data object Riposte : FlashCallout {
        override val actingClassType = CharacterClassType.Duelist
    }

    @Immutable
    data object Reversed : FlashCallout {
        override val actingClassType = CharacterClassType.Duelist
    }

    @Immutable
    data object CounterFailed : FlashCallout {
        override val actingClassType = CharacterClassType.Duelist
    }

    @Immutable
    data object Persuade : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object Charmed : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data class Redirected(val targetName: String) : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object MirrorImage : FlashCallout {
        override val actingClassType = CharacterClassType.Illusionist
    }

    @Immutable
    data class Reroll(val rawReroll: Int) : FlashCallout {
        override val actingClassType = CharacterClassType.Illusionist
    }

    @Immutable
    data object HolyShield : FlashCallout {
        override val actingClassType = CharacterClassType.Paladin
    }

    @Immutable
    data object Saved : FlashCallout {
        override val actingClassType = CharacterClassType.Paladin
    }

    @Immutable
    data object StruckDown : FlashCallout {
        override val actingClassType = CharacterClassType.Paladin
    }

    @Immutable
    data object HolyWrath : FlashCallout {
        override val actingClassType = CharacterClassType.Paladin
    }

    @Immutable data class NewRound(val roundNumber: String) : FlashCallout

    @Immutable
    data class ChoseClass(val className: String, override val actingClassType: CharacterClassType) :
        FlashCallout

    @Immutable data class Joined(val playerName: String) : FlashCallout

    @Immutable
    data class TurnStart(val className: String, override val actingClassType: CharacterClassType) :
        FlashCallout

    @Immutable
    data class TeleportRoll(val die: Int) : FlashCallout {
        override val actingClassType = CharacterClassType.Mage
    }

    @Immutable
    data object HighRoller : FlashCallout {
        override val actingClassType = CharacterClassType.Gambler
    }

    @Immutable
    data object Lulled : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object EncoreAgain : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object NoEncore : FlashCallout {
        override val actingClassType = CharacterClassType.Bard
    }

    @Immutable
    data object MirrorHolds : FlashCallout {
        override val actingClassType = CharacterClassType.Illusionist
    }
}

/** The flash call-out for a message, or null when the message is log-only. */
fun ArenaMessageState.toFlashCallout() =
    when (this) {
        is ArenaMessageState.NewRound,
        is ArenaMessageState.ChoseCharacterClass,
        is ArenaMessageState.ConnectedToArena,
        is ArenaMessageState.NewTurn -> null

        is ArenaMessageState.NormalRoll.Attack -> null

        is ArenaMessageState.NormalRoll.Outcome ->
            when (type) {
                NormalRollMessageType.ArmorClassHit ->
                    FlashCallout.Hit(
                        actingClassType = attackerClassType,
                        total = total,
                        doublesBonus = bonus,
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    FlashCallout.Miss(total = total, actingClassType = attackerClassType)

                NormalRollMessageType.Damage ->
                    FlashCallout.Damage(
                        amount = total,
                        actingClassType = attackerClassType,
                        doublesBonus = bonus,
                    )

                NormalRollMessageType.Knockout ->
                    FlashCallout.Knockout(actingClassType = attackerClassType, total = total)
            }

        is ArenaMessageState.KillingMomentum,
        is ArenaMessageState.Dodge -> null

        is ArenaMessageState.Teleport.Trigger -> null

        is ArenaMessageState.Teleport.Roll -> FlashCallout.TeleportRoll(die = singleDie)

        is ArenaMessageState.Teleport.Result ->
            if (didDefenderEscape) FlashCallout.Escaped else FlashCallout.Caught

        is ArenaMessageState.LuckyCoin.Trigger -> null

        is ArenaMessageState.LuckyCoin.HighRollerTrigger -> null

        is ArenaMessageState.LuckyCoin.Flip ->
            if (didStopAttack) {
                FlashCallout.AttackStopped(actingClassType = CharacterClassType.Gambler)
            } else {
                FlashCallout.AttackContinues(
                    die = singleDie,
                    actingClassType = CharacterClassType.Gambler,
                )
            }

        is ArenaMessageState.LuckyCoin.HighRollerDamage ->
            if (isKnockout) {
                FlashCallout.Knockout(actingClassType = CharacterClassType.Gambler)
            } else {
                FlashCallout.Damage(
                    amount = singleDie,
                    actingClassType = CharacterClassType.Gambler,
                )
            }

        is ArenaMessageState.Riposte.Trigger -> null

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

        is ArenaMessageState.Persuade.Trigger -> null

        is ArenaMessageState.Persuade.StrumLute ->
            if (isSuccessful) {
                FlashCallout.AttackStopped(actingClassType = CharacterClassType.Bard)
            } else {
                FlashCallout.AttackContinues(
                    die = diceRoll,
                    actingClassType = CharacterClassType.Bard,
                )
            }

        is ArenaMessageState.Persuade.CharmActivation,
        is ArenaMessageState.Persuade.Lull,
        is ArenaMessageState.Persuade.Charm -> null

        is ArenaMessageState.Encore.Trigger -> null

        is ArenaMessageState.Encore.Roll ->
            if (isSuccessful) FlashCallout.EncoreAgain else FlashCallout.NoEncore

        is ArenaMessageState.Impersonate -> null

        is ArenaMessageState.MirrorImage.Trigger -> null

        is ArenaMessageState.MirrorImage.Pass ->
            when (messageType) {
                NormalRollMessageType.ArmorClassHit ->
                    FlashCallout.Hit(
                        actingClassType = attackerClassType,
                        total = total,
                        doublesBonus = doublesBonus,
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    FlashCallout.Miss(total = total, actingClassType = attackerClassType)

                NormalRollMessageType.Damage ->
                    FlashCallout.Damage(
                        amount = total,
                        actingClassType = attackerClassType,
                        doublesBonus = doublesBonus,
                    )

                NormalRollMessageType.Knockout ->
                    FlashCallout.Knockout(actingClassType = attackerClassType, total = total)
            }

        is ArenaMessageState.MirrorImage.TemptFate -> FlashCallout.Reroll(rawReroll = rawReroll)

        is ArenaMessageState.SpellPotency -> FlashCallout.SpellPotency

        is ArenaMessageState.HolyShield.Trigger -> null

        is ArenaMessageState.HolyShield.Prayer ->
            if (didSurvive) FlashCallout.Saved else FlashCallout.StruckDown

        is ArenaMessageState.HolyShield.Wrath -> FlashCallout.HolyWrath

        is ArenaMessageState.AllIn ->
            when (outcome) {
                AllInOutcome.SelfKnockout ->
                    FlashCallout.Knockout(actingClassType = CharacterClassType.Gambler)

                AllInOutcome.DoubleAttack -> FlashCallout.AllInStrikeTwice

                AllInOutcome.NoEffect -> FlashCallout.AllIn
            }

        is ArenaMessageState.PrepareAttack ->
            if (isSuccessful) FlashCallout.PreparedStrike else FlashCallout.AttackWhiffed

        is ArenaMessageState.Doubles -> FlashCallout.Doubles
    }
