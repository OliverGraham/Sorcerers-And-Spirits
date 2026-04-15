package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CharacterClassCards(characterClasses: List<CharacterClass>, modifier: Modifier = Modifier) {
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
        modifier = modifier,
    )
}

@Composable
private fun CharacterClassCards(
    count: Int,
    characterClasses: List<CharacterClass>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        key = { index -> characterClasses[index % count].className },
    ) { index ->
        CharacterClassCard(characterClass = characterClasses[index % count])
    }
}

@Preview
@Composable
private fun CharacterClassCardsPreview() {
    SorcerersAndSpiritsTheme {
        CharacterClassCards(characterClasses = CharacterClass.getAllClasses())
    }
}
