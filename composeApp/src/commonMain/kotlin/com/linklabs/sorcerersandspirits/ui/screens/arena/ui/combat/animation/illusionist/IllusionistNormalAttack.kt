package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.weaponClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.NormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BardStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.drawBardDrumStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.play as playBardStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.DuelistStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.drawTwinSwordSlash
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.play as playDuelistStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.GamblerStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.drawGamblerStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.play as playGamblerStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageBoltState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.drawMageCast
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.mageElementFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.palette
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.play
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.ScythePalette
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.drawScytheReap
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.play as playNecromancerReap
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.PaladinStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.drawMaceSwing
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.play as playPaladinStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.projectileStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.RogueStrikeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.drawRogueStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.play as playRogueStrike
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry

/**
 * The Illusionist scythe recolored to Illusionist blue (SS-197): the borrowed Necromancer reap
 * carved in the class's own blues instead of the Necromancer's greens.
 */
private val illusionistScythePalette =
    ScythePalette(
        core = Color(color = 0xFFD6EAFF),
        blade = Color(color = 0xFF387CD3),
        glow = Color(color = 0xFF4C8FE0),
        snath = Color(color = 0xFF1D406D),
    )

/**
 * The Illusionist's normal attack (SS-197): it has no weapon of its own, so it borrows one. When
 * the Illusionist has impersonated a class it plays that class's weapon; with no one impersonated
 * (or a class whose weapon has not been built yet) it plays a random one of the built weapons,
 * fresh each attack. Whichever weapon it borrows, it renders in Illusionist blue rather than the
 * owning class's color. The choice comes from [weaponClassType], which every timing and gesture
 * site shares so the weapon, its defender-reaction timing, and the body gesture all agree. Sits out
 * any cue that is not an Illusionist normal attack.
 */
@Composable
internal fun IllusionistNormalAttackOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val rogueState = remember { RogueStrikeState() }
    val gamblerState = remember { GamblerStrikeState() }
    val duelistState = remember { DuelistStrikeState() }
    val necromancerState = remember { NecromancerStrikeState() }
    val bardState = remember { BardStrikeState() }
    val mageState = remember { MageBoltState() }
    val paladinState = remember { PaladinStrikeState() }
    var activeWeapon by remember { mutableStateOf<CharacterClassType?>(value = null) }
    val density = LocalDensity.current
    val accentColor = CharacterClassType.Illusionist.toColor()

    NormalAttackOverlay(
        animationCue = animationCue,
        attackerClassType = CharacterClassType.Illusionist,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = {
            activeWeapon = null
            rogueState.scene = null
            gamblerState.scene = null
            duelistState.scene = null
            necromancerState.scene = null
            bardState.scene = null
            mageState.scene = null
            paladinState.scene = null
        },
        play = { cue, attackerBounds, defenderBounds ->
            val weapon = cue.weaponClassType
            activeWeapon = weapon
            when (weapon) {
                CharacterClassType.Rogue ->
                    rogueState.playRogueStrike(
                        geometry =
                            projectileStrikeGeometry(
                                cue = cue,
                                attackerBounds = attackerBounds,
                                defenderBounds = defenderBounds,
                                accentColor = accentColor,
                                density = density,
                            ),
                        outcome = cue.outcome,
                    )

                CharacterClassType.Gambler ->
                    gamblerState.playGamblerStrike(
                        geometry =
                            projectileStrikeGeometry(
                                cue = cue,
                                attackerBounds = attackerBounds,
                                defenderBounds = defenderBounds,
                                accentColor = accentColor,
                                density = density,
                            ),
                        outcome = cue.outcome,
                    )

                CharacterClassType.Duelist ->
                    duelistState.playDuelistStrike(
                        geometry =
                            weaponStrikeGeometry(
                                attackerBounds = attackerBounds,
                                target = defenderBounds.center,
                                accentColor = accentColor,
                                density = density,
                            ),
                        outcome = cue.outcome,
                    )

                CharacterClassType.Necromancer ->
                    necromancerState.playNecromancerReap(
                        geometry =
                            weaponStrikeGeometry(
                                attackerBounds = attackerBounds,
                                target = defenderBounds.center,
                                accentColor = accentColor,
                                density = density,
                            ),
                        palette = illusionistScythePalette,
                        outcome = cue.outcome,
                    )

                CharacterClassType.Bard ->
                    bardState.playBardStrike(
                        geometry =
                            weaponStrikeGeometry(
                                attackerBounds = attackerBounds,
                                target = defenderBounds.center,
                                accentColor = accentColor,
                                density = density,
                            ),
                        outcome = cue.outcome,
                    )

                CharacterClassType.Mage -> {
                    val element = mageElementFor(cueId = cue.id)
                    mageState.play(
                        geometry =
                            projectileStrikeGeometry(
                                cue = cue,
                                attackerBounds = attackerBounds,
                                defenderBounds = defenderBounds,
                                accentColor = element.palette().glow,
                                density = density,
                            ),
                        element = element,
                        outcome = cue.outcome,
                    )
                }

                CharacterClassType.Paladin ->
                    paladinState.playPaladinStrike(
                        geometry =
                            weaponStrikeGeometry(
                                attackerBounds = attackerBounds,
                                target = defenderBounds.center,
                                accentColor = accentColor,
                                density = density,
                            ),
                        outcome = cue.outcome,
                    )

                else -> activeWeapon = null
            }
        },
        modifier = modifier,
        draw = {
            when (activeWeapon) {
                CharacterClassType.Rogue -> drawRogueStrike(state = rogueState)
                CharacterClassType.Gambler -> drawGamblerStrike(state = gamblerState)
                CharacterClassType.Duelist -> drawTwinSwordSlash(state = duelistState)
                CharacterClassType.Necromancer -> drawScytheReap(state = necromancerState)
                CharacterClassType.Bard -> drawBardDrumStrike(state = bardState)
                CharacterClassType.Mage -> drawMageCast(state = mageState)
                CharacterClassType.Paladin -> drawMaceSwing(state = paladinState)
                else -> {}
            }
        },
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsRoguePreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Rogue,
        outcome = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsGamblerPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Gambler,
        outcome = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsDuelistPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Duelist,
        outcome = NormalRollMessageType.Knockout,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsNecromancerPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Necromancer,
        outcome = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsBardPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Bard,
        outcome = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsMagePreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Mage,
        outcome = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistBorrowsPaladinPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.Paladin,
        outcome = NormalRollMessageType.Knockout,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistRandomWeaponPreview() {
    IllusionistNormalAttackPreview(
        impersonatedClassType = CharacterClassType.None,
        outcome = NormalRollMessageType.Knockout,
    )
}

@Composable
private fun IllusionistNormalAttackPreview(
    impersonatedClassType: CharacterClassType,
    outcome: NormalRollMessageType,
) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = impersonatedClassType to outcome) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "illusionist-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Illusionist,
                defenderUserId = PREVIEW_TARGET_ID,
                outcome = outcome,
                impersonatedClassType = impersonatedClassType,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Illusionist, seats = seats) {
        IllusionistNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
