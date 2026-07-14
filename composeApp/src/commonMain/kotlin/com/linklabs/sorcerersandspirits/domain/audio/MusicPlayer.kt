package com.linklabs.sorcerersandspirits.domain.audio

interface MusicPlayer {
    fun play(track: MusicTrack)

    fun stop()

    fun setMuted(isMuted: Boolean)

    fun release()
}
