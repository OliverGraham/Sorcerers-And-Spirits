package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class UpdateArenaChallengeStatusUseCase(
    private val tavernRepository: TavernRepository,
) {
    operator fun invoke(arenaChallenge: ArenaChallenge) {
        tavernRepository.updateArenaChallengeStatus(arenaChallenge = arenaChallenge)
    }
}
