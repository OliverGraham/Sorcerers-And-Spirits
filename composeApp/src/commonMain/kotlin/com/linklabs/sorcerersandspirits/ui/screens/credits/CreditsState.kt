package com.linklabs.sorcerersandspirits.ui.screens.credits

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.credits.Credits

@Immutable
data class CreditsState(val sections: List<CreditsSectionState>)

@Immutable
data class CreditsSectionState(val title: String, val entries: List<String>)

internal fun Credits.toState() = CreditsState(
    sections = sections.map { CreditsSectionState(title = it.title, entries = it.entries) },
)
