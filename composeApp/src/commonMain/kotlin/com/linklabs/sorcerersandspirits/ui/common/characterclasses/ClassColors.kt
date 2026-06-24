package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

// The single source of per-class colors for all class UI (head icon, wheel, future Arena views).
// TODO(SS-084): provisional values — the final per-class colors come from Figma. Update only here.

/** The four class color families. Two classes share each family. */
enum class ClassColorGroup {
    Red,
    Yellow,
    Green,
    Blue,
}

/** A two-stop gradient (light at the top, dark at the bottom) for a class color family. */
@Immutable class ClassGradient(val light: Color, val dark: Color)

/** The color family a class belongs to. */
fun classColorGroup(type: CharacterClassType): ClassColorGroup =
    when (type) {
        CharacterClassType.Rogue,
        CharacterClassType.Duelist -> ClassColorGroup.Red

        CharacterClassType.Gambler,
        CharacterClassType.Bard -> ClassColorGroup.Yellow

        CharacterClassType.Necromancer,
        CharacterClassType.Paladin -> ClassColorGroup.Green

        CharacterClassType.Illusionist,
        CharacterClassType.Mage -> ClassColorGroup.Blue

        CharacterClassType.None -> ClassColorGroup.Red
    }

/** The class color gradient, used where a class is rendered with a fill (e.g. the head icon). */
fun classGradient(type: CharacterClassType): ClassGradient =
    when (classColorGroup(type = type)) {
        ClassColorGroup.Red -> ClassGradient(light = RED_LIGHT, dark = RED_DARK)
        ClassColorGroup.Yellow -> ClassGradient(light = YELLOW_LIGHT, dark = YELLOW_DARK)
        ClassColorGroup.Green -> ClassGradient(light = GREEN_LIGHT, dark = GREEN_DARK)
        ClassColorGroup.Blue -> ClassGradient(light = BLUE_LIGHT, dark = BLUE_DARK)
    }

/**
 * The single representative shade for a class, used where one color is needed (e.g. wheel spokes).
 */
fun classColor(type: CharacterClassType): Color =
    when (type) {
        CharacterClassType.Rogue -> Color(color = 0xFF9D2920)
        CharacterClassType.Gambler -> Color(color = 0xFFDAA73B)
        CharacterClassType.Duelist -> Color(color = 0xFF852B1A)
        CharacterClassType.Necromancer -> Color(color = 0xFF49533B)
        CharacterClassType.Bard -> Color(color = 0xFFE8C53A)
        CharacterClassType.Illusionist -> Color(color = 0xFF1F3A5D)
        CharacterClassType.Mage -> Color(color = 0xFF243346)
        CharacterClassType.Paladin -> Color(color = 0xFF3C7A3C)
        CharacterClassType.None -> Color(color = 0xFF654E35)
    }

private val RED_LIGHT = Color(color = 0xFF9D2920)
private val RED_DARK = Color(color = 0xFF581310)
private val YELLOW_LIGHT = Color(color = 0xFFDAA73B)
private val YELLOW_DARK = Color(color = 0xFF7A5E14)
private val GREEN_LIGHT = Color(color = 0xFF3C7A3C)
private val GREEN_DARK = Color(color = 0xFF1E3D1E)
private val BLUE_LIGHT = Color(color = 0xFF1F3A5D)
private val BLUE_DARK = Color(color = 0xFF0E1C2E)
