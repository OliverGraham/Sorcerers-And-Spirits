package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class SendChallengeUseCase(private val tavernRepository: TavernRepository) {
    suspend operator fun invoke(instigator: Challenger, invitees: List<Challenger>) = tavernRepository.sendChallenge(instigator = instigator, invitees = invitees)
}
