package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.MinionType

enum class WeaponType {
    Dagger,
    Card,
    Rapiers,
    Scythe,
    Drum,
    Spell,
    Mace,
    LongSword,
    StoneFist;

    companion object {
        fun getWeaponType(
            characterClassType: CharacterClassType,
            minionAttackerType: MinionType?,
            impersonatedCharacterType: CharacterClassType?,
        ) = when {

            minionAttackerType != null -> minionAttackerType.toWeaponType()
            characterClassType == CharacterClassType.Illusionist ->
                 impersonatedWeaponType(impersonatedCharacterType = impersonatedCharacterType)
            else -> characterClassType.toWeaponType()
        }

        private fun impersonatedWeaponType(impersonatedCharacterType: CharacterClassType?) =
            impersonatedCharacterType?.toWeaponType() ?: entries.random()



        private fun CharacterClassType.toWeaponType() = when (this) {
            CharacterClassType.Rogue -> Dagger
            CharacterClassType.Gambler -> Card
            CharacterClassType.Duelist -> Rapiers
            CharacterClassType.Necromancer -> Scythe
            CharacterClassType.Bard -> Drum
            CharacterClassType.Mage -> Spell
            CharacterClassType.Paladin -> Mace
            CharacterClassType.Illusionist, CharacterClassType.None -> entries.random()
        }

        private fun MinionType.toWeaponType() = when (this) {
            MinionType.Skeleton -> LongSword
            MinionType.Golem -> StoneFist
        }
    }
}
