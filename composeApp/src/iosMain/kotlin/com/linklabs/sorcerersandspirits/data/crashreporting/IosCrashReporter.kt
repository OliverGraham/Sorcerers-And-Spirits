package com.linklabs.sorcerersandspirits.data.crashreporting

import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter

class IosCrashReporter(private val bridge: IosCrashReporterBridge) : CrashReporter {
    override fun setUserId(userId: String?) {
        bridge.setUserId(userId = userId)
    }

    override fun log(message: String) {
        bridge.log(message = message)
    }

    override fun recordException(throwable: Throwable) {
        bridge.recordException(
            name = throwable::class.qualifiedName ?: "KotlinException",
            reason = throwable.message,
            stackTrace = throwable.stackTraceToString(),
        )
    }
}
