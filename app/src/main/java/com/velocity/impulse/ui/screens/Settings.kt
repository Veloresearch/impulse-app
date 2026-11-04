package com.velocity.impulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.components.Choice
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.ListRow
import com.velocity.impulse.ui.components.PageTitle
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Accents
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Composable
fun SettingsScreen(m: AppModel) {
    val uri = LocalUriHandler.current
    Page {
        PageTitle("Settings")

        Group("Computer") {
            ListRow(m.link?.label ?: "Not connected", detail = m.server?.base?.substringAfter("://"))
            ListRow("Signed in as", detail = m.profileName ?: "Nobody")
            ListRow("Sign out", onClick = { m.signOut() })
            ListRow("Connect to another computer", onClick = { m.forgetServer() })
        }

        Group("Chat") {
            Choice(
                "Context",
                listOf<String?>(null) + m.contexts.map { it.name },
                m.context,
                label = { it ?: "None" },
                onSelect = { m.useContext(it) },
                detail = { n -> m.contexts.firstOrNull { it.name == n }?.tokens?.let { tokens(it) } },
            )
            ListRow("Show thinking", detail = "The model's reasoning, folded above each answer") {
                Toggle(m.showThinking, { m.setThinking(it) })
            }
        }

        Group("Appearance") {
            Row(
                Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("Accent", style = Type.Body)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
                    Accents.forEachIndexed { i, c ->
                        val ring by animateDpAsState(if (i == m.accent) 4.dp else 0.dp, Motion.press(), label = "ring")
                        Box(
                            Modifier.size(30.dp).pressable({ m.pickAccent(i) }, CircleShape, c, c, depth = 0.85f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(ring * 2.5f).background(Tone.Panel, CircleShape))
                        }
                    }
                }
            }
            ListRow("Reduce motion", detail = "Cross-fades instead of movement") {
                Toggle(m.reduceMotion, { m.calmMotion(it) })
            }
        }

        Group("About") {
            ListRow("Impulse for Android", detail = "Version ${BuildConfig.VERSION_NAME}")
            ListRow("Source code", detail = "github.com/Veloresearch/impulse-app", onClick = { uri.openUri("https://github.com/Veloresearch/impulse-app") })
            ListRow("License", detail = "Apache License 2.0")
        }

        if (m.error != null) BasicText(m.error!!, style = Type.Meta.copy(color = Signal.Err))
        BasicText("Your chats are kept on this phone.", style = Type.Meta.copy(color = Ink.Four), modifier = Modifier.padding(start = Space.S1))
    }
}
