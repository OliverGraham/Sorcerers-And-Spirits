package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState

// TODO: Before Clauding make sure everything here is clear and complete

@Composable
internal fun ColumnScope.NewArenaCombat(state: ArenaState.Combat, modifier: Modifier = Modifier) {
    // TODO: When there's no target, tapping a head will transition it to the opponent area
    //       If there already is a target, then the current target goes into Head form while
    //       the newly targeted head transitions into the opponent area
    //       Think everything just needs to be wrapped in the SharedTransitionLayout +
    // AnimateContent
    //          See ArenaCombat

    // TODO: How about the Dice rolling over everything?

    // TopRow - Left is MessagesIcon
    //   Row is golden like in Figma
    //   onTap opens messages and expands downward, covering FlowRow of Heads and stopping at Row of
    // FullSize Avatars
    //   Number of new messages is in center of icon
    //       Continuously loops through the 4 colors

    // SharedTransition of Heads
    //  Heads is FlowRow
    //
    // TODO: Tapping on FlowRowHead when not targeting to just get more info
    //       Expand head to reveal full Avatar, covering all content nearby?
    //       Have Drawer on right that opens with Full Avatar and stats?

    //  Row of FullSize Avatars
    //  On left is active player's stats Column then Active Player FullSizeAvatar
    //  On right is their current target - with FullSizeAvatar then StatsColumn
    //  In middle, at bottom, is Dice - if Dice is part of this structure, will be easier to keep
    // correct size
    // TODO: StatsColumn needs to be positioned correctly so each stat can be opened up and examined
    //       with short explanation

    // TODO: Need to add animations for Avatars - update existing branch and use?
    //       Likely, short messages like "Hit!" or "Miss!" should be shown on front in center
    //          Brief, stylized animation. Full message would be in log
    //          Could be a property of the ArenaMessage - short text?

    //  Dice is a Column
    //     DiceTray (the landing point)
    //        Dice can fly anywhere across the entire screen, when the roll is finished, they
    // smoothly
    //         animate back to the DiceTray
    //     Row
    //       AbilityButton  - can be Activated Ability or defensive ability
    //          When rolling only a single die, one in the tray should look disabled and not move
    // during rolling
    //       AttackButton
    //       ImpersonateButton
}

@Composable
private fun TopHalfContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // FlowRow, positioned BELOW Messages.
        // Messages - so when it opens, it opens on top of FlowRow
    }
}

@Composable private fun BottomHalfContent(modifier: Modifier = Modifier) {}
