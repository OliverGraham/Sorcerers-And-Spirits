package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.credits.Credits
import com.linklabs.sorcerersandspirits.domain.credits.CreditsSection
import kotlinx.serialization.Serializable

@Serializable
data class CreditsEntity(val sections: List<CreditsSectionEntity>) {
    fun toDomain() = Credits(sections = sections.map { it.toDomain() })

    companion object {
        val EMPTY = CreditsEntity(sections = emptyList())
    }
}

@Serializable
data class CreditsSectionEntity(val title: String, val entries: List<String>) {
    fun toDomain() = CreditsSection(title = title, entries = entries)
}
