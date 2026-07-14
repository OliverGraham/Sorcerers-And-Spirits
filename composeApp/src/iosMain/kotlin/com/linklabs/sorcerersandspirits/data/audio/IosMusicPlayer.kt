package com.linklabs.sorcerersandspirits.data.audio

import com.linklabs.sorcerersandspirits.domain.audio.MusicPlayer
import com.linklabs.sorcerersandspirits.domain.audio.MusicTrack
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

private const val CROSS_FADE_SECONDS = 0.8
private const val CROSS_FADE_MILLIS = 800L
private const val LOOP_FOREVER = -1L
private const val FULL_VOLUME = 1f
private const val SILENT = 0f

@OptIn(ExperimentalForeignApi::class)
class IosMusicPlayer : MusicPlayer {

    private val scope = CoroutineScope(context = Dispatchers.Main + SupervisorJob())

    private var currentTrack: MusicTrack? = null
    private var currentPlayer: AVAudioPlayer? = null
    private var fadeJob: Job? = null
    private var isMuted = false

    override fun play(track: MusicTrack) {
        val isAlreadyPlaying = track == currentTrack
        if (isAlreadyPlaying) return

        val path = bundlePathForAudioAsset(assetFileName = track.assetFileName) ?: return
        val incoming =
            AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path = path), error = null)

        val outgoing = currentPlayer
        currentTrack = track
        currentPlayer = incoming

        incoming.numberOfLoops = LOOP_FOREVER
        incoming.volume = SILENT
        incoming.prepareToPlay()
        incoming.play()

        crossFade(outgoing = outgoing, incoming = incoming)
    }

    override fun stop() {
        val outgoing = currentPlayer
        currentTrack = null
        currentPlayer = null
        if (outgoing == null) return

        outgoing.setVolume(volume = SILENT, fadeDuration = CROSS_FADE_SECONDS)
        stopAfterFade(outgoing = outgoing)
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
        currentPlayer?.volume = activeVolume()
    }

    override fun release() {
        scope.cancel()
        currentPlayer?.stop()
        currentPlayer = null
        currentTrack = null
    }

    private fun crossFade(outgoing: AVAudioPlayer?, incoming: AVAudioPlayer) {
        incoming.setVolume(volume = activeVolume(), fadeDuration = CROSS_FADE_SECONDS)
        outgoing?.setVolume(volume = SILENT, fadeDuration = CROSS_FADE_SECONDS)
        stopAfterFade(outgoing = outgoing)
    }

    private fun stopAfterFade(outgoing: AVAudioPlayer?) {
        if (outgoing == null) return
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                delay(timeMillis = CROSS_FADE_MILLIS)
                outgoing.stop()
            }
    }

    private fun activeVolume(): Float = if (isMuted) SILENT else FULL_VOLUME
}
