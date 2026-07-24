package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import org.jetbrains.compose.resources.DrawableResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.bard_avatar
import sorcerersandspirits.composeapp.generated.resources.bard_bust
import sorcerersandspirits.composeapp.generated.resources.duelist_avatar
import sorcerersandspirits.composeapp.generated.resources.duelist_bust
import sorcerersandspirits.composeapp.generated.resources.gambler_avatar
import sorcerersandspirits.composeapp.generated.resources.gambler_bust
import sorcerersandspirits.composeapp.generated.resources.illusionist_avatar
import sorcerersandspirits.composeapp.generated.resources.illusionist_bust
import sorcerersandspirits.composeapp.generated.resources.mage_avatar
import sorcerersandspirits.composeapp.generated.resources.mage_bust
import sorcerersandspirits.composeapp.generated.resources.necromancer_avatar
import sorcerersandspirits.composeapp.generated.resources.necromancer_bust
import sorcerersandspirits.composeapp.generated.resources.paladin_avatar
import sorcerersandspirits.composeapp.generated.resources.paladin_bust
import sorcerersandspirits.composeapp.generated.resources.rogue_avatar
import sorcerersandspirits.composeapp.generated.resources.rogue_bust

internal fun CharacterClassType.toBustResource(): DrawableResource =
    when (this) {
        CharacterClassType.Rogue -> Res.drawable.rogue_bust
        CharacterClassType.Gambler -> Res.drawable.gambler_bust
        CharacterClassType.Duelist -> Res.drawable.duelist_bust
        CharacterClassType.Necromancer -> Res.drawable.necromancer_bust
        CharacterClassType.Bard -> Res.drawable.bard_bust
        CharacterClassType.Illusionist -> Res.drawable.illusionist_bust
        CharacterClassType.Mage -> Res.drawable.mage_bust
        CharacterClassType.Paladin -> Res.drawable.paladin_bust
        CharacterClassType.None -> Res.drawable.rogue_bust
    }

internal fun CharacterClassType.toAvatarResource(): DrawableResource =
    when (this) {
        CharacterClassType.Rogue -> Res.drawable.rogue_avatar
        CharacterClassType.Gambler -> Res.drawable.gambler_avatar
        CharacterClassType.Duelist -> Res.drawable.duelist_avatar
        CharacterClassType.Necromancer -> Res.drawable.necromancer_avatar
        CharacterClassType.Bard -> Res.drawable.bard_avatar
        CharacterClassType.Illusionist -> Res.drawable.illusionist_avatar
        CharacterClassType.Mage -> Res.drawable.mage_avatar
        CharacterClassType.Paladin -> Res.drawable.paladin_avatar
        CharacterClassType.None -> Res.drawable.rogue_avatar
    }

// TODO: refactor this logic - don't need all these extra enums and mapping
enum class ClassColorGroup {
    Red,
    Yellow,
    Green,
    Blue,
}

@Immutable class ClassGradient(val topStop: Color, val bottomStop: Color)

private fun CharacterClassType.toColorGroup(): ClassColorGroup =
    when (this) {
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

fun CharacterClassType.toGradient(): ClassGradient =
    when (toColorGroup()) {
        ClassColorGroup.Red -> RED
        ClassColorGroup.Yellow -> YELLOW
        ClassColorGroup.Green -> GREEN
        ClassColorGroup.Blue -> BLUE
    }

fun CharacterClassType.toColor(): Color = toGradient().topStop

fun DieColor.toColor(): Color? =
    when (this) {
        DieColor.NormalAttack -> null
        DieColor.Disabled -> null
        DieColor.Rogue -> CharacterClassType.Rogue.toColor()
        DieColor.Gambler -> CharacterClassType.Gambler.toColor()
        DieColor.Duelist -> CharacterClassType.Duelist.toColor()
        DieColor.Necromancer -> CharacterClassType.Necromancer.toColor()
        DieColor.Bard -> CharacterClassType.Bard.toColor()
        DieColor.Illusionist -> CharacterClassType.Illusionist.toColor()
        DieColor.Mage -> CharacterClassType.Mage.toColor()
        DieColor.Paladin -> CharacterClassType.Paladin.toColor()
    }

fun classFamilyColors(): List<Color> =
    listOf(RED.topStop, YELLOW.topStop, GREEN.topStop, BLUE.topStop)

private val RED =
    ClassGradient(topStop = Color(color = 0xFF9D2920), bottomStop = Color(color = 0xFF581310))
private val YELLOW =
    ClassGradient(topStop = Color(color = 0xFFDAA73B), bottomStop = Color(color = 0xFF74591F))
private val GREEN =
    ClassGradient(topStop = Color(color = 0xFF4B5F0D), bottomStop = Color(color = 0xFF9BC51B))
private val BLUE =
    ClassGradient(topStop = Color(color = 0xFF1D406D), bottomStop = Color(color = 0xFF387CD3))
