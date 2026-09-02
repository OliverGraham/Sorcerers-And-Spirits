package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// TODO: This will replace the CharacterClassWheel eventually

@Composable
fun CharacterClassPager(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // TODO: On left-hand side, Oval "pills" that contain:
        //  on the left a Number then the className, colored with the class color
        //  onClick should go to that page in the pager
        Column() {
            characterClasses.forEachIndexed { index, characterClass ->
                ClassPagerIndicator()
            }
        }

        // TODO: Full size vertical pager
        VerticalPager() {
            characterClasses.forEach { characterClass ->

            }
        }
    }
}

@Composable
private fun ClassPagerIndicator(
    turnOrderNumber: Int,
    characterClass: CharacterClassState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

}

@Composable
private fun CharacterClassPage(
    characterClass: CharacterClassState,
    modifier: Modifier = Modifier,
) {
    // TODO: On top half of screen is the Avatar associated with the characterClass
    //  on bottom half is the stats of the character and the explanations (like the expanded version
    //  of ClassStatBar (we'll work on styling after getting it set up).
}

@Preview
@Composable
private fun CharacterClassPagerPreview() {

}
