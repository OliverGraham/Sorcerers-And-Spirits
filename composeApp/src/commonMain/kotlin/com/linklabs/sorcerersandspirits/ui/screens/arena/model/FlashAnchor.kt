package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface FlashAnchor {
    @Immutable data class Avatar(val userId: String) : FlashAnchor

    @Immutable data object Center : FlashAnchor
}

fun ArenaMessageState.toFlashAnchor(): FlashAnchor =
    when (this) {
        is ArenaMessageState.NewRound,
        is ArenaMessageState.ChoseCharacterClass,
        is ArenaMessageState.ConnectedToArena,
        is ArenaMessageState.NewTurn,
        is ArenaMessageState.Persuade.Lull,
        is ArenaMessageState.Teleport.Tie,
        is ArenaMessageState.Doubles -> FlashAnchor.Center

        is ArenaMessageState.NormalRoll -> FlashAnchor.Avatar(userId = defenderUserId)

        is ArenaMessageState.Teleport.Roll -> FlashAnchor.Avatar(userId = rollerUserId)

        is ArenaMessageState.KillingMomentum -> FlashAnchor.Avatar(userId = attackerUserId)

        is ArenaMessageState.Dodge -> FlashAnchor.Avatar(userId = dodgerUserId)

        is ArenaMessageState.Teleport -> FlashAnchor.Avatar(userId = mageUserId)

        is ArenaMessageState.LuckyCoin.HighRollerDamage ->
            FlashAnchor.Avatar(userId = attackerUserId)

        is ArenaMessageState.LuckyCoin -> FlashAnchor.Avatar(userId = gamblerUserId)

        is ArenaMessageState.Riposte.Trigger -> FlashAnchor.Avatar(userId = duelistUserId)

        is ArenaMessageState.Riposte.EnGarde -> FlashAnchor.Avatar(userId = duelistUserId)

        is ArenaMessageState.SummonMinion -> FlashAnchor.Avatar(userId = necroUserId)

        is ArenaMessageState.Persuade.Trigger -> FlashAnchor.Avatar(userId = bardUserId)

        is ArenaMessageState.Persuade.StrumLute -> FlashAnchor.Avatar(userId = bardUserId)

        is ArenaMessageState.Persuade.CharmActivation -> FlashAnchor.Avatar(userId = bardUserId)

        is ArenaMessageState.Persuade.Charm -> FlashAnchor.Avatar(userId = bardUserId)

        is ArenaMessageState.Encore -> FlashAnchor.Avatar(userId = bardUserId)

        is ArenaMessageState.Impersonate -> FlashAnchor.Avatar(userId = illusionistUserId)

        is ArenaMessageState.MirrorImage -> FlashAnchor.Avatar(userId = illusionistUserId)

        is ArenaMessageState.SpellPotency -> FlashAnchor.Avatar(userId = mageUserId)

        is ArenaMessageState.HolyShield.Wrath -> FlashAnchor.Avatar(userId = attackerUserId)

        is ArenaMessageState.HolyShield -> FlashAnchor.Avatar(userId = paladinUserId)

        is ArenaMessageState.AllIn -> FlashAnchor.Avatar(userId = gamblerUserId)

        is ArenaMessageState.PrepareAttack -> FlashAnchor.Avatar(userId = duelistUserId)
    }
