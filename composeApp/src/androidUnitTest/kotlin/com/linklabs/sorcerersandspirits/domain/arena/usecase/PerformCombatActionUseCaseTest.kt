package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

class PerformCombatActionUseCaseTest {

    companion object {
        private const val DEFAULT_USER_ID = "userId"
        private const val DEFAULT_DISPLAY_NAME = "displayName"
        private const val DEFAULT_ARENA_ID = "arenaId"
        private const val DEFAULT_ATTACKER_ID = "attackerId"
        private const val DEFAULT_DEFENDER_ID = "defenderId"

        private fun createCharacterClass(
            type: CharacterClassType = CharacterClassType.Rogue,
            hitPoints: Int = 5,
            armorClass: Int = 6,
            speed: Int = 8,
            offensiveAbility: OffensiveAbility = OffensiveAbility.None,
            defensiveAbility: DefensiveAbility = DefensiveAbility.None,
            hasActedThisRound: Boolean = false,
        ) =
            CharacterClass(
                type = type,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createRogue(
            hitPoints: Int = 5,
            armorClass: Int = 6,
            speed: Int = 8,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Rogue,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.KillingMomentum,
                defensiveAbility = DefensiveAbility.Dodge(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createGambler(
            hitPoints: Int = 3,
            armorClass: Int = 4,
            speed: Int = 7,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Gambler,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.AllIn(),
                defensiveAbility = DefensiveAbility.LuckyCoin,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createDuelist(
            hitPoints: Int = 4,
            armorClass: Int = 4,
            speed: Int = 6,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Duelist,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.PrepareAttack,
                defensiveAbility = DefensiveAbility.Riposte,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createNecromancer(
            hitPoints: Int = 7,
            armorClass: Int = 7,
            speed: Int = 5,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Necromancer,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.SummonMinion(),
                defensiveAbility = DefensiveAbility.None,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createBard(
            hitPoints: Int = 5,
            armorClass: Int = 5,
            speed: Int = 4,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Bard,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.Encore,
                defensiveAbility = DefensiveAbility.Persuade,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createIllusionist(
            hitPoints: Int = 5,
            armorClass: Int = 5,
            speed: Int = 3,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Illusionist,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.Impersonate(),
                defensiveAbility = DefensiveAbility.MirrorImage(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createMage(
            hitPoints: Int = 4,
            armorClass: Int = 4,
            speed: Int = 2,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Mage,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.SpellPotency,
                defensiveAbility = DefensiveAbility.Teleport,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createPaladin(
            hitPoints: Int = 7,
            armorClass: Int = 7,
            speed: Int = 1,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Paladin,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.None,
                defensiveAbility = DefensiveAbility.HolyShield(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createArenaPlayer(
            userId: String = DEFAULT_USER_ID,
            displayName: String = DEFAULT_DISPLAY_NAME,
            characterClass: CharacterClass = createRogue(),
        ) =
            ArenaPlayer.Selected(
                userId = userId,
                displayName = displayName,
                characterClass = characterClass,
            )

        private fun createCombatActionArguments(
            arenaId: String = DEFAULT_ARENA_ID,
            userId: String = DEFAULT_USER_ID,
            attacker: ArenaPlayer.Selected = createArenaPlayer(userId = DEFAULT_ATTACKER_ID),
            defender: ArenaPlayer.Selected = createArenaPlayer(userId = DEFAULT_DEFENDER_ID),
            playerMap: Map<String, ArenaPlayer.Selected> =
                mapOf(attacker.userId to attacker, defender.userId to defender),
            roundNumber: Int = 1,
            originalTurnUserId: String = DEFAULT_ATTACKER_ID,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
                userId = userId,
                attacker = attacker,
                defender = defender,
                playerMap = playerMap,
                roundNumber = roundNumber,
                originalTurnUserId = originalTurnUserId,
            )
    }
}
