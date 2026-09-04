package com.linklabs.sorcerersandspirits.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.platform.CurrentActivityHolder
import com.linklabs.sorcerersandspirits.ui.main.App
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val currentActivityHolder: CurrentActivityHolder by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Lets platform singletons (e.g. the Credential Manager account picker) host system UI on
        // this Activity; cleared in onDestroy so the reference never outlives it.
        currentActivityHolder.activity = this

        setContent { App() }
    }

    override fun onDestroy() {
        if (currentActivityHolder.activity == this) {
            currentActivityHolder.activity = null
        }
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
