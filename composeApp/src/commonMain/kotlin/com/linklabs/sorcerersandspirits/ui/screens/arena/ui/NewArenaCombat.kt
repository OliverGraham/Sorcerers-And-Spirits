package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

// TODO(SS-142): scaffold for the new two-half combat layout that replaces ArenaCombat. Already
// wired
//  into LoadedArenaScreen; thread the ArenaState.Combat target/roll contract through and migrate
// the
//  behavior from ArenaCombat into the segments below. Delete ArenaCombat.kt only once nothing is
//  missed.
@Composable
internal fun ColumnScope.NewArenaCombat(state: ArenaState.Combat, modifier: Modifier = Modifier) {
    // TODO(SS-144): When there's no target, tapping a ClassIcon will transition it to the opponent
    //           area. ClassAvatarDisplay(displayDirection = DisplayDirection.Right) is opponent
    // area.
    //       If there already is a target, then the current target goes into ClassIcon form while
    //       the newly targeted ClassIcon transitions into the opponent area as ClassAvatar
    //       Think everything just needs to be wrapped in the SharedTransitionLayout +
    //       AnimateContent. See ArenaCombat

    // TODO(SS-149): How about the Dice rolling over everything? What changes will need to be made
    // to
    //  facilitate that? After rolling, they must smoothly slide over to DiceHome()

    // TODO(SS-150): Tapping on ClassIcon of ClassIcons when it's not the user's turn or when
    // they're
    //  locked into a target and to just want to see more info about another class (maybe they're
    //  waiting for their opponent to roll and they want to tap around see things). Two options:
    //       1. Expand head to reveal full Avatar, covering all content nearby?
    //       2. Have Drawer on right that opens with Full Avatar and stats?

    // TODO(SS-148): Need to add animations for full-screen messages.
    //          short messages like "Hit!" or "Miss!" should be shown in center of entire screen.
    //          Brief, stylized animation (bounce, rotate, etc.). Full message would be in log.
    //          Could be a property of the ArenaMessage - short text?

    TopHalfContent(state = state)
    BottomHalfContent()
}

@Composable
private fun TopHalfContent(state: ArenaState.Combat, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // ClassIcons() need to be positioned BELOW Messages, but when Messages() expands, it covers
        // ClassIcons()
        ClassIcons(headIconPlayers = state.headIconPlayers)
        Messages()
    }
}

@Composable
private fun ClassIcons(headIconPlayers: List<PlayerState.Selected>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        headIconPlayers.forEach { player -> ClassIcon(player = player) }
    }
}

@Composable
private fun ClassIcon(player: PlayerState.Selected, modifier: Modifier = Modifier) {
    // TODO(SS-144): tag this with a userId-keyed sharedBounds/sharedElement so a tap can transition
    //  the head icon into the bottom-half targeted avatar (player.userId is the match key).
    ClassHeadIcon(characterClass = player.characterClass, modifier = modifier)
}

@Composable
private fun Messages(modifier: Modifier = Modifier) {
    // TODO(SS-147): On very left is Messages icon (use placeholder for now).
    //       onTap opens messages and expands downward, covering ClassIcons() and stopping at
    //          BottomHalfContent().
    //       Row is golden like in Figma and when expands the golden background covers things up
    //          estimate golden color for now and I'll modify later.
    //       Number of new messages is in center of icon.
    //          Each part of the number is one of the 4 class colors - each class color is always
    //          visible in the number. It animates the colors around so it looks like the colors
    //          are moving through the number, like a pulsing fashionContinuously loops through the
    //          4 colors.
}

@Composable
private fun BottomHalfContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Row {
            // On left is active player
            ClassAvatarDisplay(displayDirection = DisplayDirection.Left)
            DiceHome()

            // On right is active player's current target
            ClassAvatarDisplay(displayDirection = DisplayDirection.Right)
        }

        Buttons()
    }
}

@Composable
private fun ClassAvatarDisplay(displayDirection: DisplayDirection, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ClassAvatar()

        // TODO(SS-146): ClassAvatarStats will have expanding rows that open on top of ClassAvatar()
        ClassAvatarStats(displayDirection = displayDirection)
    }
}

@Composable
private fun ClassAvatarStats(displayDirection: DisplayDirection, modifier: Modifier = Modifier) {
    when (displayDirection) {
        DisplayDirection.Left -> {
            // TODO(SS-146): Row of Icon with stat, both with class color background.
            //       onTap is expand to the right, revealing a short explanation of the stat.
            //       For OffensiveAbility and DefensiveAbility, there's no stat, but the rest is the
            //  same.
        }

        DisplayDirection.Right -> {
            // TODO(SS-146): Opposite directions of above. Row stat and then Icon, and expands to
            //  left.
        }
    }
}

@Composable
private fun ClassAvatar(modifier: Modifier = Modifier) {
    // TODO(SS-145): Display full-size class png. Later, in another ticket, we'll have animations
    //       affect these pngs (there's a branch with some work already - maybe we'll merge it).
}

@Composable
private fun DiceHome(modifier: Modifier = Modifier) {
    // TODO(SS-149): This is where the Dice move to after rolling anywhere across the entire screen.
    //       When someone rolls a single die, one of the dice should become disabled while the other
    //       rolls and moves back here.
}

@Composable
private fun Buttons(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        AbilityButton()
        AttackButton()
        ImpersonateButton(shouldShowImpersonateButton = false)
    }
}

@Composable
private fun AbilityButton(modifier: Modifier = Modifier) {
    // TODO(SS-151): should be the user's ability, when it's their turn and they have an
    //  ActivatedAbility
    //  (OffensiveAbility icon) or when they're being attacked and their defensive ability
    //  (DefensiveAbility icon) requires a roll, or when they are participating in a TeleportRollOff
    //  (separate icon). The specific icons will be supplied at a later time and now a placeholder
    //  should be used.
    ArenaButton()
}

@Composable private fun AttackButton(modifier: Modifier = Modifier) {}

@Composable
private fun ImpersonateButton(shouldShowImpersonateButton: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = shouldShowImpersonateButton, modifier = modifier) {}
}

@Composable
private fun ArenaButton(modifier: Modifier = Modifier) {
    // TODO(SS-151): Should have a common style for each button, except that each button would have
    //       icons and in the case of the ImpersonateButton(), a label below saying "Impersonate" or
    //       "Impersonating: $className"
}

private enum class DisplayDirection {
    Left,
    Right,
}
