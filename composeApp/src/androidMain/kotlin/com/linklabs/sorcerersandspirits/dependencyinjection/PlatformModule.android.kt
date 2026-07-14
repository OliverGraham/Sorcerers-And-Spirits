package com.linklabs.sorcerersandspirits.dependencyinjection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.linklabs.sorcerersandspirits.data.audio.AndroidMusicPlayer
import com.linklabs.sorcerersandspirits.data.audio.AndroidSoundEffectPlayer
import com.linklabs.sorcerersandspirits.data.authentication.AndroidSocialSignInProvider
import com.linklabs.sorcerersandspirits.data.connectivity.AndroidConnectivityObserver
import com.linklabs.sorcerersandspirits.data.haptics.AndroidHaptics
import com.linklabs.sorcerersandspirits.data.profile.PENDING_STATS_DATASTORE_FILE
import com.linklabs.sorcerersandspirits.data.profile.createPendingStatsDataStore
import com.linklabs.sorcerersandspirits.data.sensor.AndroidShakeDetector
import com.linklabs.sorcerersandspirits.data.version.AndroidCurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.audio.MusicPlayer
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffectPlayer
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.platform.CurrentActivityHolder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ConnectivityObserver> { AndroidConnectivityObserver(context = get()) }
    single<ShakeDetector> { AndroidShakeDetector(context = get()) }
    single<Haptics> { AndroidHaptics(context = get()) }
    single<MusicPlayer> { AndroidMusicPlayer(context = get()) }
    single<SoundEffectPlayer> { AndroidSoundEffectPlayer(context = get()) }
    single<CurrentAppVersionProvider> { AndroidCurrentAppVersionProvider(context = get()) }
    single { CurrentActivityHolder() }
    single<DataStore<Preferences>> {
        val context: Context = get()
        createPendingStatsDataStore {
            context.filesDir.resolve(PENDING_STATS_DATASTORE_FILE).absolutePath
        }
    }
    single<SocialSignInProvider> {
        AndroidSocialSignInProvider(context = get(), currentActivityHolder = get())
    }
}
