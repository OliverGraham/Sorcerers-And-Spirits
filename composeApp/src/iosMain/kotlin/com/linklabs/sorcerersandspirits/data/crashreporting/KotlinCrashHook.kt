package com.linklabs.sorcerersandspirits.data.crashreporting

import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException

@OptIn(ExperimentalNativeApi::class)
fun installKotlinCrashHook(crashReporter: CrashReporter) {
    setUnhandledExceptionHook { throwable ->
        crashReporter.recordException(throwable = throwable)
        terminateWithUnhandledException(throwable = throwable)
    }
}
