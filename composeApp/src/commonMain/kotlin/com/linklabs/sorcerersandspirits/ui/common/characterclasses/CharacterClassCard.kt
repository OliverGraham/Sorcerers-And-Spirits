package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
fun CharacterClassCard(characterClass: CharacterClass, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(all = 8.dp)) {
        Column(
            modifier = Modifier.padding(all = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Header(className = characterClass.className, classSpeed = characterClass.speed)
            Body(
                offensiveAbility = characterClass.offensiveAbility,
                defensiveAbility = characterClass.defensiveAbility,
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
    SorcerersAndSpiritsTheme { CharacterClassCard(characterClass = CharacterClass.Rogue()) }
}

@Preview(showBackground = true)
@Composable
private fun MageCardPreview() {
    SorcerersAndSpiritsTheme { CharacterClassCard(characterClass = CharacterClass.Mage()) }
}

@Preview(showBackground = true)
@Composable
private fun PaladinCardPreview() {
    SorcerersAndSpiritsTheme { CharacterClassCard(characterClass = CharacterClass.Paladin()) }
}
