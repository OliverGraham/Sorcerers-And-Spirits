package com.linklabs.sorcerersandspirits.dependencyinjection

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.linklabs.sorcerersandspirits.data.audio.IosMusicPlayer
import com.linklabs.sorcerersandspirits.data.audio.IosSoundEffectPlayer
import com.linklabs.sorcerersandspirits.data.authentication.IosSocialSignInProvider
import com.linklabs.sorcerersandspirits.data.connectivity.IosConnectivityObserver
import com.linklabs.sorcerersandspirits.data.haptics.IosHaptics
import com.linklabs.sorcerersandspirits.data.platform.IosPlatformInfoProvider
import com.linklabs.sorcerersandspirits.data.profile.PENDING_STATS_DATASTORE_FILE
import com.linklabs.sorcerersandspirits.data.profile.createPendingStatsDataStore
import com.linklabs.sorcerersandspirits.data.sensor.IosShakeDetector
import com.linklabs.sorcerersandspirits.data.version.IosCurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.audio.MusicPlayer
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffectPlayer
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import com.linklabs.sorcerersandspirits.domain.platform.PlatformInfoProvider
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun platformModule(): Module = module {
    single<ConnectivityObserver> { IosConnectivityObserver() }
    single<ShakeDetector> { IosShakeDetector() }
    single<Haptics> { IosHaptics() }
    single<MusicPlayer> { IosMusicPlayer() }
    single<SoundEffectPlayer> { IosSoundEffectPlayer() }
    single<CurrentAppVersionProvider> { IosCurrentAppVersionProvider() }
    single<PlatformInfoProvider> { IosPlatformInfoProvider() }
    single<SocialSignInProvider> { IosSocialSignInProvider(bridge = get()) }
    single<DataStore<Preferences>> {
        createPendingStatsDataStore { "${documentDirectoryPath()}/$PENDING_STATS_DATASTORE_FILE" }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectoryPath(): String {
    val documentDirectory: NSURL? =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(documentDirectory?.path)
}
