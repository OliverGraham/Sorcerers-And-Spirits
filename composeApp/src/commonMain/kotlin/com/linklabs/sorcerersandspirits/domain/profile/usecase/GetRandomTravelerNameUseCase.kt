package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.profile.RandomNameProvider

class GetRandomTravelerNameUseCase(private val randomNameProvider: RandomNameProvider) {
    operator fun invoke(): String = randomNameProvider.getRandomName()
}
