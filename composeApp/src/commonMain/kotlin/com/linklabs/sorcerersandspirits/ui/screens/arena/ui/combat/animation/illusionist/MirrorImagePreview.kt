package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.GestureClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar

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
    val avatars = rememberSoloAvatar()
    val bounds = avatars.boundsFor(PREVIEW_ACTOR_ID)
    val density = LocalDensity.current
    val classState = previewCharacterClassState(type = CharacterClassType.Illusionist)
    AnimationPreview(
        accentClass = CharacterClassType.Illusionist,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.MirrorImage.Pass(
                id = id,
                illusionistUserId = PREVIEW_ACTOR_ID,
                attackerClassName = "Rogue",
                attackerClassType = CharacterClassType.Rogue,
                messageType = outcome,
                baseRoll = 8,
                doublesBonus = 0,
                animation = ArenaAnimationState.FlashAndAvatar(),
                gestures = emptyMap(),
            )
        },
    ) { message ->
        GestureClassAvatar(
            animatingMessage = message,
            isTopSeat = false,
            userId = PREVIEW_ACTOR_ID,
            characterClass = classState,
            modifier =
                Modifier.offset(
                        x = with(density) { bounds.left.toDp() },
                        y = with(density) { bounds.top.toDp() },
                    )
                    .size(
                        width = with(density) { bounds.width.toDp() },
                        height = with(density) { bounds.height.toDp() },
                    ),
        )
    }
}
