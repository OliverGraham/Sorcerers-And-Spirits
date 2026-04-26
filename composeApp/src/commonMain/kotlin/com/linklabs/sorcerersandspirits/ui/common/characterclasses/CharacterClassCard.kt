package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CharacterClassCard(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    selectedBy: String? = null,
    onClick: () -> Unit = {},
) {
    val border =
        if (isSelected) {
            BorderStroke(width = 4.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            null
        }

    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height = 240.dp).padding(all = 8.dp),
        border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            // TODO Arena: resource, update the slop
            if (selectedBy != null) {
                Text(
                    text = "Chosen by: $selectedBy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Header(className = characterClass.className, classSpeed = characterClass.speed)
            Body(
                offensiveAbility = characterClass.offensiveAbility,
                defensiveAbility = characterClass.defensiveAbility,
                modifier = Modifier.weight(weight = 1f),
            )
            Footer(armorClass = characterClass.armorClass, hitPoints = characterClass.hitPoints)
        }
    }
}

@Composable
private fun Header(className: String, classSpeed: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Spacer(modifier = Modifier.weight(weight = 1f))

        Text(
            text = className,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Speed: $classSpeed",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(weight = 1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Body(
    offensiveAbility: OffensiveAbility,
    defensiveAbility: DefensiveAbility,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Offense: $offensiveAbility", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Defense: $defensiveAbility", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Footer(armorClass: Int, hitPoints: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = "AC: $armorClass", style = MaterialTheme.typography.titleMedium)
        Text(text = "HP: $hitPoints", style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun RogueCardPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCard(characterClass = CharacterClass.Rogue().toState())
    }
}

@Preview(showBackground = true)
@Composable
private fun MageCardPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCard(characterClass = CharacterClass.Mage().toState())
    }
}

@Preview(showBackground = true)
@Composable
private fun PaladinCardPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCard(characterClass = CharacterClass.Paladin().toState())
    }
}
