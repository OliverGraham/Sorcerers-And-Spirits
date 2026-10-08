package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.authentication.IosSocialSignInBridge
import com.linklabs.sorcerersandspirits.data.crashreporting.IosCrashReporterBridge
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initializeKoin(
    socialSignInBridge: IosSocialSignInBridge,
    crashReporterBridge: IosCrashReporterBridge,
) {
    startKoin {
        modules(
            module {
                single { socialSignInBridge }
                single { crashReporterBridge }
            },
            commonModule,
        )
    }
}
