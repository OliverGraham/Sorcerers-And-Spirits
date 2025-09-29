package com.linklabs.sorcerersandspirits.data

import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class TavernRepositoryDataSource : TavernRepository {
    override fun getTavern(): Tavern {
        return Tavern(players = emptyList())
    }
}
