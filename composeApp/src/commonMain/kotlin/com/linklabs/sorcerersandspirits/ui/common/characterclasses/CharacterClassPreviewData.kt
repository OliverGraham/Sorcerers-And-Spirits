package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import com.linklabs.sorcerersandspirits.domain.game.AbilityCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys

private const val PREVIEW_STAT = 5

private val PREVIEW_COPY_NAMES =
    mapOf(
        ClassCopyKeys.HEALTH to "Health",
        ClassCopyKeys.ARMOR to "Armor",
        ClassCopyKeys.SPEED to "Speed",
        ClassCopyKeys.KILLING_MOMENTUM to "Killing Momentum",
        ClassCopyKeys.ALL_IN to "All-In",
        ClassCopyKeys.PREPARE_ATTACK to "Prepare Attack",
        ClassCopyKeys.SUMMON_MINION to "Summon Minion",
        ClassCopyKeys.ENCORE to "Encore",
        ClassCopyKeys.IMPERSONATE to "Impersonate",
        ClassCopyKeys.SPELL_POTENCY to "Spell Potency",
        ClassCopyKeys.DODGE to "Dodge",
        ClassCopyKeys.LUCKY_COIN to "Lucky Coin",
        ClassCopyKeys.RIPOSTE to "Riposte",
        ClassCopyKeys.PERSUADE to "Persuade",
        ClassCopyKeys.MIRROR_IMAGE to "Mirror Image",
        ClassCopyKeys.TELEPORT to "Teleport",
        ClassCopyKeys.HOLY_SHIELD to "Holy Shield",
    )

internal fun previewCharacterClassCopy(): CharacterClassCopy = CharacterClassCopy(
    entries =
    PREVIEW_COPY_NAMES.mapValues { (_, name) ->
        AbilityCopy(name = name, long = "Sample description.", short = "Sample.")
    },
)

internal fun previewCharacterClassState(type: CharacterClassType): CharacterClassState = CharacterClass.create(
    type = type,
    hitPoints = PREVIEW_STAT,
    maxHitPoints = PREVIEW_STAT,
    armorClass = PREVIEW_STAT,
    speed = PREVIEW_STAT,
)
    .toState()

internal fun previewCharacterClassStates(): List<CharacterClassState> = CharacterClass.playableTypes().map { type ->
    previewCharacterClassState(type = type)
}
