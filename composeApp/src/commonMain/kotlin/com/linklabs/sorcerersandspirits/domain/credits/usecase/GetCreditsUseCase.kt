package com.linklabs.sorcerersandspirits.domain.credits.usecase

import com.linklabs.sorcerersandspirits.domain.credits.Credits
import com.linklabs.sorcerersandspirits.domain.credits.CreditsProvider

class GetCreditsUseCase(private val creditsProvider: CreditsProvider) {
    operator fun invoke(): Credits = creditsProvider.getCredits()
}
