package com.linklabs.sorcerersandspirits.ui.screens.arena.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.common.animation.LottieFx
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState

private const val COMBAT_FX_DIR = "files/combat_fx"

/**
 * Plays the full-bleed effect for the current [latestMessage]. Drop this on top of the combat
 * content (e.g. in a [Box] inside `ArenaCombat`) — it draws nothing when there is no message or the
 * message has no visual (lobby/round/turn banners), so it is safe to always compose.
 *
 * Avatar-anchored gestures live with the avatars themselves: feed
 * `latestMessage?.toCombatAnimation()?.attackerGesture` / `defenderGesture` (keyed on the message
 * id) into [CombatAvatar] at each player's position.
 */
@Composable
internal fun CombatAnimationOverlay(
    latestMessage: ArenaMessageState?,
    modifier: Modifier = Modifier,
) {
    val animation =
        remember(latestMessage) {
            CombatAnimation(fx = CombatFx.Test, attackerGesture = AvatarGesture.Shake)
            // latestMessage?.toCombatAnimation()
        }
    val assetFileName = animation?.fx?.assetFileName

    AnimatedVisibility(
        visible = assetFileName != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (assetFileName != null) {
                // `latestMessage?.id` as part of the key would force a restart per event; the
                // assetPath key already restarts when the effect changes.
                LottieFx(assetPath = "$COMBAT_FX_DIR/$assetFileName")
            }
        }
    }
}
