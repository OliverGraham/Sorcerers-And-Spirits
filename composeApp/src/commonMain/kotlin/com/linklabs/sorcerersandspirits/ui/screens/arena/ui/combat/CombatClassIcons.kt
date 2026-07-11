package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_user_player

private val USER_MARKER_ICON_SIZE = 24.dp
private val USER_MARKER_ICON_INSET = 4.dp

/**
 * The right column of collapsed class heads — every player not seated as a full avatar (SS-143).
 * Tapping a head targets it on the user's own turn, otherwise it opens the inspect drawer. The
 * local player's head carries the identity marker at the foot of its circle. [animationCue] drives
 * the Spell Potency splash decrement on the splashed opponents' heads (SS-214).
 */
@Composable
internal fun ClassIcons(
    headIconPlayers: List<PlayerState.Selected>,
    userPlayerId: String,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    animationCue: CombatAnimationCue?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        headIconPlayers.forEach { player ->
            ClassIcon(
                player = player,
                isUserPlayer = player.userId == userPlayerId,
                isTargetable = isTargetable,
                onSelectTarget = onSelectTarget,
                onInspect = onInspect,
                animationCue = animationCue,
            )
        }
    }
}

@Composable
private fun ClassIcon(
    player: PlayerState.Selected,
    isUserPlayer: Boolean,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    animationCue: CombatAnimationCue?,
    modifier: Modifier = Modifier,
) {
    val isConscious = player.characterClass.isConscious
    val isTargetingTap = isTargetable && isConscious

    // TODO: only call this function if there's a conscious character with SpellPotency
    val splash = rememberSpellPotencySplash(animationCue = animationCue, player = player)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val headWidth = minOf(a = maxWidth, b = maxHeight)

        ClassHeadIcon(
            characterClass = player.characterClass,
            isEnabled = isConscious,
            width = headWidth,
            hitPointsEmphasis = splash.hitPointsEmphasis,
            modifier =
                Modifier.sharedBounds(key = player.userId).clickable {
                    if (isTargetingTap) onSelectTarget(player.userId) else onInspect(player.userId)
                },
        )

        SpellPotencySplashIndicator(
            splash = splash,
            headWidth = headWidth,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
        )

        if (isUserPlayer) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_user_player),
                contentDescription = null,
                modifier =
                    Modifier.align(alignment = Alignment.BottomCenter)
                        .padding(bottom = USER_MARKER_ICON_INSET)
                        .size(size = USER_MARKER_ICON_SIZE),
            )
        }
    }
}
