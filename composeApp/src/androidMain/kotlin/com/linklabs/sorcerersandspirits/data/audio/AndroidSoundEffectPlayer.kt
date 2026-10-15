package com.linklabs.sorcerersandspirits.data.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.SoundPool
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffect
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffectPlayer
import java.io.IOException

private const val MAX_SIMULTANEOUS_EFFECTS = 8
private const val LOAD_PRIORITY = 1
private const val NO_LOOP = 0
private const val NORMAL_PLAYBACK_RATE = 1f
private const val LOAD_SUCCESS = 0
private const val FULL_VOLUME = 1f
private const val SILENT = 0f

class AndroidSoundEffectPlayer(private val context: Context) : SoundEffectPlayer {

    private val soundPool =
        SoundPool.Builder()
            .setMaxStreams(MAX_SIMULTANEOUS_EFFECTS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()

    private val sampleIdsByEffect = mutableMapOf<SoundEffect, Int>()
    private val samplesPendingPlay = mutableSetOf<Int>()
    private var isMuted = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val isLoaded = status == LOAD_SUCCESS
            val wasPendingPlay = samplesPendingPlay.remove(element = sampleId)
            if (isLoaded && wasPendingPlay) playSample(sampleId = sampleId)
        }
    }

    override fun play(effect: SoundEffect) {
        val loadedSampleId = sampleIdsByEffect[effect]
        if (loadedSampleId != null) {
            playSample(sampleId = loadedSampleId)
            return
        }

        val descriptor = openAssetOrNull(effect = effect) ?: return
        val sampleId = soundPool.load(descriptor, LOAD_PRIORITY)
        descriptor.close()
        sampleIdsByEffect[effect] = sampleId
        samplesPendingPlay.add(element = sampleId)
    }

    private fun openAssetOrNull(effect: SoundEffect): AssetFileDescriptor? = try {
        context.assets.openFd("$AUDIO_ASSET_DIRECTORY/${effect.assetFileName}")
    } catch (exception: IOException) {
        null
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
    }

    override fun release() {
        soundPool.release()
        sampleIdsByEffect.clear()
        samplesPendingPlay.clear()
    }

    private fun playSample(sampleId: Int) {
        val volume = if (isMuted) SILENT else FULL_VOLUME
        soundPool.play(sampleId, volume, volume, LOAD_PRIORITY, NO_LOOP, NORMAL_PLAYBACK_RATE)
    }
}
