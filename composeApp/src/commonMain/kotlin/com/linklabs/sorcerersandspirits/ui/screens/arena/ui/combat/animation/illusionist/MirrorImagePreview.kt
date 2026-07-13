package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.MirrorImagePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.GestureClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.rememberAvatarGesture
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

/**
 * Interactive previews for the Illusionist's Mirror Image (SS-209). Unlike the overlay-driven
 * effects, this one lives on the seated avatar itself, so its preview stands a real
 * [GestureClassAvatar] on the arena background and replays a Split-then-Resolve cue loop through it
 * — the Illusionist splits into two, one copy is struck and poofs, and the real one reforms and
 * reacts to the outcome. One preview per outcome: a dodged miss, a landed hit, and a knockout.
 */
@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MirrorImageMissPreview() {
    MirrorImageSeatPreview(outcome = NormalRollMessageType.ArmorClassMiss)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MirrorImageHitPreview() {
    MirrorImageSeatPreview(outcome = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MirrorImageKnockoutPreview() {
    MirrorImageSeatPreview(outcome = NormalRollMessageType.Knockout)
}

@Composable
private fun MirrorImageSeatPreview(outcome: NormalRollMessageType) {
    val classState = previewCharacterClassState(type = CharacterClassType.Illusionist)
    val cue =
        rememberReplayingCues(replayKey = outcome) { iteration ->
            listOf(
                CombatAnimationCue.MirrorImage(
                    id = "mirror-split-$iteration",
                    illusionistUserId = PREVIEW_ACTOR_ID,
                    phase = MirrorImagePhase.Split,
                    messageType = null,
                ),
                CombatAnimationCue.MirrorImage(
                    id = "mirror-resolve-$iteration",
                    illusionistUserId = PREVIEW_ACTOR_ID,
                    phase = MirrorImagePhase.Resolve,
                    messageType = outcome,
                ),
            )
        }
    val gestureState =
        rememberAvatarGesture(animationCue = cue, userId = PREVIEW_ACTOR_ID, isTopSeat = false)

    SorcerersAndSpiritsTheme {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .paint(
                        painter = painterResource(resource = Res.drawable.arena_background),
                        contentScale = ContentScale.Crop,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            GestureClassAvatar(
                characterClass = classState,
                gestureState = gestureState,
                modifier = Modifier.size(width = 150.dp, height = 200.dp),
            )
        }
    }
}
