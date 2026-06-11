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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

// TODO: Update Slop

@Composable
fun CharacterClassCard(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    enabled: Boolean = true,
    selectedBy: String? = null,
    onClick: () -> Unit = {},
) {
    val borderColor =
        when {
            isActive -> Color(0xFFFFD700)

            // Golden for active
            isSelected -> MaterialTheme.colorScheme.primary

            else -> MaterialTheme.colorScheme.outlineVariant
        }

    val borderWidth = if (isActive || isSelected) 4.dp else 1.dp

    val cardModifier =
        modifier
            .fillMaxWidth()
            .then(if (isActive) Modifier.height(320.dp) else Modifier.height(240.dp))
            .padding(all = 8.dp)

    if (isActive) {
        ElevatedCard(
            onClick = onClick,
            enabled = enabled,
            modifier = cardModifier,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            CardContent(selectedBy, characterClass)
        }
    } else {
        OutlinedCard(
            onClick = onClick,
            enabled = enabled,
            modifier = cardModifier,
            border = BorderStroke(width = borderWidth, color = borderColor),
        ) {
            CardContent(selectedBy, characterClass)
        }
    }
}

@Composable
private fun CardContent(selectedBy: String?, characterClass: CharacterClassState) {
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
    offensiveAbility: OffensiveAbilityState,
    defensiveAbility: DefensiveAbilityState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Offense: ${offensiveAbility::class.simpleName}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Defense: ${defensiveAbility::class.simpleName}",
            style = MaterialTheme.typography.bodyMedium,
        )
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
        CharacterClassCard(characterClass = CharacterClass.getRogue().toState())
    }
}

@Preview(showBackground = true)
@Composable
private fun MageCardPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCard(characterClass = CharacterClass.getMage().toState())
    }
}

@Preview(showBackground = true)
@Composable
private fun PaladinCardPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCard(characterClass = CharacterClass.getPaladin().toState())
    }
}
