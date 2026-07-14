package com.linklabs.sorcerersandspirits.domain.audio

interface SoundEffectPlayer {
    fun play(effect: SoundEffect)

    fun setMuted(isMuted: Boolean)

    fun release()
}
