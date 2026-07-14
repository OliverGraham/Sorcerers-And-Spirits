package com.linklabs.sorcerersandspirits.data.audio

import com.linklabs.sorcerersandspirits.domain.audio.SoundEffect
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffectPlayer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile

private const val FULL_VOLUME = 1f
private const val SILENT = 0f

@OptIn(ExperimentalForeignApi::class)
class IosSoundEffectPlayer : SoundEffectPlayer {

    private val dataByEffect = mutableMapOf<SoundEffect, NSData>()
    private val activePlayers = mutableListOf<AVAudioPlayer>()
    private var isMuted = false

    override fun play(effect: SoundEffect) {
        val data = dataByEffect.getOrPut(key = effect) { loadData(effect = effect) ?: return }

        activePlayers.retainAll { it.playing }
        val player = AVAudioPlayer(data = data, error = null)
        player.volume = if (isMuted) SILENT else FULL_VOLUME
        player.prepareToPlay()
        player.play()
        activePlayers.add(element = player)
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
    }

    override fun release() {
        activePlayers.forEach { it.stop() }
        activePlayers.clear()
        dataByEffect.clear()
    }

    private fun loadData(effect: SoundEffect): NSData? {
        val path = bundlePathForAudioAsset(assetFileName = effect.assetFileName) ?: return null
        return NSData.dataWithContentsOfFile(path = path)
    }
}
