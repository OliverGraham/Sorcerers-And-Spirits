package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The elemental flavors of the Mage's cast projectile (SS-198). Each is a focused bolt on a normal
 * attack and an amplified area-of-effect on Spell Potency (SS-210); the two never co-occur, since
 * the domain resolves a doubles roll as Spell Potency without a normal-roll message. Picked at
 * random per cast via [mageElementFor], so the framework grows toward six by adding entries here
 * and their draw branches — no caller changes.
 */
enum class MageSpellElement {
    Ice,
    Lightning,
    Fire,
}

/**
 * The element this cast renders, chosen deterministically from the cue [cueId] so every timing and
 * draw site resolves the same element for a given cast — the same keying the Illusionist uses to
 * borrow a weapon (SS-197). The pick spreads across whichever elements are currently built.
 */
internal fun mageElementFor(cueId: String): MageSpellElement {
    val elements = MageSpellElement.entries
    val index = ((cueId.hashCode() % elements.size) + elements.size) % elements.size
    return elements[index]
}

/**
 * A cue id that [mageElementFor] resolves to [element], so an interactive preview can lock onto one
 * element while still varying its id each [iteration] to replay the effect.
 */
internal fun mageElementPreviewCueId(element: MageSpellElement, iteration: Int): String {
    var candidate = iteration * 101
    while (mageElementFor(cueId = "mage-preview-$element-$candidate") != element) {
        candidate++
    }
    return "mage-preview-$element-$candidate"
}

/**
 * The colors one element is lit with: a near-white [core] at the hottest point, the [glow] that
 * carries the element's identity, a paler [trail] streaming behind the bolt, and the [atmosphere]
 * the amplified area-of-effect washes the arena in.
 */
@Immutable
internal data class MageElementPalette(
    val core: Color,
    val glow: Color,
    val trail: Color,
    val atmosphere: Color,
)

internal fun MageSpellElement.palette(): MageElementPalette =
    when (this) {
        MageSpellElement.Ice ->
            MageElementPalette(
                core = Color(color = 0xFFF2FBFF),
                glow = Color(color = 0xFF7FD8FF),
                trail = Color(color = 0xFFBEE9FF),
                atmosphere = Color(color = 0xFFAEE3FF),
            )

        MageSpellElement.Lightning ->
            MageElementPalette(
                core = Color(color = 0xFFF7F1FF),
                glow = Color(color = 0xFF9B6BFF),
                trail = Color(color = 0xFFCBB6FF),
                atmosphere = Color(color = 0xFF2B2740),
            )

        MageSpellElement.Fire ->
            MageElementPalette(
                core = Color(color = 0xFFFFF1B8),
                glow = Color(color = 0xFFFF832B),
                trail = Color(color = 0xFFFF4E28),
                atmosphere = Color(color = 0xFFFF3A1C),
            )
    }
