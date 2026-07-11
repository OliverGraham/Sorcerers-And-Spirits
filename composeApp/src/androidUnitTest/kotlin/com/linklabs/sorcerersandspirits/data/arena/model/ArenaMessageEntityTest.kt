package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The combat log is serialized to the data layer and distributed to other clients, so every
 * [ArenaMessage] must survive a `toEntity -> toDomain` round trip with all of its fields intact.
 * These tests guard against fields being dropped, hardcoded, or message types collapsing into one
 * another during serialization.
 */
class ArenaMessageEntityTest {

    @Test
    fun `every arena message survives a data-layer round trip with its id and fields intact`() {
        allMessages.forEach { message ->
            val restored = message.toEntity(id = MESSAGE_ID).toDomain()

            assertEquals(expected = MESSAGE_ID, actual = restored.id)
            assertEquals(
                expected = message,
                actual = restored.message,
                message = "Round trip changed ${message::class.simpleName}",
            )
        }
    }

    companion object {
        private const val MESSAGE_ID = "message_1"

        private val characterClassInfo =
            CharacterClassMessageInfo(className = "Rogue", type = CharacterClassType.Rogue)

        /**
         * One instance of every [ArenaMessage]. Booleans and ids use values that differ from the
         * old hardcoded placeholders (e.g. `didStopAttack = true`, non-empty user ids) so a
         * regression back to fabricated data fails the round trip.
         */
        private val allMessages: List<ArenaMessage> =
            listOf(
                ArenaMessage.ChoseCharacterClass(
                    displayName = "Alice",
                    characterClassInfo = characterClassInfo,
                ),
                ArenaMessage.ConnectedToArena(displayName = "Alice"),
                ArenaMessage.NewRound(roundNumber = 3),
                ArenaMessage.NewTurn(characterClassInfo = characterClassInfo),
                ArenaMessage.NormalRoll(
                    attackerUserId = "attacker",
                    defenderUserId = "defender",
                    firstDie = 4,
                    secondDie = 5,
                    bonus = 1,
                    type = NormalRollMessageType.Knockout,
                    impersonatedCharacterType = CharacterClassType.None,
                ),
                ArenaMessage.KillingMomentum(attackerUserId = "rogue"),
                ArenaMessage.Dodge(didArmorIncrease = true, dodgerUserId = "rogue"),
                ArenaMessage.Dodge(didArmorIncrease = false, dodgerUserId = "rogue"),
                ArenaMessage.Teleport.Trigger(mageUserId = "mage"),
                ArenaMessage.Teleport.Roll(mageUserId = "mage", userId = "mage", singleDie = 5),
                ArenaMessage.Teleport.Result(mageUserId = "mage", didDefenderEscape = true),
                ArenaMessage.LuckyCoin.Trigger(gamblerUserId = "gambler"),
                ArenaMessage.LuckyCoin.Flip(
                    gamblerUserId = "gambler",
                    singleDie = 6,
                    didStopAttack = true,
                ),
                ArenaMessage.LuckyCoin.HighRollerTrigger(
                    gamblerUserId = "gambler",
                    attackerUserId = "attacker",
                ),
                ArenaMessage.LuckyCoin.HighRollerDamage(
                    gamblerUserId = "gambler",
                    attackerUserId = "attacker",
                    singleDie = 6,
                    isKnockout = true,
                ),
                ArenaMessage.Riposte.Trigger(
                    duelistUserId = "duelist",
                    attackerUserId = "attacker",
                ),
                ArenaMessage.Riposte.EnGarde(
                    duelistUserId = "duelist",
                    attackerUserId = "attacker",
                    firstDie = 3,
                    secondDie = 4,
                    isSuccessful = true,
                ),
                ArenaMessage.SummonMinion(necroUserId = "necro", singleDie = 6),
                ArenaMessage.Persuade.Trigger(bardUserId = "bard"),
                ArenaMessage.Persuade.StrumLute(
                    bardUserId = "bard",
                    diceRoll = 5,
                    isSuccessful = true,
                ),
                ArenaMessage.Persuade.CharmActivation(bardUserId = "bard"),
                ArenaMessage.Persuade.Lull,
                ArenaMessage.Persuade.Charm(
                    bardUserId = "bard",
                    attackerUserId = "attacker",
                    defenderUserId = "defender",
                ),
                ArenaMessage.Encore.Trigger(bardUserId = "bard"),
                ArenaMessage.Encore.Roll(bardUserId = "bard", encoreDie = 5, isSuccessful = true),
                ArenaMessage.Impersonate(
                    illusionistUserId = "illusionist",
                    impersonateAbility =
                        OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.AllIn()),
                ),
                ArenaMessage.MirrorImage.Trigger(
                    illusionistUserId = "illusionist",
                    firstDie = 3,
                    secondDie = 4,
                ),
                ArenaMessage.MirrorImage.Pass(
                    illusionistUserId = "illusionist",
                    attackerUserId = "attacker",
                    outcome = NormalRollMessageType.ArmorClassMiss,
                ),
                ArenaMessage.MirrorImage.TemptFate(
                    illusionistUserId = "illusionist",
                    rerolledMirrorImageDie =
                        AbilityAction.Passive.MirrorImage.MirrorImageDie(
                            originalRoll = 4,
                            reroll = 6,
                        ),
                    attackerUserId = "attacker",
                    outcome = NormalRollMessageType.Knockout,
                ),
                ArenaMessage.SpellPotency(
                    mageUserId = "mage",
                    mainTargetUserId = "target",
                    mainDamage = 8,
                    splashDamage = 4,
                    splashTargetUserIds = listOf("rogue", "bard"),
                ),
                ArenaMessage.HolyShield.Trigger(paladinUserId = "paladin"),
                ArenaMessage.HolyShield.Prayer(
                    paladinUserId = "paladin",
                    firstDie = 5,
                    secondDie = 6,
                    didSurvive = true,
                ),
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = "paladin",
                    attackerUserId = "attacker",
                    singleDie = 4,
                ),
                ArenaMessage.AllIn(gamblerUserId = "gambler", singleDie = 1),
                ArenaMessage.PrepareAttack(
                    duelistUserId = "duelist",
                    targetUserId = "target",
                    firstDie = 6,
                    secondDie = 6,
                    isSuccessful = true,
                ),
                ArenaMessage.Doubles(userId = "attacker"),
            )
    }
}
