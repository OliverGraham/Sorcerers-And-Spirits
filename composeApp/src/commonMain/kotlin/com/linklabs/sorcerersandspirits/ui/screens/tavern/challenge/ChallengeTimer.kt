package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.domain.utilities.ONE_SECOND
import com.linklabs.sorcerersandspirits.domain.utilities.SIXTY_SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Can make more generic and reuse for Arena
internal class ChallengeTimer(
    private val coroutineScope: CoroutineScope,
    private val onTimerTick: (Int) -> Unit,
    private val onTimerExpiration: () -> Unit,
) {
    private var timerJob: Job? = null

    fun startTimer(callOnTimerExpiration: Boolean = true) {
        stopTimer()

        timerJob =
            coroutineScope.launch {
                var secondsRemaining = SIXTY_SECONDS

                while (secondsRemaining >= 0) {
                    onTimerTick(secondsRemaining)

                    delay(timeMillis = ONE_SECOND)
                    secondsRemaining--
                }

                if (callOnTimerExpiration) {
                    onTimerExpiration()
                }
            }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
