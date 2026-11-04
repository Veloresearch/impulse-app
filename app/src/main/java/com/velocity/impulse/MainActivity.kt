package com.velocity.impulse

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.velocity.impulse.data.AnswerService
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Shared
import com.velocity.impulse.ui.App

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Ask for the display's top refresh rate so every animation runs at it.
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { mode ->
                window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
            }
        }
        if (savedInstanceState == null) handle(intent)
        setContent {
            Bars()
            App(model)
        }
    }

    /** Status bar icons that follow the theme, and the notification question when the model asks for it. */
    @Composable
    private fun Bars() {
        val systemDark = isSystemInDarkTheme()
        val light = model.theme == "light" || model.theme == "auto" && !systemDark
        LaunchedEffect(light) {
            val bars = if (light) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT)
            enableEdgeToEdge(bars, bars)
        }
        val ask = model.askNotifications
        LaunchedEffect(ask) {
            if (ask && Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            if (ask) model.askedNotifications()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        Shared.of(intent)?.let { return model.receive(it) }
        intent.getStringExtra(AnswerService.CHAT)?.let { return model.reveal(it) }
        when (intent.action) {
            NEW_CHAT -> model.openShortcut("new")
            VOICE -> model.openShortcut("voice")
            CAMERA -> model.openShortcut("camera")
            Intent.ACTION_VIEW -> intent.data?.takeIf { it.scheme == "impulse" && it.host == "connect" }?.let { model.openLink(it.toString()) }
        }
    }

    override fun onResume() {
        super.onResume()
        model.onScreen(true)
        model.resume()
    }

    override fun onPause() {
        super.onPause()
        model.onScreen(false)
        model.pause()
    }

    private companion object {
        const val NEW_CHAT = "com.velocity.impulse.NEW_CHAT"
        const val VOICE = "com.velocity.impulse.VOICE"
        const val CAMERA = "com.velocity.impulse.CAMERA"
    }
}
