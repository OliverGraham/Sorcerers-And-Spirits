package com.linklabs.sorcerersandspirits.domain.credits

data class Credits(val sections: List<CreditsSection>) {
    companion object {
        val EMPTY = Credits(sections = emptyList())
    }
}

data class CreditsSection(val title: String, val entries: List<String>)
