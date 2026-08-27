package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.profile.RandomNameProvider

class RemoteConfigRandomNameProvider(private val remoteConfigDataSource: RemoteConfigDataSource) :
    RandomNameProvider {

    override fun getRandomName(): String = remoteConfigDataSource.randomTravelerNames().random()
}
