package com.linklabs.sorcerersandspirits.domain.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class Timer(
    private val coroutineScope: CoroutineScope,
    private val onTimerTick: (Int) -> Unit,
    private val onTimerExpiration: suspend () -> Unit,
) {
    private var timerJob: Job? = null

    fun startTimer(timerDuration: Int = SIXTY_SECONDS, callOnTimerExpiration: Boolean = true) {
        stopTimer()

        timerJob =
            coroutineScope.launch {
                var secondsRemaining = timerDuration

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
