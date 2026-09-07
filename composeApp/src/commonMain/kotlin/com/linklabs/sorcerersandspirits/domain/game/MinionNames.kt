package com.linklabs.sorcerersandspirits.domain.game

internal object MinionNames {
    private val pool = setOf(
        "Gabaner", "Gabantik", "Gabarer", "Gabarn", "Gabartik", "Gabekab", "Gabekn", "Gabobab",
        "Gabober", "Gabobn", "Gabobtik", "Gabanab", "Gabann", "Gananab", "Gananer", "Ganann",
        "Ganantik", "Ganarer", "Ganartik", "Ganobn", "Ganobtik", "Gantik", "Gatik", "Goner",
        "Jabobtik", "Jobaber", "Jebantik", "Jonaner", "Jarober", "Kebab", "Kebober", "Konan",
        "Kabanak", "Laser", "Loner", "Vibobtik", "Vibn", "Xibeker", "Xeneker", "Xebobtik",
        "Zabobtik", "Zebantik", "Zoner",
    )

    fun nameNotIn(usedNames: Set<String>): String {
        val availableNames = pool - usedNames
        return availableNames.randomOrNull() ?: pool.random()
    }
}
