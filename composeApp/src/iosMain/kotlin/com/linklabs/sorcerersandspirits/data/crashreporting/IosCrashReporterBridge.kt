package com.linklabs.sorcerersandspirits.data.crashreporting

interface IosCrashReporterBridge {
    fun setUserId(userId: String?)

    fun log(message: String)

    fun recordException(name: String, reason: String?, stackTrace: String)
}
