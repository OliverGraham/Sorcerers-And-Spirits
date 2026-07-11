package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlin.math.PI
import kotlin.math.sin
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points

private const val SPLASH_DURATION_MILLIS = 900
private const val SPLASH_RISE_FRACTION = 0.45f
private const val SPLASH_FADE_IN_FRACTION = 0.18f
private const val SPLASH_FADE_OUT_START = 0.68f
private const val INDICATOR_TEXT_FRACTION = 0.17f
private const val INDICATOR_ICON_FRACTION = 0.2f

private val INDICATOR_COLOR = Color(color = 0xFFE23B3B)

/**
 * One Spell Potency splash landing on a class-head icon (SS-214). [progress] runs the whole
 * decrement so the head's hit-point value swells and reddens via [hitPointsEmphasis] while a
 * floating red "-[splashDamage]" rises off the icon. [splashDamage] is null between casts, when
 * nothing is shown.
 */
@Stable
internal class SpellPotencySplashState {
    val progress = Animatable(initialValue = 0f)

    var splashDamage by mutableStateOf<Int?>(value = null)
        private set

    val hitPointsEmphasis: Float
        get() = sin(x = progress.value * PI.toFloat())

    suspend fun play(damage: Int) {
        splashDamage = damage
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = SPLASH_DURATION_MILLIS, easing = LinearEasing),
        )
        splashDamage = null
    }

    suspend fun cancel() {
        splashDamage = null
        progress.snapTo(targetValue = 0f)
    }
}

/**
 * Drives the splash decrement for one head-icon [player]: on each new Spell Potency cue that
 * splashes this conscious opponent, it plays the decrement once. A cue already live at first
 * composition, or one that does not splash this player, leaves the head untouched.
 */
@Composable
internal fun rememberSpellPotencySplash(
    animationCue: CombatAnimationCue?,
    player: PlayerState.Selected,
): SpellPotencySplashState {
    val state = remember { SpellPotencySplashState() }
    var lastPlayedCueId by remember { mutableStateOf(value = animationCue?.id) }

    LaunchedEffect(key1 = animationCue) {
        val splashDamage = animationCue.spellPotencySplashFor(player = player)
        if (splashDamage == null) {
            state.cancel()
            lastPlayedCueId = animationCue?.id
            return@LaunchedEffect
        }
        if (animationCue?.id == lastPlayedCueId) return@LaunchedEffect
        lastPlayedCueId = animationCue?.id
        state.play(damage = splashDamage)
    }
    return state
}

/**
 * The splash damage this Spell Potency cue lands on [player]'s head icon, or null when the cue is
 * something else or does not reach this player. Membership is the exact set the cast splashed —
 * every opponent conscious when it resolved — so a player knocked out by the splash still shows the
 * decrement while a player already down before the cast does not.
 */
private fun CombatAnimationCue?.spellPotencySplashFor(player: PlayerState.Selected): Int? {
    val cue = this as? CombatAnimationCue.SpellPotency ?: return null
    val wasSplashed = player.userId in cue.splashTargetUserIds
    if (!wasSplashed) return null
    return cue.splashDamage.takeIf { it > 0 }
}

/**
 * The floating red "-X" and hit-point icon that rises off a head as its Spell Potency splash lands
 * (SS-214), sized to the [headWidth] it sits beside. Draws nothing between casts.
 */
@Composable
internal fun SpellPotencySplashIndicator(
    splash: SpellPotencySplashState,
    headWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val splashDamage = splash.splashDamage ?: return
    val progress = splash.progress.value
    val rise = headWidth * SPLASH_RISE_FRACTION * progress

    Row(
        modifier =
            modifier.offset(y = -rise).alpha(alpha = splashIndicatorAlpha(progress = progress)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "-$splashDamage",
            color = INDICATOR_COLOR,
            fontWeight = FontWeight.Bold,
            fontSize = (headWidth.value * INDICATOR_TEXT_FRACTION).sp,
        )
        Image(
            painter = painterResource(resource = Res.drawable.ic_hit_points),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = INDICATOR_COLOR),
            modifier = Modifier.size(size = headWidth * INDICATOR_ICON_FRACTION),
        )
    }
}

private fun splashIndicatorAlpha(progress: Float): Float =
    when {
        progress < SPLASH_FADE_IN_FRACTION -> progress / SPLASH_FADE_IN_FRACTION

        progress > SPLASH_FADE_OUT_START ->
            ((1f - progress) / (1f - SPLASH_FADE_OUT_START)).coerceAtLeast(minimumValue = 0f)

        else -> 1f
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun SpellPotencySplashPreview() {
    val headIconPlayers =
        listOf(
            PlayerState.Selected(
                userId = "rogue",
                displayName = "Rogue",
                characterClass = previewCharacterClassState(type = CharacterClassType.Rogue),
            ),
            PlayerState.Selected(
                userId = "bard",
                displayName = "Bard",
                characterClass = previewCharacterClassState(type = CharacterClassType.Bard),
            ),
        )
    val animationCue =
        rememberReplayingCue(replayKey = Unit) { iteration ->
            CombatAnimationCue.SpellPotency(
                id = "splash-$iteration",
                mageUserId = "mage",
                mainTargetUserId = "target",
                mainDamage = 8,
                splashDamage = 3,
                splashTargetUserIds = headIconPlayers.map { it.userId },
            )
        }
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(
            value = LocalCharacterClassCopy provides previewCharacterClassCopy()
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    SharedTransitionProvider(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                    ) {
                        ClassIcons(
                            headIconPlayers = headIconPlayers,
                            userPlayerId = "rogue",
                            isTargetable = false,
                            onSelectTarget = {},
                            onInspect = {},
                            animationCue = animationCue,
                            modifier = Modifier.size(width = 140.dp, height = 520.dp),
                        )
                    }
                }
            }
        }
    }
}
