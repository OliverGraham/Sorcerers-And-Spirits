package com.linklabs.sorcerersandspirits.data

import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepositoryCopyForIOS
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.utilities.StateFlowContainer
import kotlinx.coroutines.flow.Flow

/**
 * Used in iOSApp.swift.
 *
 * @param TavernRepositoryCopyForIOS responsible for actually calling the code
 */
class IOSTavernRepositoryCoordinator(
    private val stateFlowContainer: StateFlowContainer<Tavern>,
    private val tavernRepositoryCopyForIOS: TavernRepositoryCopyForIOS,
) : TavernRepository {

    override fun getTavernFlow(): Flow<Tavern> = stateFlowContainer.flow

    override suspend fun aTravelerEntersTavern(traveler: Traveler) {
        tavernRepositoryCopyForIOS.aTravelerEntersTavern(traveler = traveler)
    }
}
