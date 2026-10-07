package com.linklabs.sorcerersandspirits.data.crashreporting

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter

class AndroidCrashReporter : CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}
