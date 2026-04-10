package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Can make more generic and reuse for Arena
internal class ChallengeTimer(
    private val coroutineScope: CoroutineScope,
    private val onTimerTick: (Int?) -> Unit,
    private val onTimerExpiration: () -> Unit,
) {
    private var timerJob: Job? = null

    fun startTimer() {
        stopTimer()

        timerJob =
            coroutineScope.launch {
                var secondsRemaining = COUNTDOWN_SECONDS

                while (secondsRemaining >= 0) {
                    onTimerTick(secondsRemaining)

                    delay(timeMillis = 1000L)
                    secondsRemaining--
                }

                onTimerExpiration()
            }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null

        onTimerTick(null)
    }

    fun isTimerCurrentlyRunning() = timerJob?.isActive == true

    companion object {
        private const val COUNTDOWN_SECONDS = 60
    }
}
