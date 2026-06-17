package com.linklabs.sorcerersandspirits.ui.screens.arena.animation

import androidx.compose.runtime.Immutable

/**
 * A decoupled, asset-agnostic description of what should be shown on screen for a single arena
 * event. The domain emits an [com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage];
 * the UI maps the resulting
 * [com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState] to one of these (see
 * `ArenaMessageState.toCombatAnimation`). Nothing here knows about a specific animation file, so
 * the asset backing a cue can be swapped (Lottie now, Rive later) without touching the mapping.
 */
@Immutable
data class CombatAnimation(
    val fx: CombatFx,
    val attackerGesture: AvatarGesture = AvatarGesture.None,
    val defenderGesture: AvatarGesture = AvatarGesture.None,
)

/**
 * A full-screen / anchored effect overlay, currently backed by a Lottie clip rendered with
 * Compottie. [assetFileName] is resolved against `composeResources/files/combat_fx/`. A `null` file
 * name means "no overlay" (the cue is carried purely by avatar motion and/or the message text).
 */
enum class CombatFx(val assetFileName: String?) {
    Test(assetFileName = "standupRogue.json"),
    None(assetFileName = null),
    Slash(assetFileName = "slash.json"),
    ArmorBlock(assetFileName = "armor_block.json"),
    Miss(assetFileName = "miss.json"),
    Impact(assetFileName = "impact.json"),
    Knockout(assetFileName = "knockout.json"),
    Heal(assetFileName = "heal.json"),
    Shield(assetFileName = "shield.json"),
    Evade(assetFileName = "evade.json"),
    Summon(assetFileName = "summon.json"),
    MagicBurst(assetFileName = "magic_burst.json"),
    Cast(assetFileName = "cast.json"),
    DiceRoll(assetFileName = "dice_roll.json"),
}

/**
 * Lightweight motion applied to an avatar via Compose ([CombatAvatar]). These are intentionally
 * library-free so basic "the avatar reacts" motion works today; individual avatars can later be
 * promoted to Rive state machines without changing this contract.
 */
enum class AvatarGesture {
    None,
    Lunge,
    Recoil,
    Shake,
    Cast,
    Collapse,
    Cheer,
}
