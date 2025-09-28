package com.linklabs.data

import com.linklabs.domain.tavern.Tavern
import com.linklabs.domain.tavern.TavernRepository

class TavernRepositoryDataSource : TavernRepository {
    override fun getTavern(): Tavern {
        return Tavern(players = emptyList())
    }
}
