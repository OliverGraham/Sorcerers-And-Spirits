package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.minionAttackerIndexFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.SummonedMinions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_attack_button
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_speed
import sorcerersandspirits.composeapp.generated.resources.ic_user_player

private val statPillShape = RoundedCornerShape(size = 8.dp)
private val statPillIconSize = 24.dp
private const val ATTACKS_COUNT_LABEL = "attacksCount"

// TODO(SS-258): Continue renaming, refactoring CombatAnimationCue. Move things into separate files
// and such
@Composable
internal fun CombatAvatarColumn(
    topPlayer: PlayerState.Selected?,
    bottomPlayer: PlayerState.Selected?,
    animatingMessage: ArenaMessageState?,
    userPlayerId: String,
    attackerUserId: String,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    onDiceHomeBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SeatedAvatar(
            player = topPlayer,
            animatingMessage = animatingMessage,
            isTopSeat = true,
            userPlayerId = userPlayerId,
            attackerUserId = attackerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
        )

        DiceHomeSlot(onBoundsChanged = onDiceHomeBoundsChanged)

        SeatedAvatar(
            player = bottomPlayer,
            animatingMessage = animatingMessage,
            isTopSeat = false,
            userPlayerId = userPlayerId,
            attackerUserId = attackerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
        )
    }
}

// TODO(SS-258): Refactor data that's passed in. Confusing the pass in userPlayerId for the purpose
// its used
// for
@Composable
private fun ColumnScope.SeatedAvatar(
    player: PlayerState.Selected?,
    animatingMessage: ArenaMessageState?,
    isTopSeat: Boolean,
    userPlayerId: String,
    attackerUserId: String,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
) {
    if (player != null) {
        LargeAvatar(
            player = player,
            animatingMessage = animatingMessage,
            isTopSeat = isTopSeat,
            isUserPlayer = player.userId == userPlayerId,
            isAttacker = player.userId == attackerUserId,
            onInspect = onInspect,
            onAvatarBoundsChanged = onAvatarBoundsChanged,
            modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
        )
    } else {
        // TODO(SS-234): If the player is null, then show the not-yet-created "Choose your target"
        // avatar
        Spacer(modifier = Modifier.weight(weight = 1f))
    }
}

@Composable
private fun DiceHomeSlot(onBoundsChanged: (Rect) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier.fillMaxWidth().height(height = 56.dp).onGloballyPositioned {
            onBoundsChanged(it.boundsInRoot())
        },
    )
}

@Composable
private fun LargeAvatar(
    player: PlayerState.Selected,
    animatingMessage: ArenaMessageState?,
    isTopSeat: Boolean,
    isUserPlayer: Boolean,
    isAttacker: Boolean,
    onInspect: (String) -> Unit,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .sharedBounds(key = player.userId)
            .clickable { onInspect(player.userId) }
            .onGloballyPositioned {
                onAvatarBoundsChanged(player.userId, it.boundsInRoot())
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        GestureClassAvatar(
            animatingMessage = animatingMessage,
            characterClass = player.characterClass,
            userId = player.userId,
            isTopSeat = isTopSeat,
            modifier = Modifier.fillMaxSize(),
        )

        val summonedMinions = player.characterClass.summonedMinions
        if (summonedMinions.isNotEmpty()) {
            SummonedMinions(
                minions = summonedMinions,
                attackingMinionIndex =
                animatingMessage?.minionAttackerIndexFor(attackerUserId = player.userId),
                animatingMessage = animatingMessage,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AvatarStatsBar(
            characterClass = player.characterClass,
            isUserPlayer = isUserPlayer,
            isAttacker = isAttacker,
            classColor = player.characterClass.type.toColor(),
            modifier = Modifier.align(alignment = Alignment.CenterStart),
        )
    }
}

@Composable
private fun AvatarStatsBar(
    characterClass: CharacterClassState,
    isUserPlayer: Boolean,
    isAttacker: Boolean,
    classColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        if (isUserPlayer) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_user_player),
                contentDescription = null,
                modifier = Modifier.size(size = 44.dp),
            )
        }

        StatPill(
            icon = Res.drawable.ic_speed,
            value = characterClass.speed.toString(),
            classColor = classColor,
        )
        StatPill(
            icon = Res.drawable.ic_armor_class,
            value = characterClass.armorClassDisplay,
            classColor = classColor,
        )
        StatPill(
            icon = Res.drawable.ic_hit_points,
            value = characterClass.hitPointsDisplay,
            classColor = classColor,
        )

        AnimatedVisibility(visible = isAttacker) {
            AttacksStatPill(count = characterClass.attacksRemaining, classColor = classColor)
        }
    }
}

@Composable
private fun StatPill(
    icon: DrawableResource,
    value: String,
    classColor: Color,
    modifier: Modifier = Modifier,
) {
    StatPillFrame(icon = icon, classColor = classColor, modifier = modifier) {
        StatPillValue(value = value)
    }
}

@Composable
private fun AttacksStatPill(count: Int, classColor: Color, modifier: Modifier = Modifier) {
    StatPillFrame(
        icon = Res.drawable.ic_attack_button,
        classColor = classColor,
        modifier = modifier,
    ) {
        AnimatedContent(targetState = count, label = ATTACKS_COUNT_LABEL) { attacks ->
            StatPillValue(value = attacks.toString())
        }
    }
}

@Composable
private fun StatPillFrame(
    icon: DrawableResource,
    classColor: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier =
        modifier
            .clip(shape = statPillShape)
            .background(color = classColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = icon),
            contentDescription = null,
            modifier = Modifier.size(size = statPillIconSize),
        )
        value()
    }
}

@Composable
private fun StatPillValue(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}
