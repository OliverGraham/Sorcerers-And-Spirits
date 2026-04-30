package com.linklabs.sorcerersandspirits.ui

import android.app.Application
import com.linklabs.sorcerersandspirits.dependencyinjection.commonModule
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SorcerersAndSpiritsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Firebase.initialize(context = this)

        startKoin {
            androidContext(androidContext = this@SorcerersAndSpiritsApplication)
            modules(commonModule)
        }
    }
}
