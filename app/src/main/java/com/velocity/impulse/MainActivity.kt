package com.velocity.impulse

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.App

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Ask for the display's top refresh rate so every animation runs at it.
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { mode ->
                window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
            }
        }
        setContent { App(model) }
    }

    override fun onResume() {
        super.onResume()
        model.resume()
    }

    override fun onPause() {
        super.onPause()
        model.pause()
    }
}
