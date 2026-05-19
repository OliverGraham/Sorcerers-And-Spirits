package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO Arena: Update class-selection logic

@Composable
fun CharacterClassCards(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
    selectedClass: CharacterClassState? = null,
    unavailableClasses: List<CharacterClassState> = emptyList(),
    classSelections: Map<CharacterClassState, String> = emptyMap(),
    onClassSelected: (CharacterClassState) -> Unit = {},
) {
    val count = characterClasses.size
    val startPage =
        remember(key1 = count) {
            val mid = Int.MAX_VALUE / 2
            mid - (mid % count)
        }

    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { Int.MAX_VALUE })

    CharacterClassCards(
        count = count,
        characterClasses = characterClasses,
        pagerState = pagerState,
        selectedClass = selectedClass,
        unavailableClasses = unavailableClasses,
        classSelections = classSelections,
        onClassSelected = onClassSelected,
        modifier = modifier,
    )
}

@Composable
private fun CharacterClassCards(
    count: Int,
    characterClasses: List<CharacterClassState>,
    pagerState: PagerState,
    selectedClass: CharacterClassState? = null,
    unavailableClasses: List<CharacterClassState> = emptyList(),
    classSelections: Map<CharacterClassState, String> = emptyMap(),
    onClassSelected: (CharacterClassState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        key = { index ->
            val characterClass = characterClasses[index % count]
            "${characterClass.className}-$index"
        },
    ) { index ->
        val characterClass = characterClasses[index % count]
        CharacterClassCard(
            characterClass = characterClass,
            isSelected = characterClass == selectedClass,
            enabled = characterClass !in unavailableClasses,
            selectedBy = classSelections[characterClass],
            onClick = { onClassSelected(characterClass) },
        )
    }
}

@Preview
@Composable
private fun CharacterClassCardsPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCards(characterClasses = CharacterClass.getAllClasses().map { it.toState() })
    }
}
