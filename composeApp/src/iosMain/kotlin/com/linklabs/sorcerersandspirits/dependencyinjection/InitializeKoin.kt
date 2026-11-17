package com.linklabs.sorcerersandspirits.dependencyinjection

import com.linklabs.sorcerersandspirits.data.authentication.IosSocialSignInBridge
import com.linklabs.sorcerersandspirits.data.crashreporting.IosCrashReporterBridge
import com.linklabs.sorcerersandspirits.data.crashreporting.installKotlinCrashHook
import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initializeKoin(
    socialSignInBridge: IosSocialSignInBridge,
    crashReporterBridge: IosCrashReporterBridge,
) {
    val koinApplication =
        startKoin {
            modules(
                module {
                    single { socialSignInBridge }
                    single { crashReporterBridge }
                },
                commonModule,
            )
        }

    installKotlinCrashHook(crashReporter = koinApplication.koin.get<CrashReporter>())
}
