package com.linklabs.sorcerersandspirits.ui

import android.app.Application
import com.google.firebase.FirebaseApp
import com.linklabs.sorcerersandspirits.dependencyinjection.androidModule
import com.linklabs.sorcerersandspirits.dependencyinjection.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SorcerersAndSpiritsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        startKoin {
            androidContext(androidContext = this@SorcerersAndSpiritsApplication)
            modules(commonModule, androidModule)
        }
    }
}