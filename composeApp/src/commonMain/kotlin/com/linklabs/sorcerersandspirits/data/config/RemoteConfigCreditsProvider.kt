package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.credits.Credits
import com.linklabs.sorcerersandspirits.domain.credits.CreditsProvider

class RemoteConfigCreditsProvider(private val remoteConfigDataSource: RemoteConfigDataSource) : CreditsProvider {

    override fun getCredits(): Credits = remoteConfigDataSource.credits().toDomain()
}
