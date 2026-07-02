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
                ),
                ArenaMessage.KillingMomentum,
                ArenaMessage.Dodge(didArmorIncrease = true),
                ArenaMessage.Dodge(didArmorIncrease = false),
                ArenaMessage.Teleport.Trigger,
                ArenaMessage.Teleport.Roll(userId = "mage", singleDie = 5),
                ArenaMessage.Teleport.Result(didDefenderEscape = true),
                ArenaMessage.LuckyCoin.Trigger,
                ArenaMessage.LuckyCoin.Flip(singleDie = 6, didStopAttack = true),
                ArenaMessage.LuckyCoin.HighRollerTrigger(targetUserId = "target"),
                ArenaMessage.LuckyCoin.HighRollerDamage(singleDie = 6, isKnockout = true),
                ArenaMessage.Riposte.Trigger,
                ArenaMessage.Riposte.EnGarde(firstDie = 3, secondDie = 4, isSuccessful = true),
                ArenaMessage.SummonMinion(necroUserId = "necro", singleDie = 6),
                ArenaMessage.Persuade.Trigger,
                ArenaMessage.Persuade.StrumLute(diceRoll = 5, isSuccessful = true),
                ArenaMessage.Persuade.CharmActivation,
                ArenaMessage.Persuade.Lull,
                ArenaMessage.Persuade.Charm(
                    attackerUserId = "attacker",
                    defenderUserId = "defender",
                ),
                ArenaMessage.Encore.Trigger,
                ArenaMessage.Encore.Roll(encoreDie = 5, isSuccessful = true),
                ArenaMessage.Impersonate(
                    illusionistUserId = "illusionist",
                    impersonateAbility =
                        OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.AllIn()),
                ),
                ArenaMessage.MirrorImage.Trigger(firstDie = 3, secondDie = 4),
                ArenaMessage.MirrorImage.Pass(
                    attackerUserId = "attacker",
                    outcome = NormalRollMessageType.ArmorClassMiss,
                ),
                ArenaMessage.MirrorImage.TemptFate(
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
                ),
                ArenaMessage.HolyShield.Trigger,
                ArenaMessage.HolyShield.Prayer(firstDie = 5, secondDie = 6, didSurvive = true),
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
