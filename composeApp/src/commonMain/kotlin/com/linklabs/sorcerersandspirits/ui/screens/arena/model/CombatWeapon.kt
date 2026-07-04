package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * The signature weapon a class attacks with (SS-192…SS-199). The weapon is the strike: it flies at
 * the defender accented in the class color, while the attacking avatar only moves enough to sell
 * it. A [Dagger] closes the gap in melee; a [PlayingCard] spins across it as a thrown projectile. A
 * class without a weapon yet falls back to the SS-191 body lunges.
 */
enum class CombatWeapon {
    Dagger,
    PlayingCard,
}

val CharacterClassType.combatWeapon: CombatWeapon?
    get() =
        when (this) {
            CharacterClassType.Rogue -> CombatWeapon.Dagger

            CharacterClassType.Gambler -> CombatWeapon.PlayingCard

            // TODO(SS-194): Duelist weapon.
            CharacterClassType.Duelist -> null

            // TODO(SS-195): Necromancer weapon.
            CharacterClassType.Necromancer -> null

            // TODO(SS-196): Bard weapon.
            CharacterClassType.Bard -> null

            // TODO(SS-197): Illusionist weapon.
            CharacterClassType.Illusionist -> null

            // TODO(SS-198): Mage weapon.
            CharacterClassType.Mage -> null

            // TODO(SS-199): Paladin weapon.
            CharacterClassType.Paladin -> null

            CharacterClassType.None -> null
        }
