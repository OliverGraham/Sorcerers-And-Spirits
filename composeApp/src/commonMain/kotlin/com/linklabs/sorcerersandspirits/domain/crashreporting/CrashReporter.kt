package com.linklabs.sorcerersandspirits.domain.crashreporting

interface CrashReporter {
    fun setUserId(userId: String?)

    fun log(message: String)

    fun recordException(throwable: Throwable)
}
