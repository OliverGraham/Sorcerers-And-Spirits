package com.linklabs.sorcerersandspirits.data.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.linklabs.sorcerersandspirits.domain.audio.MusicPlayer
import com.linklabs.sorcerersandspirits.domain.audio.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AndroidMusicPlayer(private val context: Context) : MusicPlayer {

    private val scope = CoroutineScope(context = Dispatchers.Main + SupervisorJob())

    private var currentTrack: MusicTrack? = null
    private var currentPlayer: ExoPlayer? = null
    private var fadeJob: Job? = null
    private var isMuted = false

    override fun play(track: MusicTrack) {
        val isAlreadyPlaying = track == currentTrack
        if (isAlreadyPlaying) return

        val outgoing = currentPlayer
        val incoming = createPlayer(track = track)
        currentTrack = track
        currentPlayer = incoming
        fadeIn(outgoing = outgoing, incoming = incoming)
    }

    override fun stop() {
        val outgoing = currentPlayer
        currentTrack = null
        currentPlayer = null
        fadeOutAndRelease(outgoing = outgoing)
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
        currentPlayer?.volume = if (isMuted) SILENT else FULL_VOLUME
    }

    override fun release() {
        scope.cancel()
        currentPlayer?.release()
        currentPlayer = null
        currentTrack = null
    }

    private fun createPlayer(track: MusicTrack): ExoPlayer =
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri("asset:///$AUDIO_ASSET_DIRECTORY/${track.assetFileName}"),
            )
            repeatMode = Player.REPEAT_MODE_ONE
            volume = SILENT
            prepare()
            playWhenReady = true
        }

    private fun fadeIn(outgoing: ExoPlayer?, incoming: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                repeat(times = CROSS_FADE_STEPS) { step ->
                    val fraction = step.toFadeFraction()
                    incoming.volume = getIncomingVolumeOrMuted(fadeFraction = fraction)
                    outgoing?.volume = getOutgoingVolumeOrMuted(fadeFraction = fraction)
                    delay(timeMillis = FADE_TIME_MILLIS)
                }
                outgoing?.release()
            }
    }

    private fun fadeOutAndRelease(outgoing: ExoPlayer?) {
        if (outgoing == null) return
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                repeat(times = CROSS_FADE_STEPS) { step ->
                    val fraction = step.toFadeFraction()
                    outgoing.volume = getOutgoingVolumeOrMuted(fadeFraction = fraction)
                    delay(timeMillis = FADE_TIME_MILLIS)
                }
                outgoing.release()
            }
    }

    private fun Int.toFadeFraction() = (this + 1).toFloat() / CROSS_FADE_STEPS

    private fun getIncomingVolumeOrMuted(fadeFraction: Float) =
        if (isMuted) SILENT else fadeFraction

    private fun getOutgoingVolumeOrMuted(fadeFraction: Float) = if (isMuted) {
        SILENT
    } else {
        FULL_VOLUME -
            fadeFraction
    }

    companion object {
        private const val CROSS_FADE_MILLIS = 800L
        private const val CROSS_FADE_STEPS = 20
        private const val FULL_VOLUME = 1f
        private const val SILENT = 0f
        private const val FADE_TIME_MILLIS = CROSS_FADE_MILLIS / CROSS_FADE_STEPS
    }
}
