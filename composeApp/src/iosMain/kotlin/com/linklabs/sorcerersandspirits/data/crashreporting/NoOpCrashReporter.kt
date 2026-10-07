package com.linklabs.sorcerersandspirits.data.crashreporting

import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter

class NoOpCrashReporter : CrashReporter {
    override fun setUserId(userId: String?) {}

    override fun log(message: String) {}

    override fun recordException(throwable: Throwable) {}
}
